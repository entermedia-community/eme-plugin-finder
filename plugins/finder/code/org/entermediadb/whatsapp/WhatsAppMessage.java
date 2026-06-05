package org.entermediadb.whatsapp;

import java.util.List;
import java.util.Map;

public final class WhatsAppMessage
{
	private String fieldSenderPhone;
	private String fieldMessageId;
	private String fieldMessageText;
	private String fieldMessageType;

	public String getSenderPhone()
	{
		return fieldSenderPhone;
	}

	public void setSenderPhone(String senderPhone)
	{
		fieldSenderPhone = senderPhone;
	}

	public String getMessageId()
	{
		return fieldMessageId;
	}

	public void setMessageId(String messageId)
	{
		fieldMessageId = messageId;
	}

	public String getMessageText()
	{
		return fieldMessageText;
	}

	public void setMessageText(String messageText)
	{
		fieldMessageText = messageText;
	}

	public String getMessageType()
	{
		return fieldMessageType;
	}

	public void setMessageType(String messageType)
	{
		fieldMessageType = messageType;
	}

	public WhatsAppMessage(Map body) {
		if (body == null || body.get("object") == null)
		{
			return;
		}

		List entry = (List) body.get("entry");
		if (entry == null || entry.isEmpty())
		{
			return;
		}

		Map entry0 = (Map) entry.get(0);
		List changes = (List) entry0.get("changes");
		if (changes == null || changes.isEmpty())
		{
			return;
		}

		Map changes0 = (Map) changes.get(0);
		Map value = (Map) changes0.get("value");
		if (value == null)
		{
			return;
		}

		List messages = (List) value.get("messages");
		if (messages == null || messages.isEmpty())
		{
			return;
		}

		Map messageData = (Map) messages.get(0);

		String senderPhone = (String) messageData.get("from");
		setSenderPhone(senderPhone);

		String messageId = (String) messageData.get("id");
		setMessageId(messageId);

		String messageType = (String) messageData.get("type");
		setMessageType(messageType);

		if ("text".equals(messageType))
		{
			Map textObj = (Map) messageData.get("text");
			String messageText = textObj != null ? (String) textObj.get("body") : null;
			setMessageText(messageText);
		}
		else if ("interactive".equals(messageType))
		{
			Map interactiveObj = (Map) messageData.get("interactive");
			if (interactiveObj != null)
			{
				String interactiveType = (String) interactiveObj.get("type");
				if ("button_reply".equals(interactiveType))
				{
					Map buttonReply = (Map) interactiveObj.get("button_reply");
					String messageText = buttonReply != null ? (String) buttonReply.get("title") : null;
					setMessageText(messageText);
				}
			}
			else
			{
				setMessageText(null);
			}
		}
		else
		{
			setMessageText(null);
		}
	}
}
