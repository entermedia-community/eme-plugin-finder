package org.entermediadb.ai.classify.agents;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.entermediadb.ai.AgentContext;
import org.entermediadb.ai.BaseSkill;
import org.json.simple.JSONObject;
import org.openedit.util.HttpSharedConnection;

public class WhatsAppSkill extends BaseSkill
{

	HttpSharedConnection fieldSharedConnection;

	public HttpSharedConnection getSharedConnection()
	{
		if (fieldSharedConnection == null)
		{
			fieldSharedConnection = new HttpSharedConnection();
			getSharedConnection().addSharedHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
		}
		return fieldSharedConnection;
	}

	// Everyone gets an emeprofile

	// Table of taxi drivers
	// Table or customers
	//

	// https://business.facebook.com/latest/whatsapp_manager/template_library?business_id=1467948425034321&asset_id=1395992459249751
	// https://business.facebook.com/latest/whatsapp_manager/phone_numbers?business_id=1467948425034321&tab=phone-numbers&childRoute=PHONE_PROFILE%2FINSIGHTS&phone_number=1%20555-668-2460&nav_ref=whatsapp_manager&asset_id=1395992459249751

	@Override
	public void process(AgentContext inContext)
	{

		JSONObject payload = new JSONObject();
		payload.put("messaging_product", "whatsapp");
		payload.put("to", "14155238886");
		payload.put("type", "template");
		JSONObject template = new JSONObject();
		template.put("name", "hello_world");
		JSONObject language = new JSONObject();
		language.put("code", "en_US");
		template.put("language", language);
		payload.put("template", template);

		String url = "https://graph.facebook.com/v15.0/105417441489348/messages";
		getSharedConnection().addSharedHeader("Authorization", "Bearer EAAJZCZA9ZB8ZCUBAGZCzZCjZC1ZB7ZA4ZC2ZA9ZA5ZAiZA3nqLhZBzZB8mXoGgkHqvKZC1ZA9ZA5ZAiZA3nqLhZBzZB8mXoGgkHqvKZD");
		CloseableHttpResponse response = getSharedConnection().sharedPostWithJson(url, payload);
		if (response.getStatusLine().getStatusCode() != 200)
		{
			getSharedConnection().release(response);
			throw new RuntimeException("Failed to send message: " + response.getStatusLine().toString());
		}
		super.process(inContext);
	}

}
