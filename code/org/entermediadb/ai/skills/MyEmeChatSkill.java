package org.entermediadb.ai.skills;

import java.io.File;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.ai.AgentContext;
import org.entermediadb.ai.BaseSkill;
import org.entermediadb.ai.ChatMessageContext;
import org.entermediadb.ai.agentjobs.AgentJobManager;
import org.entermediadb.ai.llm.BasicLlmResponse;
import org.entermediadb.mcp.client.OpenCodeClient;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.openedit.Data;
import org.openedit.MultiValued;

/**
 * Sends every message in a user's My EME chat to that chat's OpenCode session and shows OpenCode's work
 * as one chat message that builds up while it happens.
 *
 * Each chat channel has its own OpenCode session, found by the channel's opencodesessionid and created
 * on the first message in the user's workspace folder (see MyEmeWorkspace). Starting a new chat session
 * in the app makes a new channel, so it gets a new OpenCode session.
 *
 * The reply is a single chatterbox message. Its text, the tools OpenCode uses and any error are collected
 * in a MyEmeTurnMessage, and every change goes to the clients as a messageappend or messagereplace patch
 * of that same message. The skill sleeps on the session's event queue, so it wakes only when OpenCode
 * says something (there is no polling), gathers events that arrive together, and sends one patch. When the
 * turn ends the finished message is saved and sent whole through the normal status update. Errors are
 * shown in the message, not hidden. The response names this step as the next one, so the following
 * message in the chat comes back here.
 */
public class MyEmeChatSkill extends BaseSkill
{
	private static final Log log = LogFactory.getLog(MyEmeChatSkill.class);

	protected static final long TURN_TIMEOUT_MS = TimeUnit.MINUTES.toMillis(20);
	/** After an event, how long to gather others that arrive with it before sending a patch */
	protected static final long BATCH_MS = 200;
	/** How often the message is saved while it builds up, so a reload shows roughly what was on screen */
	protected static final long SAVE_EVERY_MS = 3000;
	/** The longest quiet wait before looking at the clock again; nothing else wakes the thread while OpenCode is silent */
	protected static final long QUIET_WAIT_MS = TimeUnit.MINUTES.toMillis(1);

	@Override
	public void process(AgentContext inContext)
	{
		ChatMessageContext messageContext = (ChatMessageContext) inContext;
		MultiValued usermessage = messageContext.getUserMessage();
		String prompt = usermessage == null ? null : usermessage.get("message");
		if (prompt == null || prompt.trim().isEmpty())
		{
			// Arriving from the menu with nothing to say yet
			reply(inContext, textOps("What would you like me to do?"), "What would you like me to do?", "done");
			return;
		}

		MultiValued agentmessage = messageContext.getAgentMessage();
		MyEmeTurnMessage turn = new MyEmeTurnMessage();
		try
		{
			streamTurn(messageContext, agentmessage, prompt.trim(), turn);
		}
		catch (InterruptedException e)
		{
			Thread.currentThread().interrupt();
			log.error("My EME chat was interrupted while waiting for OpenCode", e);
			turn.fail("Stopped waiting for OpenCode.");
		}
		catch (Exception e)
		{
			log.error("My EME chat could not run the prompt in OpenCode", e);
			turn.fail("OpenCode could not run this: " + e.getMessage());
		}

		JSONArray ops = turn.ops();
		if (ops.isEmpty())
		{
			ops = textOps("OpenCode finished without saying anything.");
		}
		reply(inContext, ops, turn.plainText(), turn.getStatus());
	}

	/** Runs the prompt in the channel's session, sending the message's changes as they happen, until the turn ends */
	protected void streamTurn(ChatMessageContext inContext, MultiValued inAgentMessage, String inPrompt, MyEmeTurnMessage inTurn) throws InterruptedException
	{
		AgentJobManager manager = (AgentJobManager) getMediaArchive().getBean("agentJobManager");
		OpenCodeClient client = manager.getOpenCodeClient();
		client.connectToServer(); // listen for events before the prompt goes out

		String sessionid = findOrCreateSession(client, inContext, inPrompt);

		BlockingQueue<JSONObject> queue = client.openEventQueue(sessionid);
		try
		{
			String rejected = client.sendPrompt(sessionid, inPrompt);
			if (rejected != null)
			{
				inTurn.fail(rejected);
				return;
			}

			long deadline = System.currentTimeMillis() + TURN_TIMEOUT_MS;
			long lastSave = System.currentTimeMillis();
			while (!inTurn.isFinished())
			{
				long remaining = deadline - System.currentTimeMillis();
				if (remaining <= 0)
				{
					client.abortSession(sessionid);
					inTurn.fail("OpenCode did not finish within " + TimeUnit.MILLISECONDS.toMinutes(TURN_TIMEOUT_MS) + " minutes.");
				}
				else
				{
					JSONObject event = queue.poll(Math.min(remaining, QUIET_WAIT_MS), TimeUnit.MILLISECONDS);
					if (event == null)
					{
						continue; // quiet: look at the deadline and wait again
					}
					handle(client, sessionid, inTurn, event);

					// Others usually arrive in the same moment (text comes in bursts), so send them together
					long batchEnd = System.currentTimeMillis() + BATCH_MS;
					while (!inTurn.isFinished())
					{
						long wait = batchEnd - System.currentTimeMillis();
						JSONObject more = wait > 0 ? queue.poll(wait, TimeUnit.MILLISECONDS) : null;
						if (more == null)
						{
							break;
						}
						handle(client, sessionid, inTurn, more);
					}
				}

				JSONObject patch = inTurn.nextPatch();
				if (patch != null)
				{
					manager.broadcastMessagePatch(inAgentMessage, (String) patch.get("command"), (JSONArray) patch.get("ops"), ((Integer) patch.get("seq")).intValue(),
							(String) patch.get("status"));
				}
				if (!inTurn.isFinished() && System.currentTimeMillis() - lastSave >= SAVE_EVERY_MS)
				{
					saveProgress(inAgentMessage, inTurn);
					lastSave = System.currentTimeMillis();
				}
			}
		}
		finally
		{
			client.closeEventQueue(sessionid, queue);
		}
	}

	/** Feeds one event to the message; a permission request is refused, since nobody is there to approve it */
	protected void handle(OpenCodeClient inClient, String inSessionId, MyEmeTurnMessage inTurn, JSONObject inEvent)
	{
		String type = (String) inEvent.get("type");
		if (OpenCodeClient.SUPERSEDED.equals(type))
		{
			inTurn.handOff();
		}
		else if ("permission.asked".equals(type))
		{
			JSONObject data = (JSONObject) inEvent.get("data");
			if (data != null && data.get("id") != null)
			{
				inClient.replyToPermission(inSessionId, String.valueOf(data.get("id")), "reject");
				inTurn.note("Not allowed here: " + data.get("action"));
			}
		}
		else
		{
			inTurn.apply(inEvent);
		}
	}

	/** Saves what the message looks like so far, so a client that loads the chat mid-turn sees it */
	protected void saveProgress(MultiValued inAgentMessage, MyEmeTurnMessage inTurn)
	{
		JSONObject message = new JSONObject();
		message.put("ops", inTurn.ops());
		inAgentMessage.setValue("message", message.toJSONString());
		inAgentMessage.setValue("messageplain", inTurn.plainText());
		getMediaArchive().saveData("chatterbox", inAgentMessage);
	}

	/** The channel's OpenCode session, started in the user's workspace if it has none yet (or OpenCode has lost it) */
	protected String findOrCreateSession(OpenCodeClient inClient, ChatMessageContext inContext, String inPrompt)
	{
		Data channel = inContext.getChannel();
		String userid = channel.get("user");
		File folder = MyEmeWorkspace.folderFor(getMediaArchive(), userid);

		// A session is only reused when it works in the user's session folder. One saved from before the folders
		// moved (or started anywhere else) could still reach what it should not, so it is replaced.
		String sessionid = channel.get("opencodesessionid");
		String directory = inClient.sessionDirectory(sessionid);
		if (directory != null && new File(directory).equals(folder))
		{
			return sessionid;
		}
		if (directory != null)
		{
			log.info("OpenCode session " + sessionid + " of channel " + channel.getId() + " works in " + directory + ", not in " + folder + "; starting a new one");
		}

		String title = inPrompt.length() > 200 ? inPrompt.substring(0, 200) : inPrompt;
		JSONObject session = inClient.createSession(title, folder.getAbsolutePath(), MyEmeWorkspace.permissions(getMediaArchive(), userid));
		sessionid = (String) session.get("id");
		log.info("Started OpenCode session " + sessionid + " for channel " + channel.getId() + " in " + folder);

		channel.setValue("opencodesessionid", sessionid);
		getMediaArchive().saveData("channel", channel);
		return sessionid;
	}

	@SuppressWarnings("unchecked")
	protected JSONArray textOps(String inText)
	{
		JSONObject insert = new JSONObject();
		insert.put("insert", inText.endsWith("\n") ? inText : inText + "\n");
		JSONArray ops = new JSONArray();
		ops.add(insert);
		return ops;
	}

	/**
	 * Saves and sends the finished message whole (clients that missed a patch catch up here) and keeps this
	 * step as the one that handles the next message. inStatus goes out as messagestatus.
	 */
	@SuppressWarnings("unchecked")
	protected void reply(AgentContext inContext, JSONArray inOps, String inPlainText, String inStatus)
	{
		JSONObject message = new JSONObject();
		message.put("ops", inOps);

		BasicLlmResponse response = new BasicLlmResponse();
		response.setMessage(message.toJSONString());
		response.setMessagePlain(inPlainText);
		response.setNextAutomationStep(inContext.getCurrentAgentJobStep().getEnabledId());
		inContext.setLastResponse(response);
		inContext.put("messagestatus", inStatus);
		processCompleted(inContext);
	}
}
