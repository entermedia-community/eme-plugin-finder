package org.entermediadb.whatsapp;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.modules.BaseMediaModule;
import org.openedit.WebPageRequest;

public class WhatsAppModule extends BaseMediaModule
{
	private static final Log log = LogFactory.getLog(WhatsAppModule.class);

	public WhatsAppManager getWhatsAppManager(WebPageRequest inReq)
	{
		String catalogid = inReq.findValue("catalogid");
		WhatsAppManager whatsappManager = (WhatsAppManager) getMediaArchive(catalogid).getBean("whatsappManager");
		return whatsappManager;
	}

	public void handleWebhook(WebPageRequest inReq)
	{
		String method = inReq.getRequest().getMethod();

		WhatsAppManager whatsappManager = getWhatsAppManager(inReq);

		if ("GET".equalsIgnoreCase(method))
		{
			whatsappManager.verifyWebhook(inReq);
		}
		else
		{
			if ("POST".equalsIgnoreCase(method))
			{
				whatsappManager.receiveMessage(inReq);
			}
			else
			{
				log.warn("Unsupported HTTP method: " + method);
				inReq.getResponse().setStatus(405); // Method Not Allowed
			}
		}
	}
}
