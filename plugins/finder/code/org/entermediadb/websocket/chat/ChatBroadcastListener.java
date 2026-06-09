package org.entermediadb.websocket.chat;

import org.openedit.Data;

public interface ChatBroadcastListener
{
	public void onBroadcast(Data message, Data channel);
}
