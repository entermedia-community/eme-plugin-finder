package org.entermediadb.fulservice.scl;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Read-only client for the Subcontrata Ley portal (www5.subcontrataley.cl). Logs in through
 * ajax/login.ajax.php (act 1001), keeps the session cookie, then posts act codes to
 * ajax/contratista.ajax.php. Credentials come only from the server environment and are never logged.
 * See .agents/skills/gathering-scl-data/SKILL.md.
 */
public class SclClient
{
	public static final String USER_ENV = "FULSERVICE_SCL_USER";
	public static final String PASS_ENV = "FULSERVICE_SCL_PASSWORD";
	public static final String BASE_URL_ENV = "FULSERVICE_SCL_URL";
	public static final String DEFAULT_BASE_URL = "https://www5.subcontrataley.cl";

	public static final String ACT_LOGIN = "1001";
	/** contratista.ajax.php act whose response renders the contractor table */
	public static final String ACT_CONTRATISTA = "5004";

	protected final HttpClient fieldHttp;
	protected final String fieldUser;
	protected final String fieldPass;
	protected final String fieldBaseUrl;
	protected boolean fieldLoggedIn;

	public SclClient()
	{
		CookieManager cookies = new CookieManager(null, CookiePolicy.ACCEPT_ALL); // one jar per client = one session per run
		fieldHttp = HttpClient.newBuilder().cookieHandler(cookies).followRedirects(HttpClient.Redirect.NORMAL).connectTimeout(Duration.ofSeconds(15)).build();
		fieldUser = System.getenv(USER_ENV);
		fieldPass = System.getenv(PASS_ENV);
		String base = System.getenv(BASE_URL_ENV);
		fieldBaseUrl = (base == null || base.isBlank()) ? DEFAULT_BASE_URL : base.replaceAll("/+$", "");
	}

	public boolean isConfigured()
	{
		return fieldUser != null && !fieldUser.isBlank() && fieldPass != null && !fieldPass.isBlank();
	}

	public static class SclException extends Exception
	{
		protected final String fieldCode;

		public SclException(String inCode, String inMessage)
		{
			super(inMessage);
			fieldCode = inCode;
		}

		public String getCode()
		{
			return fieldCode;
		}
	}

	public void login() throws SclException
	{
		if (!isConfigured())
		{
			throw new SclException("no_credentials", "Environment variables " + USER_ENV + " / " + PASS_ENV + " are not set");
		}
		Map<String, String> form = new LinkedHashMap<>();
		form.put("act", ACT_LOGIN);
		form.put("user", fieldUser);
		form.put("pass", fieldPass);
		form.put("w", "1192"); // the portal ignores the login silently (empty reply) when screen w/h are missing
		form.put("h", "929");
		byte[] reply = post("login.ajax.php", form);
		String text = new String(reply, StandardCharsets.UTF_8);
		if (!text.replace(" ", "").contains("\"validation\":true"))
		{
			throw new SclException("auth", "SCL login was not accepted (bad credentials or changed login flow)");
		}
		fieldLoggedIn = true;
	}

	/** @return the raw response body, untouched */
	public byte[] contratista() throws SclException
	{
		return data(ACT_CONTRATISTA);
	}

	/** Posts an act to contratista.ajax.php; logs in first, and once more if the session looks lost. */
	public byte[] data(String inAct) throws SclException
	{
		if (!fieldLoggedIn)
		{
			login();
		}
		Map<String, String> form = new LinkedHashMap<>();
		form.put("act", inAct);
		byte[] body = post("contratista.ajax.php", form);
		if (looksLikeLogin(body))
		{
			login();
			body = post("contratista.ajax.php", form);
			if (looksLikeLogin(body))
			{
				throw new SclException("auth", "SCL rejected the session after login (bad credentials or changed login flow)");
			}
		}
		return body;
	}

	protected boolean looksLikeLogin(byte[] inBody)
	{
		if (inBody == null || inBody.length == 0)
		{
			return true;
		}
		// data replies are JSON; an HTML page here means the session was lost and the portal sent the login page
		String head = new String(inBody, 0, Math.min(inBody.length, 400), StandardCharsets.UTF_8).trim().toLowerCase();
		return head.startsWith("<");
	}

	protected byte[] post(String inEndpoint, Map<String, String> inForm) throws SclException
	{
		String body = inForm.entrySet().stream().map(e -> URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8) + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8)).collect(Collectors.joining("&"));
		URI uri = URI.create(fieldBaseUrl + "/ajax/" + inEndpoint);
		SclException last = null;
		for (int attempt = 1; attempt <= 3; attempt++)
		{
			try
			{
				HttpRequest req = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(60))
						.header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
						.header("X-Requested-With", "XMLHttpRequest")
						.header("Referer", fieldBaseUrl + "/")
						.header("User-Agent", "Mozilla/5.0 (compatible; FulserviceSync)")
						.POST(HttpRequest.BodyPublishers.ofString(body)).build();
				HttpResponse<byte[]> res = fieldHttp.send(req, HttpResponse.BodyHandlers.ofByteArray());
				int code = res.statusCode();
				if (code == 200)
				{
					return res.body();
				}
				last = new SclException("http_" + code, "SCL returned HTTP " + code + " for " + inEndpoint);
				if (code != 429 && code < 500)
				{
					throw last;
				}
			}
			catch (SclException ex)
			{
				throw ex;
			}
			catch (InterruptedException ex)
			{
				Thread.currentThread().interrupt();
				throw new SclException("interrupted", "Import interrupted");
			}
			catch (Exception ex)
			{
				last = new SclException("network", "SCL request failed: " + ex.getClass().getSimpleName());
			}
			try
			{
				Thread.sleep(1000L * attempt * attempt);
			}
			catch (InterruptedException ex)
			{
				Thread.currentThread().interrupt();
				throw new SclException("interrupted", "Import interrupted");
			}
		}
		throw last;
	}
}
