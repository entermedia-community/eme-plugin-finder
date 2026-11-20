package org.entermediadb.fulservice.buk;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

/**
 * Read-only client for the BUK Chile API (see .agents/skills/gathering-buk/SKILL.md). The token
 * comes only from the server environment (FULSERVICE_BUK_API_TOKEN) and is never logged.
 */
public class BukClient
{
	public static final String TOKEN_ENV = "FULSERVICE_BUK_API_TOKEN";
	public static final String BASE_URL_ENV = "FULSERVICE_BUK_BASE_URL";
	public static final String DEFAULT_BASE_URL = "https://fulservice.buk.cl/api/v1/chile";
	public static final int PAGE_SIZE = 100;

	protected final HttpClient fieldHttp = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();
	protected String fieldToken;
	protected String fieldBaseUrl;

	public BukClient() {
		fieldToken = System.getenv(TOKEN_ENV);
		String base = System.getenv(BASE_URL_ENV);
		fieldBaseUrl = (base == null || base.isBlank()) ? DEFAULT_BASE_URL : base.replaceAll("/+$", "");
	}

	public boolean isConfigured()
	{
		return fieldToken != null && !fieldToken.isBlank();
	}

	/** @throws BukException with a stable error code (no_token, http_401, http_429, network, ...) */
	public JSONObject employees(int inPage) throws BukException
	{
		return getJson(URI.create(fieldBaseUrl + "/employees?page=" + inPage + "&page_size=" + PAGE_SIZE));
	}

	/** Files BUK holds for one employee: {"employee_files":[{file_id, filename, path, created_at}]} (no download URL) */
	public JSONObject docs(Object inEmployeeId) throws BukException
	{
		return getJson(URI.create(apiRoot() + "/employees/" + inEmployeeId + "/docs"));
	}

	/** Bytes of one employee file. A redirect (e.g. to S3) is followed WITHOUT the auth_token header. */
	public byte[] downloadDoc(Object inEmployeeId, Object inFileId) throws BukException
	{
		URI uri = URI.create(apiRoot() + "/employees/" + inEmployeeId + "/docs/" + inFileId);
		HttpResponse<byte[]> res = send(uri, true, HttpResponse.BodyHandlers.ofByteArray());
		if (res.statusCode() >= 300 && res.statusCode() < 400)
		{
			String loc = res.headers().firstValue("Location").orElse(null);
			if (loc == null)
			{
				throw new BukException("http_" + res.statusCode(), "Redirect without Location");
			}
			res = send(uri.resolve(loc), false, HttpResponse.BodyHandlers.ofByteArray());
		}
		if (res.statusCode() != 200)
		{
			throw new BukException("http_" + res.statusCode(), "BUK returned HTTP " + res.statusCode());
		}
		return res.body();
	}

	/** The documents endpoints live under /api/v1 (no country segment) */
	protected String apiRoot()
	{
		return fieldBaseUrl.replaceAll("/chile$", "");
	}

	protected JSONObject getJson(URI inUri) throws BukException
	{
		HttpResponse<String> res = send(inUri, true, HttpResponse.BodyHandlers.ofString());
		if (res.statusCode() != 200)
		{
			throw new BukException("http_" + res.statusCode(), "BUK returned HTTP " + res.statusCode());
		}
		try
		{
			return (JSONObject) new JSONParser().parse(res.body());
		}
		catch (Exception ex)
		{
			throw new BukException("bad_json", "BUK returned an unreadable body");
		}
	}

	/** Retries 429, 5xx and network errors (3 attempts); any other status is returned for the caller to judge */
	protected <T> HttpResponse<T> send(URI inUri, boolean inAuth, HttpResponse.BodyHandler<T> inHandler) throws BukException
	{
		if (inAuth && !isConfigured())
		{
			throw new BukException("no_token", "Environment variable " + TOKEN_ENV + " is not set");
		}
		BukException last = null;
		for (int attempt = 1; attempt <= 3; attempt++)
		{
			try
			{
				HttpRequest.Builder b = HttpRequest.newBuilder(inUri).timeout(Duration.ofSeconds(60)).GET();
				if (inAuth)
				{
					b.header("auth_token", fieldToken).header("Accept", "application/json");
				}
				HttpResponse<T> res = fieldHttp.send(b.build(), inHandler);
				int code = res.statusCode();
				if (code != 429 && code < 500)
				{
					return res;
				}
				last = new BukException("http_" + code, "BUK returned HTTP " + code);
			}
			catch (InterruptedException ex)
			{
				Thread.currentThread().interrupt();
				throw new BukException("interrupted", "Import interrupted");
			}
			catch (Exception ex)
			{
				last = new BukException("network", "BUK request failed: " + ex.getClass().getSimpleName());
			}
			try
			{
				Thread.sleep(1000L * attempt * attempt);
			}
			catch (InterruptedException ex)
			{
				Thread.currentThread().interrupt();
				throw new BukException("interrupted", "Import interrupted");
			}
		}
		throw last;
	}

	public static class BukException extends Exception
	{
		protected final String fieldCode;

		public BukException(String inCode, String inMessage) {
			super(inMessage);
			fieldCode = inCode;
		}

		public String getCode()
		{
			return fieldCode;
		}
	}
}
