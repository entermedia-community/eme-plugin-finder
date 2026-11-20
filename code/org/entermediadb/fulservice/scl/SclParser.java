package org.entermediadb.fulservice.scl;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * Offline parser for a raw SCL ajax response (JSON wrapping an HTML fragment, or plain HTML).
 * Rows are keyed by header text, never by column position. Never touches the network, so a saved
 * capture can be re-parsed at any time.
 */
public class SclParser
{
	public static class Table
	{
		public final List<String> headers = new ArrayList<>();
		public final List<Map<String, String>> rows = new ArrayList<>();
	}

	/** @throws SclClient.SclException code "structure" when there is no table or a required header is missing */
	public Table parse(byte[] inBody, List<String> inRequiredHeaders) throws SclClient.SclException
	{
		String text = new String(inBody, StandardCharsets.UTF_8);
		String html = extractHtml(text);
		if (html == null)
		{
			throw new SclClient.SclException("structure", "No <table> found in the SCL response");
		}
		Document doc = Jsoup.parse(html);
		Element table = null;
		for (Element t : doc.select("table"))
		{
			if (table == null || t.select("tr").size() > table.select("tr").size())
			{
				table = t;
			}
		}
		if (table == null)
		{
			throw new SclClient.SclException("structure", "No <table> found in the SCL response");
		}
		Elements headRows = table.select("thead tr");
		Element headRow = headRows.isEmpty() ? table.select("tr").first() : headRows.last();
		Table out = new Table();
		for (Element h : headRow.children())
		{
			out.headers.add(clean(h.text()));
		}
		if (out.headers.isEmpty() || out.headers.stream().allMatch(String::isEmpty))
		{
			throw new SclClient.SclException("structure", "SCL table has no readable headers");
		}
		if (inRequiredHeaders != null)
		{
			List<String> missing = new ArrayList<>();
			for (String need : inRequiredHeaders)
			{
				if (out.headers.stream().noneMatch(x -> x.equalsIgnoreCase(need)))
				{
					missing.add(need);
				}
			}
			if (!missing.isEmpty())
			{
				throw new SclClient.SclException("structure", "SCL table is missing header(s) " + missing + "; found " + out.headers);
			}
		}
		Elements bodyRows = table.select("tbody tr");
		if (bodyRows.isEmpty())
		{
			bodyRows = new Elements(table.select("tr"));
			bodyRows.remove(0);
		}
		for (Element tr : bodyRows)
		{
			Elements cells = tr.children().select("td");
			if (cells.isEmpty())
			{
				continue;
			}
			Map<String, String> row = new LinkedHashMap<>();
			for (int i = 0; i < out.headers.size() && i < cells.size(); i++)
			{
				String key = out.headers.get(i).isEmpty() ? "col" + i : out.headers.get(i);
				row.put(key, clean(cells.get(i).text()));
				Element link = cells.get(i).selectFirst("a[href]");
				if (link != null)
				{
					row.put(key + "__href", link.attr("href"));
				}
			}
			for (org.jsoup.nodes.Attribute a : tr.attributes())
			{
				if (a.getKey().startsWith("data-"))
				{
					row.put("@" + a.getKey(), a.getValue());
				}
			}
			out.rows.add(row);
		}
		return out;
	}

	/** Column order of "Tablero resumen" (page header: área/faena, sin subir, por revisar, aprobado, rechazado, anulado, estado) */
	public static final List<String> TABLERO_HEADERS = List.of("area", "sin subir", "por revisar", "aprobado", "rechazado", "anulado", "estado");
	/** Status code the portal puts in each count cell's onclick, in the same column order as TABLERO_HEADERS[1..5] */
	protected static final String[] TABLERO_CODES = { "99", "2", "4", "3", "1" };
	protected static final java.util.regex.Pattern DETAIL_CALL = java.util.regex.Pattern.compile("verDetalleResumenActividad\\('([^']*)','([^']*)',(\\d+)\\)");

	/**
	 * Parses the DataTables server-side JSON returned by contratista.ajax.php act 5004
	 * ({draw, recordsTotal, recordsFiltered, data:[[area, n, n, n, n, n, estado], ...]}).
	 * Each count cell is an HTML div whose onclick carries base64 ids (area, contractor) and the status code;
	 * those ids are kept for drilling into the detail lists later.
	 */
	public Table parseTablero(byte[] inBody) throws SclClient.SclException
	{
		Object json;
		try
		{
			json = new JSONParser().parse(new String(inBody, StandardCharsets.UTF_8));
		}
		catch (Exception ex)
		{
			throw new SclClient.SclException("structure", "SCL tablero reply is not JSON");
		}
		if (!(json instanceof JSONObject) || !(((JSONObject) json).get("data") instanceof JSONArray))
		{
			throw new SclClient.SclException("structure", "SCL tablero reply has no \"data\" array");
		}
		JSONObject root = (JSONObject) json;
		JSONArray data = (JSONArray) root.get("data");
		Object filtered = root.get("recordsFiltered");
		if (filtered instanceof Number && ((Number) filtered).intValue() != data.size())
		{
			throw new SclClient.SclException("structure", "SCL tablero is incomplete: recordsFiltered=" + filtered + " but " + data.size() + " rows returned (paging?)");
		}
		Table out = new Table();
		out.headers.addAll(TABLERO_HEADERS);
		int n = 0;
		for (Object o : data)
		{
			n++;
			JSONArray cells = o instanceof JSONArray ? (JSONArray) o : null;
			if (cells == null || cells.size() != TABLERO_HEADERS.size())
			{
				throw new SclClient.SclException("structure", "SCL tablero row " + n + " has " + (cells == null ? 0 : cells.size()) + " cells, expected " + TABLERO_HEADERS.size());
			}
			Map<String, String> row = new LinkedHashMap<>();
			row.put("area", clean(Jsoup.parse(String.valueOf(cells.get(0))).text()));
			String areaId = null, contractorId = null;
			for (int i = 1; i <= 5; i++)
			{
				String cell = String.valueOf(cells.get(i));
				row.put(TABLERO_HEADERS.get(i), clean(Jsoup.parse(cell).text()));
				java.util.regex.Matcher m = DETAIL_CALL.matcher(cell);
				if (!m.find() || !TABLERO_CODES[i - 1].equals(m.group(3)))
				{
					throw new SclClient.SclException("structure", "SCL tablero row " + n + " column '" + TABLERO_HEADERS.get(i) + "' no longer carries status code " + TABLERO_CODES[i - 1]);
				}
				areaId = decode(m.group(1));
				contractorId = decode(m.group(2));
			}
			row.put("estado", clean(Jsoup.parse(String.valueOf(cells.get(6))).text()));
			row.put("portalAreaId", areaId);
			row.put("portalContractorId", contractorId);
			out.rows.add(row);
		}
		return out;
	}

	protected static String decode(String inBase64)
	{
		try
		{
			return new String(java.util.Base64.getDecoder().decode(inBase64), StandardCharsets.UTF_8);
		}
		catch (IllegalArgumentException ex)
		{
			return inBase64;
		}
	}

	/** Longest string value containing a table, or the whole body when it is not JSON */
	protected String extractHtml(String inText)
	{
		Object json;
		try
		{
			json = new JSONParser().parse(inText);
		}
		catch (Exception ex)
		{
			return inText.toLowerCase().contains("<table") ? inText : null;
		}
		return longestTableString(json, null);
	}

	protected String longestTableString(Object inNode, String inBest)
	{
		String best = inBest;
		if (inNode instanceof String)
		{
			String s = (String) inNode;
			if (s.toLowerCase().contains("<table") && (best == null || s.length() > best.length()))
			{
				best = s;
			}
		}
		else if (inNode instanceof JSONObject)
		{
			for (Object v : ((JSONObject) inNode).values())
			{
				best = longestTableString(v, best);
			}
		}
		else if (inNode instanceof JSONArray)
		{
			for (Object v : (JSONArray) inNode)
			{
				best = longestTableString(v, best);
			}
		}
		return best;
	}

	protected static String clean(String inText)
	{
		return inText == null ? "" : inText.replace(' ', ' ').replaceAll("\\s+", " ").trim();
	}
}
