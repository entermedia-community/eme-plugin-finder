---
name: transcriber
description: Use when working on audio/video transcription - TranscriberSkill, TranscriberManager, TranscriberModule, the `transcriber` aiserver row (aiservertype="transcribe"), or videotrack captions. Explains the full call path, the exact HTTP request sent to the transcribe server, the JSON it must return, and how results land in the videotrack table.
---

# Transcriber: how assets get transcribed

Transcription turns the audio of a video or audio asset into timestamped caption cues stored in the
`videotrack` table. The work is done by a remote transcribe server; EME only cuts the audio into
5-minute MP3 chunks, uploads each one, and stores the returned segments.

| Piece | File | Role |
|---|---|---|
| `transcriberSkill` | `plugins/finder/code/org/entermediadb/ai/skills/TranscriberSkill.java` | Agent step (`aiskill` row `autotranscribe`) |
| `transcriberManager` | `plugins/finder/code/org/entermediadb/ai/transcriber/TranscriberManager.java` | Chunks audio, calls the server, saves captions |
| `TranscriberModule` | `plugins/finder/code/org/entermediadb/ai/transcriber/TranscriberModule.java` | Web action `transcribeAsset` for one asset (UI button) |
| aiserver row `transcriber` | `plugins/catalog/html/data/lists/aiserver/baseaiserver.xml` | Server URL and API key |
| `httpConnection` | `plugins/finder/code/org/entermediadb/ai/llm/http/HttpConnection.java` (extends `BaseLlmConnection`) | Does the HTTP call |

Beans are declared in `plugins/finder/html/src/plugin.xml` (all `scope="prototype"`).

## The aiserver row

```xml
<data id="transcriber" connectionbean="httpConnection"
      serverroot="https://transcribe.emediaworkspace.com"
      serverapikey="demo" aiservertype="transcribe">
  <name><![CDATA[AI: Transcriber Server]]></name>
</data>
```

| Attribute | Used by | Effect |
|---|---|---|
| `aiservertype="transcribe"` | `MediaArchive.getLlmConnection("transcribe")` | How the manager finds this row. The code never uses the id `transcriber` |
| `connectionbean="httpConnection"` | `getLlmConnection` | Bean created for the connection (`HttpConnection`) |
| `serverroot` | `BaseLlmConnection.getServerRoot()` | Base URL. `serverpathprefix` is appended if set (not set here) |
| `serverapikey` | `BaseLlmConnection.getApiKey()` | Sent as `Authorization: Bearer <key>` |
| `modelname`, `monitorspeed` | - | Not used by the transcriber |

### How the row is picked (`MediaArchive.getLlmConnection`, `MediaArchive.java:3386`)

1. Look in cache `llmconnection` under key `transcribe`. If found, use it.
2. Otherwise `query("aiserver").exact("aiservertype","transcribe").sort("ordering").searchOne()`.
   If several rows have `aiservertype="transcribe"`, the lowest `ordering` wins.
3. If none exist, fall back to the `localhost` row. That row has no `serverroot`, so the URL becomes
   `null/transcribe` and the call fails.
4. Create the `connectionbean`, call `setAiServerData(row)`, cache it, and log
   `transcribe picked llmconnection type:httpConnection selected AI server URL: ...`.

Because the connection is cached, changing the row (for example pointing `serverroot` at a local
server) needs the data reset **and** a cache clear (see `reload-list-data`):

```bash
AUTH='Authorization: Bearer adminmd5421c0af185908a6c0c40d50fd5e3f16760d5580bc'
curl -H "$AUTH" "http://localhost:8080/site/find/views/settings/lists/datamanager/list/restoredata.html?searchtype=aiserver&oemaxlevel=1"
curl -H "$AUTH" http://localhost:8080/site/find/views/settings/status/tools/clearcaches.html
```

## Call path

```
Agent job step (aiskill "autotranscribe", contextbean informaticsContext)
  -> TranscriberSkill.process(AgentContext)
       wraps in InformaticsContext, reads assetsToProcess
  -> TranscriberManager.transcribeAssets(log, assets)
       keeps only assets whose getMediaRenderType is "video" or "audio"
       for each: no "length" -> set llmerror=true, save asset, skip
  -> TranscriberManager.transcribeOneAsset(log, asset)
       find/create videotrack row, mark inprogress
  -> TranscriberManager.transcribe(asset, track)
       loop over 300 s chunks: transcode to MP3, then
  -> TranscriberManager.getTranscribedData(mp3)
  -> getLlmConnection("transcribe").callJson("/transcribe", headers, {"file": File})
  -> BaseLlmConnection.callJson(path, headers, Map)
  -> HttpSharedConnection.sharedMimePost(serverroot + "/transcribe", params)
```

The web action `TranscriberModule.transcribeAsset` (request param `assetid`) skips the skill and
`transcribeAssets` and calls `transcribe(asset, track)` itself, with its own copy of the videotrack setup.

### 1. TranscriberSkill.process

- Builds an `InformaticsContext` and reads `assetsToProcess` (the page of assets passed down by the
  Informatics Processor).
- Calls `transcribeAssets(scriptLogger, assets)`.
- Removes assets with `llmerror=true` from the list and sets the result back as `assetsToProcess`,
  so child steps skip them.
- Calls `super.process` (`BaseSkill`), which fires the step-complete status and runs the child steps.

### 2. transcribeOneAsset: the videotrack row

Searcher `videotrack`, one row per asset (`exact("assetid", id).searchOne()`):

- If the row exists with `transcribestatus=complete`, do nothing and return `false`.
- If the row is `inprogress`, skip it unless it is stale (`isStale`): `requesteddate` is missing, or it
  was more than the asset `length` plus one hour (`STALE_GRACE_MS`) ago. A stale track was abandoned,
  for example by a restart, so it is transcribed again from the start.
- Otherwise create it if needed (`assetid`, `length` copied from the asset), set `requesteddate`, set
  `sourcelang=en`, set `transcribestatus=inprogress`, and save.
- After `transcribe()`: `complete` on success, `error` on any exception. `completeddate` is always set
  and the row is saved.

A failed transcription sets `transcribestatus=error` on the track and `llmerror=true` on the asset,
so the skill drops the asset from `assetsToProcess`. The asset is not saved here: the Informatics
Processor saves the whole page at the end, as with `ClassifyManager`. Assets with `llmerror=true` are
left out of later informatics queries until the flag is cleared.

### 3. transcribe: chunking the audio

- Uses the `AudioConversionManager` for `mp3` and `createInstructions(asset, "audio.mp3")`. Input is
  `findInput(instructions)` (an existing audio rendition), or the original file if there is none.
- `length` (a Double, in seconds) sets the loop: `timeoffset = 0, 300, 600, ... < length`.
- Each chunk is transcoded with `timeoffset=<offset>`, `duration=300`, `bitrate=128`, `resample=48000`
  to `/WEB-INF/temp/<assetid>data.mp3`. The file is removed before and after each chunk.
- If the transcode fails: `OpenEditException("Could not transcode audio")`.

## The HTTP request

One request per 5-minute chunk:

```
POST https://transcribe.emediaworkspace.com/transcribe
Authorization: Bearer demo
Content-Type: multipart/form-data; boundary=...

--boundary
Content-Disposition: form-data; name="file"; filename="<assetid>data.mp3"
Content-Type: application/octet-stream

<128 kbps, 48 kHz MP3 bytes, up to 300 seconds>
--boundary--
```

- URL: `getServerRoot() + "/transcribe"` = `serverroot` (+ `serverpathprefix` if set) + `/transcribe`.
- Auth: the manager puts `Authorization: Bearer <serverapikey>` in a header map. `callJson` adds it
  to the connection's shared headers (`putSharedHeader`), so it stays on that cached connection.
- Body: `HttpMimeBuilder` adds one part named `file` from the `java.io.File`, with type
  `application/octet-stream` and the file name as the filename. There are no other form fields.

Test the server directly with:

```bash
curl -H 'Authorization: Bearer demo' -F "file=@sample.mp3" https://transcribe.emediaworkspace.com/transcribe
```

### Expected response

HTTP 200 with a JSON object (parsed with json-simple):

```json
{
  "language": "en",
  "num_speakers": 2,
  "segments": [
    { "start": 0.0, "end": 4.2, "text": "Hello and welcome.", "speaker": "SPEAKER_00" },
    { "start": 4.2, "end": 9.8, "text": "Thanks for having me.", "speaker": "SPEAKER_01" }
  ]
}
```

| Field | Required | Stored as |
|---|---|---|
| `language` | no | `videotrack.sourcelang` (overrides the default `en`) |
| `num_speakers` | no | `videotrack.speakercount` (any number, stored as a long) |
| `segments` | **yes** | one cue in `videotrack.captions` per segment |
| `segments[].start`, `end` | **yes** | seconds within the chunk. Any JSON number: `0` and `0.0` both work |
| `segments[].text` | yes | cue `cliplabel` |
| `segments[].speaker` | no | cue `speaker`, `"Unknown"` if missing |

Errors:

- Status other than 200: the status code and body are logged
  (`Error: <url> returned status code: ...`) and `OpenEditException("Could not call /transcribe")` is thrown.
- A JSON array instead of an object: `getRawResponse()` is null, so `"Transcriber server error"` is thrown.
- Any exception stops the remaining chunks and sets `transcribestatus=error`. Captions from chunks
  that already finished stay saved.

## How segments become captions

For each segment, a cue map is added to one `captions` collection that covers the whole asset:

| Cue key | Value |
|---|---|
| `cliplabel` | `text` |
| `speaker` | `speaker` or `Unknown` |
| `timecodestart` | `round((timeoffset + start) * 1000)` in ms from the start of the asset |
| `timecodelength` | `round((end - start) * 1000)` in ms |

After each chunk, `captions` is set on the track and the `videotrack` row is saved, so the transcript
fills in 5 minutes at a time. Field definitions are in `plugins/catalog/html/data/fields/videotrack.xml`.

## Gotchas

- Assets with no `length` are flagged `llmerror` and skipped. Run metadata extraction first.
- Only `video` and `audio` render types are processed. Other assets pass through untouched.
- A track stuck in `inprogress` is only retried once it is stale (asset length plus one hour after
  `requesteddate`). To retry sooner, set `transcribestatus` to `error` or delete the row.
- A track in `error` is retried the next time the asset is processed, but the asset has `llmerror=true`
  by then, so it is only picked up again after that flag is cleared.
- The temp file name is based on the asset id, so two transcriptions of the same asset at the same
  time would overwrite each other's chunk.
- After editing any of the Java files above, run `bin/restart.sh`.
