package org.entermediadb.whatsapp;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.entermediadb.ai.BaseAiManager;
import org.entermediadb.websocket.chat.ChatBroadcastListener;
import org.entermediadb.websocket.chat.ChatServer;
import org.json.simple.JSONObject;
import org.openedit.Data;
import org.openedit.MultiValued;
import org.openedit.OpenEditException;
import org.openedit.WebPageRequest;
import org.openedit.util.HttpSharedConnection;

public class WhatsAppManager extends BaseAiManager implements ChatBroadcastListener
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
			String senderPhone = message.getSenderPhone();

			Data senderProfile = getMediaArchive().query("emeprofile").exact("phonenumber", senderPhone).searchOne();

			if (senderProfile == null)
			{
				senderProfile = getMediaArchive().getSearcher("emeprofile").createNewData();
				senderProfile.setProperty("phonenumber", senderPhone);

				Data user = getMediaArchive().getSearcher("user").createNewData();
				getMediaArchive().getSearcher("user").saveData(user);

				senderProfile.setProperty("owner", user.getId());
				getMediaArchive().getSearcher("emeprofile").saveData(senderProfile);
			}

			Data businessProfile = getMediaArchive().query("emeprofile").exact("phonenumber", message.getDisplayPhoneNumber()).searchOne();

			if (businessProfile == null)
			{
				log.warn("Received message for unknown business phone number: " + message.getDisplayPhoneNumber());
				inReq.getResponse().setStatus(404);
				return;
			}

			Collection<String> profileIds = Arrays.asList(senderProfile.getId(), businessProfile.getId());

			Data channel = getMediaArchive().query("channel").andgroup("dataid", profileIds).searchOne();

			if (channel == null)
			{
				channel = getMediaArchive().getSearcher("channel").createNewData();
				channel.setValue("dataid", profileIds);
				channel.setValue("searchtype", "emeprofile");
				channel.setValue("refreshdate", new Date());

				getMediaArchive().getSearcher("channel").saveData(channel);
			}

			MultiValued whatsappMessage = (MultiValued) getMediaArchive().getSearcher("chatterbox").createNewData();
			whatsappMessage.setValue("user", senderProfile.get("owner"));
			whatsappMessage.setValue("channel", channel.getId());
			whatsappMessage.setValue("date", new Date());
			whatsappMessage.setValue("message", message.getMessageText());
			getMediaArchive().getSearcher("chatterbox").saveData(whatsappMessage);

			ChatServer server = (ChatServer) getMediaArchive().getBean("chatServer");
			server.addChatBroadcastListener(this);
			server.broadcastMessage(getMediaArchive().getCatalogId(), whatsappMessage);

			// getMediaArchive().fireSharedMediaEvent("llm/monitorchats");

			// log.info("Intercepted message from " + message.getSenderPhone() + ": " +
			// message.getMessageText());

			// String messageType = message.getMessageType();
			// if ("interactive".equals(messageType))
			// {
			// log.info("Received interactive message, ignoring for now.");
			// inReq.getResponse().setStatus(200);
			// return;
			// }

			// LlmConnection llmconnection = getMediaArchive().getLlmConnection("parse_ride_request");
			// AgentContext context = new BaseAgentContext();
			// context.put("userquery", message.getMessageText());

			// LlmResponse response = llmconnection.callToolsFunction(context, "parse_ride_message");

			// String functionName = response.getFunctionName();
			// JSONObject functionArgs = response.getFunctionArguments();

			// JSONObject replyBody = null;

			// if (!"request_clarification".equals(functionName))
			// {
			// if ("book_ride".equals(functionName))
			// {
			// replyBody = WhatsAppResponse.buildTemplateReply(message, "ride_request_preview", functionArgs);
			// }
			// else if ("check_status".equals(functionName))
			// {
			// // TODO: implement check status
			// }
			// }
			// else
			// {
			// String reply = (String) functionArgs.get("clarification_question");
			// if (reply != null)
			// {
			// replyBody = WhatsAppResponse.buildTextReply(message, reply);
			// }
			// }

			// if (replyBody == null)
			// {
			// log.warn("No reply generated for function: " + functionName);
			// inReq.getResponse().setStatus(200);
			// return;
			// }

			// log.info("Generated reply for function " + functionName + ": " + replyBody.toJSONString());

			// String url = "https://graph.facebook.com/v25.0/" + PHONE_NUMBER_ID + "/messages";
			// HttpSharedConnection connection = new HttpSharedConnection();
			// connection.addSharedHeader("Authorization", "Bearer " + ACCESS_TOKEN);
			// CloseableHttpResponse resp = null;

			// try
			// {
			// resp = connection.sharedPostWithJson(url, replyBody);
			// log.info("Reply sent successfully.");
			// }
			// catch (Exception e)
			// {
			// log.error("Failed to send reply", e);
			// }
			// finally
			// {
			// connection.release(resp);
			// }
		}

		// Meta requires a 200 OK response within seconds, or they will retry sending the payload.
		inReq.getResponse().setStatus(200);
	}

	@Override
	public void onBroadcast(Data channel, Data message)
	{
		Collection<String> dataIds = channel.getValues("dataid");

		for (String dataId : dataIds)
		{
			String searchType = channel.get("searchtype");

			MultiValued profile = (MultiValued) getMediaArchive().query(searchType).exact("id", dataId).searchOne();
			if (profile != null)
			{
				String phoneNumber = profile.get("phonenumber");

				if (phoneNumber != null)
				{
					log.info("Broadcasting message to channel " + channel.getId() + " with phone number " + phoneNumber);

					boolean whatsappEnabled = profile.getBoolean("whatsappenabled");
					if (whatsappEnabled)
					{
						sendMessage(phoneNumber, message.get("message"));
					}
				}
			}
		}

	}

	public void sendMessage(String toPhoneNumber, String messageText)
	{
		JSONObject replyBody = WhatsAppResponse.buildTextReply(toPhoneNumber, messageText);

		String url = "https://graph.facebook.com/v25.0/" + PHONE_NUMBER_ID + "/messages";
		HttpSharedConnection httpConnection = new HttpSharedConnection();
		httpConnection.addSharedHeader("Authorization", "Bearer " + ACCESS_TOKEN);
		CloseableHttpResponse resp = null;

		try
		{
			resp = httpConnection.sharedPostWithJson(url, replyBody);
			log.info("Reply sent successfully.");
		}
		catch (Exception e)
		{
			log.error("Failed to send reply", e);
		}
		finally
		{
			httpConnection.release(resp);
		}
	}
}
