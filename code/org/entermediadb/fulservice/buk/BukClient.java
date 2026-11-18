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
		if (!isConfigured())
		{
			throw new BukException("no_token", "Environment variable " + TOKEN_ENV + " is not set");
		}
		URI uri = URI.create(fieldBaseUrl + "/employees?page=" + inPage + "&page_size=" + PAGE_SIZE);
		BukException last = null;
		for (int attempt = 1; attempt <= 3; attempt++)
		{
			try
			{
				HttpRequest req = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(30)).header("auth_token", fieldToken).header("Accept", "application/json").GET().build();
				HttpResponse<String> res = fieldHttp.send(req, HttpResponse.BodyHandlers.ofString());
				int code = res.statusCode();
				if (code == 200)
				{
					return (JSONObject) new JSONParser().parse(res.body());
				}
				last = new BukException("http_" + code, "BUK returned HTTP " + code);
				if (code != 429 && code < 500)
				{
					throw last; // auth / client errors do not heal by retrying
				}
			}
			catch (BukException ex)
			{
				throw ex;
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
