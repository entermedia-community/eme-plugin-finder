package org.entermediadb.whatsapp;

import java.util.Arrays;
import java.util.Map;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class WhatsAppResponse
{

	public static JSONObject buildTextReply(WhatsAppMessage inMessage, String inReplyText)
	{
		return buildTextReply(inMessage.getSenderPhone(), inReplyText);
	}

	public static JSONObject buildTextReply(String phoneNumber, String messageReply)
	{
		JSONObject context = new JSONObject();
		// context.put("message_id", inMessage.getMessageId());

		JSONObject text = new JSONObject();
		text.put("body", messageReply);

		JSONObject body = new JSONObject();
		body.put("messaging_product", "whatsapp");
		body.put("recipient_type", "individual");
		body.put("to", phoneNumber);
		body.put("context", context);
		body.put("type", "text");
		body.put("text", text);

		return body;
	}

	public static JSONObject buildTemplateReply(WhatsAppMessage inMessage, String inTemplateName, JSONObject inTemplateParameters)
	{
		JSONObject context = new JSONObject();
		context.put("message_id", inMessage.getMessageId());

		JSONObject language = new JSONObject();
		language.put("code", "en_US");

		JSONObject template = new JSONObject();
		template.put("name", inTemplateName);
		template.put("language", language);

		if (inTemplateParameters != null)
		{
			JSONObject components = new JSONObject();
			components.put("type", "body");
			JSONArray parameters = new JSONArray();
			for (Map.Entry<String, Object> entry : inTemplateParameters.entrySet())
			{
				JSONObject param = new JSONObject();
				param.put("type", "text");
				param.put("text", entry.getValue().toString());
				param.put("parameter_name", entry.getKey());
				parameters.add(param);
			}
			components.put("parameters", parameters);
			template.put("components", Arrays.asList(components));
		}

		JSONObject body = new JSONObject();
		body.put("messaging_product", "whatsapp");
		body.put("recipient_type", "individual");
		body.put("to", inMessage.getSenderPhone());

		body.put("type", "template");
		body.put("template", template);

		return body;
	}
}
