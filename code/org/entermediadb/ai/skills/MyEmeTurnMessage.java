package org.entermediadb.ai.skills;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.entermediadb.markdown.MarkdownToQuill;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 * One chat message built up from the OpenCode events of a turn.
 *
 * Reply text, the tools OpenCode used and any error are kept in the order they happened. ops() is the
 * whole message as Quill ops. nextPatch() says what changed since the last patch: a messageappend when
 * the message only grew at the end, otherwise a messagereplace with the whole message. The caller
 * broadcasts that, and the finished message is the same ops().
 *
 * Not thread safe; one thread feeds events and asks for patches.
 */
public class MyEmeTurnMessage
{
	public static final String APPEND = "messageappend";
	public static final String REPLACE = "messagereplace";

	protected static final String NOTE_COLOR = "#777777";
	protected static final String ERROR_COLOR = "#c62828";

	/** A piece of the message: reply text (markdown), a line about a tool, or an error */
	protected static class Segment
	{
		String kind; // text, tool, note or error
		StringBuilder text = new StringBuilder(); // text and error
		String tool; // tool name
		String target; // what the tool works on, such as a file name
		String failure; // why the tool failed
		boolean done; // the tool finished
	}

	protected final List<Segment> fieldSegments = new ArrayList<Segment>();
	protected final Map<String, Segment> fieldByKey = new HashMap<String, Segment>();
	protected final MarkdownToQuill fieldMarkdown = new MarkdownToQuill();

	protected JSONArray fieldSentOps = new JSONArray();
	protected String fieldSentStatus;
	protected int fieldSeq;

	protected boolean fieldTurnStarted; // saw an event of this turn, so an earlier turn's end can't finish it
	protected boolean fieldFinished;
	protected String fieldOutcome; // succeeded, failed or interrupted
	protected Segment fieldOpenText; // the reply text that is still arriving

	// ---------------------------------------------------------------------
	// Events
	// ---------------------------------------------------------------------

	/** Applies one OpenCode event for this session; events that do not change the message are ignored */
	public void apply(JSONObject inEvent)
	{
		String type = string(inEvent.get("type"));
		JSONObject data = object(inEvent.get("data"));
		if (type == null || data == null || fieldFinished)
		{
			return;
		}
		switch (type)
		{
		case "session.inbox.enqueued":
		case "session.execution.started":
		case "session.step.started":
			fieldTurnStarted = true;
			break;
		case "session.text.delta":
			fieldTurnStarted = true;
			Segment arriving = textSegment(data);
			if (data.get("delta") != null)
			{
				arriving.text.append(string(data.get("delta")));
			}
			fieldOpenText = arriving; // open until its ended event
			break;
		case "session.text.ended":
			fieldTurnStarted = true;
			// The ended event carries the whole part, so it corrects anything a delta missed
			Segment part = textSegment(data);
			if (data.get("text") != null)
			{
				part.text = new StringBuilder(string(data.get("text")));
			}
			if (fieldOpenText == part)
			{
				fieldOpenText = null;
			}
			break;
		case "session.tool.input.started":
			fieldTurnStarted = true;
			toolSegment(data).tool = string(data.get("name"));
			break;
		case "session.tool.called":
			fieldTurnStarted = true;
			toolSegment(data).target = describe(object(data.get("input")));
			break;
		case "session.tool.success":
			toolSegment(data).done = true;
			break;
		case "session.tool.failed":
			Segment tool = toolSegment(data);
			tool.done = true;
			JSONObject error = object(data.get("error"));
			tool.failure = error != null && error.get("message") != null ? string(error.get("message")) : "failed";
			break;
		case "session.execution.succeeded":
			finish("succeeded", null);
			break;
		case "session.execution.failed":
			JSONObject failure = object(data.get("error"));
			finish("failed", failure != null && failure.get("message") != null ? string(failure.get("message")) : "OpenCode reported an error");
			break;
		case "session.execution.interrupted":
			finish("interrupted", "The session was interrupted before it finished");
			break;
		default:
			break;
		}
	}

	/** Ends the message with an error that did not come from an event, such as a timeout or a refused prompt */
	public void fail(String inMessage)
	{
		if (!fieldFinished)
		{
			fieldTurnStarted = true;
			finish("failed", inMessage);
		}
	}

	/** Ends this message because a newer message in the chat took over the session; the reply continues there */
	public void handOff()
	{
		if (!fieldFinished)
		{
			fieldTurnStarted = true;
			note("Continued in the next reply.");
			finish("superseded", null);
		}
	}

	/** Adds a muted line of its own, such as why a tool was refused */
	public void note(String inText)
	{
		Segment note = new Segment();
		note.kind = "note";
		note.text.append(inText);
		fieldSegments.add(note);
	}

	protected void finish(String inOutcome, String inError)
	{
		if (!fieldTurnStarted)
		{
			return; // the end of an earlier turn, not ours
		}
		fieldFinished = true;
		fieldOutcome = inOutcome;
		fieldOpenText = null;
		if (inError != null)
		{
			Segment error = new Segment();
			error.kind = "error";
			error.text.append(inError);
			fieldSegments.add(error);
		}
	}

	protected Segment textSegment(JSONObject inData)
	{
		String key = "text:" + string(inData.get("assistantMessageID")) + ":" + string(inData.get("ordinal"));
		Segment segment = fieldByKey.get(key);
		if (segment == null)
		{
			segment = new Segment();
			segment.kind = "text";
			fieldByKey.put(key, segment);
			fieldSegments.add(segment);
		}
		return segment;
	}

	protected Segment toolSegment(JSONObject inData)
	{
		String key = "tool:" + string(inData.get("id"));
		Segment segment = fieldByKey.get(key);
		if (segment == null)
		{
			segment = new Segment();
			segment.kind = "tool";
			fieldByKey.put(key, segment);
			fieldSegments.add(segment);
		}
		return segment;
	}

	/** What a tool is working on, from its input. Paths are cut to the file name so the server's folders never show. */
	protected String describe(JSONObject inInput)
	{
		if (inInput == null)
		{
			return null;
		}
		for (String key : new String[] { "path", "filePath", "file_path", "pattern", "url", "query" })
		{
			String value = string(inInput.get(key));
			if (value == null || value.isEmpty())
			{
				continue;
			}
			if (key.toLowerCase().contains("path"))
			{
				value = new File(value).getName();
			}
			return value.length() > 60 ? value.substring(0, 60) + "…" : value;
		}
		return null;
	}

	// ---------------------------------------------------------------------
	// The message
	// ---------------------------------------------------------------------

	public boolean isFinished()
	{
		return fieldFinished;
	}

	public String getOutcome()
	{
		return fieldOutcome;
	}

	/** done, error, streaming (reply text still arriving) or working (anything else, such as a tool running) */
	public String getStatus()
	{
		if (fieldFinished)
		{
			return "succeeded".equals(fieldOutcome) || "superseded".equals(fieldOutcome) ? "done" : "error";
		}
		return fieldOpenText != null ? "streaming" : "working";
	}

	/** The whole message so far as Quill ops */
	@SuppressWarnings("unchecked")
	public JSONArray ops()
	{
		JSONArray ops = new JSONArray();
		for (Segment segment : fieldSegments)
		{
			switch (segment.kind)
			{
			case "text":
				ops.addAll(fieldMarkdown.toOps(segment.text.toString()));
				break;
			case "tool":
				ops.addAll(line(toolLine(segment), false));
				break;
			case "note":
				ops.addAll(line(segment.text.toString(), false));
				break;
			case "error":
				ops.addAll(line(segment.text.toString(), true));
				break;
			default:
				break;
			}
		}
		return ops;
	}

	/** The reply text without formatting, for previews and notifications */
	public String plainText()
	{
		StringBuilder plain = new StringBuilder();
		for (Segment segment : fieldSegments)
		{
			if ("text".equals(segment.kind) || "error".equals(segment.kind))
			{
				if (plain.length() > 0)
				{
					plain.append("\n");
				}
				plain.append(segment.text);
			}
		}
		return plain.toString().trim();
	}

	protected String toolLine(Segment inTool)
	{
		StringBuilder line = new StringBuilder(inTool.tool == null ? "tool" : inTool.tool);
		if (inTool.target != null)
		{
			line.append(" ").append(inTool.target);
		}
		if (inTool.failure != null)
		{
			line.append(" — ").append(inTool.failure);
		}
		else if (!inTool.done)
		{
			line.append("…");
		}
		return line.toString();
	}

	/** A one-line note in muted type (or red for an error), ended by an ordinary newline */
	@SuppressWarnings("unchecked")
	protected JSONArray line(String inText, boolean inError)
	{
		JSONObject attributes = new JSONObject();
		if (inError)
		{
			attributes.put("bold", Boolean.TRUE);
		}
		else
		{
			attributes.put("italic", Boolean.TRUE);
		}
		attributes.put("color", inError ? ERROR_COLOR : NOTE_COLOR);
		JSONObject text = new JSONObject();
		text.put("insert", inText);
		text.put("attributes", attributes);
		JSONObject newline = new JSONObject();
		newline.put("insert", "\n");
		JSONArray ops = new JSONArray();
		ops.add(text);
		ops.add(newline);
		return ops;
	}

	// ---------------------------------------------------------------------
	// Patches
	// ---------------------------------------------------------------------

	/**
	 * What to broadcast now, or null when nothing changed since the last patch. The result holds
	 * {@code command} (messageappend or messagereplace), {@code ops}, {@code seq} (1, 2, 3... per message, so a
	 * client can tell it missed one) and {@code status}.
	 */
	@SuppressWarnings("unchecked")
	public JSONObject nextPatch()
	{
		JSONArray ops = ops();
		String status = getStatus();
		if (ops.isEmpty() && fieldSentOps.isEmpty())
		{
			return null; // nothing to show yet; leave the "Working..." message alone rather than blank it
		}
		boolean opsChanged = !ops.equals(fieldSentOps);
		if (!opsChanged && status.equals(fieldSentStatus))
		{
			return null;
		}

		JSONObject patch = new JSONObject();
		boolean grewAtTheEnd = !fieldSentOps.isEmpty() && ops.size() >= fieldSentOps.size() && ops.subList(0, fieldSentOps.size()).equals(fieldSentOps);
		if (grewAtTheEnd)
		{
			patch.put("command", APPEND);
			JSONArray added = new JSONArray();
			added.addAll(ops.subList(fieldSentOps.size(), ops.size()));
			patch.put("ops", added);
		}
		else
		{
			patch.put("command", REPLACE);
			patch.put("ops", ops);
		}
		patch.put("seq", Integer.valueOf(++fieldSeq));
		patch.put("status", status);

		fieldSentOps = ops;
		fieldSentStatus = status;
		return patch;
	}

	protected static String string(Object inValue)
	{
		return inValue == null ? null : inValue.toString();
	}

	protected static JSONObject object(Object inValue)
	{
		return inValue instanceof JSONObject ? (JSONObject) inValue : null;
	}
}
