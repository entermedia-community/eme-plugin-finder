package org.entermediadb.whatsapp;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.entermediadb.ai.AgentContext;
import org.entermediadb.ai.BaseAiManager;
import org.entermediadb.ai.llm.BaseAgentContext;
import org.entermediadb.ai.llm.LlmConnection;
import org.entermediadb.ai.llm.LlmResponse;
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
		"EAAMddinMeh4BRqNLiGMMRUZAts9RY07wJC53gdsqVmIEfhWQSiFo9Eh4eJmljC7yBhZBmmA0sbqDpmOzZANmZCpwl95v53yfdZA7zxsIpURov2PeRUMF17FfZCpY5TswCbZBYoAxSRAvj0JC0dYB4ZB7wTbMfIiarLK2ZAxZC6AIiKqxJ3YDb0YACihJcgOLg1zJr1J80S0FpFWEdXcQiE4jMvXVX1B4HLFTTiTL8gCPqZAfd8wvxqd5xbpAP0PcGT4o3VBPuJmvjIwvhx6ou5OGV1zD3SbNAZDZD";
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

			String messageType = message.getMessageType();
			if ("interactive".equals(messageType))
			{
				log.info("Received interactive message, ignoring for now.");
				inReq.getResponse().setStatus(200);
				return;
			}

			LlmConnection llmconnection = getMediaArchive().getLlmConnection("parse_ride_request");
			AgentContext context = new BaseAgentContext();
			context.put("userquery", message.getMessageText());

			LlmResponse response = llmconnection.callToolsFunction(context, "parse_ride_message");

			String functionName = response.getFunctionName();
			JSONObject functionArgs = response.getFunctionArguments();

			JSONObject replyBody = null;

			if ("request_clarification".equals(functionName))
			{
				replyBody = WhatsAppResponse.buildTextReply(message, "Sorry, I didn't understand your request. Could you please clarify?");
			}
			else
			{
				if ("book_ride".equals(functionName))
				{
					replyBody = WhatsAppResponse.buildTemplateReply(message, "ride_request_preview", functionArgs);
				}
				else if ("check_status".equals(functionName))
				{
					// TODO: implement check status
				}
			}

			if (replyBody == null)
			{
				log.warn("No reply generated for function: " + functionName);
				inReq.getResponse().setStatus(200);
				return;
			}

			log.info("Generated reply for function " + functionName + ": " + replyBody.toJSONString());

			String url = "https://graph.facebook.com/v25.0/" + PHONE_NUMBER_ID + "/messages";
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
