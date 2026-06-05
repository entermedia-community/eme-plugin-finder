package org.entermediadb.whatsapp;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.modules.BaseMediaModule;
import org.openedit.OpenEditException;
import org.openedit.WebPageRequest;

public class WhatsAppModule extends BaseMediaModule
{
	private static final Log log = LogFactory.getLog(WhatsAppModule.class);

	public WhatsAppManager getWhatsAppManager()
	{
		WhatsAppManager manager = new WhatsAppManager();
		return manager;
	}

	public void handleWebhook(WebPageRequest inReq)
	{
		String method = inReq.getRequest().getMethod();

		if ("GET".equalsIgnoreCase(method))
		{
			getWhatsAppManager().verifyWebhook(inReq);
		}
		else
		{
			if ("POST".equalsIgnoreCase(method))
			{
				getWhatsAppManager().receiveMessage(inReq);
			}
			else
			{
				log.warn("Unsupported HTTP method: " + method);
				inReq.getResponse().setStatus(405); // Method Not Allowed
			}
		}
	}
}
