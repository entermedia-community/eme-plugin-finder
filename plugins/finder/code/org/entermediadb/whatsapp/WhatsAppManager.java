package org.entermediadb.whatsapp;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.entermediadb.ai.BaseAiManager;
import org.entermediadb.ai.llm.LlmConnection;
import org.json.simple.JSONObject;
import org.openedit.OpenEditException;
import org.openedit.WebPageRequest;
import org.openedit.util.HttpSharedConnection;

public class WhatsAppManager extends BaseAiManager
{
	private static final Log log = LogFactory.getLog(WhatsAppManager.class);

	// to be moved to config later
	private String VERIFY_TOKEN = "TEST_LOCAL_TOKEN";
	private String ACCESS_TOKEN =
		"EAAMddinMeh4BRtN9aiY23I8p9cxZBdSXkZAn8FPnLDlqcnthQV3iKS2K6ZCow4yYHFHUrj8WiuOFnpuvbljngoYaTalqSZBhZBY77VWrrkk4G7FwnJAci50ZBRRlEZCJZB5LKyC6YxEXoFeTMuKd9cFxBivA74h3ZBgzi7DHqaF0GAZA2ZBFZA5y34bfbZC8GeFDiWidAW2JfIW8QZCHbSufnhPdJsbHV5Vnfd353jzvHSgwp8xGLzAqKQ9MnOUsbPjUpHqXPBWBzZCBeQigNbP7YiKLyivIQub";
	private String PHONE_NUMBER_ID = "1104936289378170";

	public void verifyWebhook(WebPageRequest inReq)
	{
		String mode = inReq.getRequestParameter("hub.mode");
		String token = inReq.getRequestParameter("hub.verify_token");
		String challenge = inReq.getRequestParameter("hub.challenge");

		if ("subscribe".equals(mode) && VERIFY_TOKEN.equals(token))
		{
			log.info("Webhook verified successfully.");
			inReq.getResponse().setStatus(200);
			try
			{
				inReq.getResponse().setContentType("application/json");
				inReq.putPageValue("challenge", challenge);
			}
			catch (OpenEditException e)
			{
				log.error("Error writing challenge to response", e);
			}
		}
		else
		{
			log.warn("Webhook verification failed. Invalid token.");
			inReq.getResponse().setStatus(403);
		}
	}

	public void receiveMessage(WebPageRequest inReq)
	{
		Map body = inReq.getJsonRequest();

		if (body == null || body.get("object") == null)
		{
			inReq.getResponse().setStatus(404);
			return;
		}

		WhatsAppMessage message = new WhatsAppMessage(body);

		if (message != null)
		{
			log.info("Intercepted message from " + message.getSenderPhone() + ": " + message.getMessageText());

			// webapp/mediadb/ai/default/calls/whatsapp/parsemessage.json
			// LlmConnection llm = getMediaArchive().getLlmConnection("documentsplitasset");
			// TODO

			String replyText = "Server received: \"" + message.getMessageText() + "\".";
			JSONObject replyBody = WhatsAppResponse.buildTextReply(message, replyText);

			String url = "https://graph.facebook.com/v19.0/" + PHONE_NUMBER_ID + "/messages";
			HttpSharedConnection connection = new HttpSharedConnection();
			connection.addSharedHeader("Authorization", "Bearer " + ACCESS_TOKEN);
			CloseableHttpResponse resp = null;
			try
			{
				resp = connection.sharedPostWithJson(url, replyBody);
				log.info("Reply sent successfully.");
			}
			catch (Exception e)
			{
				log.error("Failed to send reply", e);
			}
			finally
			{
				connection.release(resp);
			}
		}

		// Meta requires a 200 OK response within seconds, or they will retry sending the payload.
		inReq.getResponse().setStatus(200);
	}
}
