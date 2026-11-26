package org.entermediadb.ai.agentjobs;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import junit.framework.TestCase;
import org.entermediadb.ai.llm.BaseAgentContext;
import org.entermediadb.mcp.client.OpenCodeClient;
import org.entermediadb.mcp.client.SessionStatus;
import org.entermediadb.scripts.ScriptLogger;
import org.json.simple.JSONObject;
import org.openedit.MultiValued;
import org.openedit.data.BaseData;

public class OpenCodeOrchestratorChecks extends TestCase
{
	private static class FakeClient extends OpenCodeClient
	{
		final SessionStatus session = new SessionStatus();
		final CountDownLatch advanceCalled = new CountDownLatch(1);
		final CountDownLatch terminal = new CountDownLatch(1);

		FakeClient()
		{
			session.setSessionId("session");
		}

		@Override
		public SessionStatus loadStatus(String inStepId)
		{
			return session;
		}

		@Override
		public SessionStatus advanceSession(ScriptLogger inLog, String inStepId, long inTimeoutMs) throws InterruptedException
		{
			advanceCalled.countDown();
			terminal.await();
			return session;
		}

		@Override
		public List<JSONObject> getMessages(String inSessionId)
		{
			return Collections.emptyList();
		}
	}

	private static class TestOrchestrator extends OpenCodeOrchestrator
	{
		@Override
		protected void completeIfRunning(MultiValued inStep)
		{
			if ("running".equals(inStep.get("status")))
			{
				inStep.setValue("status", "complete");
			}
		}
	}

	private static class EventClient extends OpenCodeClient
	{
		void apply(SessionStatus inStatus, String inType, String inSessionId)
		{
			JSONObject data = new JSONObject();
			data.put("sessionID", inSessionId);
			JSONObject event = new JSONObject();
			event.put("type", inType);
			event.put("data", data);
			applySessionEvent(null, inStatus, event);
		}

		void requestPermission(SessionStatus inStatus)
		{
			JSONObject data = new JSONObject();
			data.put("sessionID", inStatus.getSessionId());
			data.put("id", "permission");
			JSONObject event = new JSONObject();
			event.put("type", "permission.asked");
			event.put("data", data);
			applySessionEvent(null, inStatus, event);
		}
	}

	public void testWaitsForSuccessBeforeCompletingStep() throws Exception
	{
		FakeClient client = new FakeClient();
		AgentJobRunnable job = new AgentJobRunnable();
		job.setContext(new BaseAgentContext());
		BaseData step = new BaseData();
		step.setId("step");
		step.setValue("status", "running");
		AtomicReference<Throwable> error = new AtomicReference<>();
		Thread thread = new Thread(() -> {
			try
			{
				new TestOrchestrator().waitForSession(job, step, client);
			}
			catch (Throwable ex)
			{
				error.set(ex);
			}
		});
		thread.start();
		assertTrue(client.advanceCalled.await(5, TimeUnit.SECONDS));
		assertTrue(thread.isAlive());
		assertEquals("running", step.get("status"));
		client.session.setCompleted(true);
		client.terminal.countDown();
		thread.join(5000);
		assertFalse(thread.isAlive());
		assertNull(error.get());
		assertEquals("complete", step.get("status"));
	}

	public void testFailureIsNotMarkedComplete()
	{
		FakeClient client = new FakeClient();
		client.session.setError("execution failed");
		client.session.setCompleted(true);
		client.terminal.countDown();
		AgentJobRunnable job = new AgentJobRunnable();
		job.setContext(new BaseAgentContext());
		BaseData step = new BaseData();
		step.setId("step");
		step.setValue("status", "running");
		try
		{
			new TestOrchestrator().waitForSession(job, step, client);
			fail("Expected execution failure");
		}
		catch (RuntimeException ex)
		{
			assertTrue(ex.getMessage().contains("execution failed"));
			assertEquals("running", step.get("status"));
		}
	}

	public void testOnlyTerminalOrUnstartedStatusesCanRepeat()
	{
		assertTrue(AgentJobManager.isRepeatableStatus(null));
		assertTrue(AgentJobManager.isRepeatableStatus("complete"));
		assertTrue(AgentJobManager.isRepeatableStatus("error"));
		assertFalse(AgentJobManager.isRepeatableStatus("running"));
		assertFalse(AgentJobManager.isRepeatableStatus("new"));
		assertFalse(AgentJobManager.isRepeatableStatus("question"));
		assertFalse(AgentJobManager.isRepeatableStatus("securityprompt"));
	}

	public void testCompletionEventsAreScopedToSessionAndAllowNullLogger()
	{
		EventClient client = new EventClient();
		SessionStatus session = new SessionStatus();
		session.setSessionId("session");
		client.apply(session, "session.step.started", "session");
		client.apply(session, "session.execution.succeeded", "other-session");
		assertFalse(session.isCompleted());
		client.apply(session, "session.execution.succeeded", "session");
		assertTrue(session.isCompleted());
	}

	public void testPendingPermissionDoesNotCompleteSession()
	{
		EventClient client = new EventClient();
		SessionStatus session = new SessionStatus();
		session.setSessionId("session");
		client.requestPermission(session);
		client.apply(session, "session.execution.succeeded", "session");
		assertFalse(session.isCompleted());
		assertEquals("permission", session.getConfirmationPendingId());
	}
}
