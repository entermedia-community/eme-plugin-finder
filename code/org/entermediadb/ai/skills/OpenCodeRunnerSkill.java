package org.entermediadb.ai.skills;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.ai.AgentContext;
import org.entermediadb.ai.BaseSkill;
import org.entermediadb.ai.llm.BasicLlmResponse;
import org.entermediadb.ai.llm.LlmResponse;
import org.openedit.util.Exec;
import org.openedit.util.FinalizedProcess;
import org.openedit.util.FinalizedProcessBuilder;

/**
 * OpenCodeRunnerSkill - Runs the opencode CLI in a working directory passed in the agent context
 * and appends its output to a temporary file, then returns the file contents as the LLM response
 * message.
 *
 * Usage in AgentContext: - Set "workingpath" (optional) - Directory the opencode command runs in;
 * defaults to one level above getMediaArchive().getRootDirectory() - Set "outputfile" (optional) -
 * Path of the temp file to append output to; if not provided, defaults to a unique log file under
 * workingpath/tomcat/logs/
 *
 * Results stored in context: - "commandoutput" - Full contents of the output file after running -
 * "outputfilepath" - Path of the temp file that holds the appended output
 */
public class OpenCodeRunnerSkill extends BaseSkill
{
	private static final Log log = LogFactory.getLog(OpenCodeRunnerSkill.class);

	protected static final String COMMAND = "opencode";

	protected Exec fieldExec;

	@Override
	public void process(AgentContext inContext)
	{
		String workingpath = (String) inContext.getContextValue("workingpath");
		if (workingpath == null || workingpath.trim().isEmpty())
		{
			workingpath = getMediaArchive().getRootDirectory().getParentFile().getAbsolutePath();
			log.info("OpenCodeRunnerSkill: No workingpath provided, defaulting to " + workingpath);
		}

		String outputfilepath = (String) inContext.getContextValue("outputfile");
		File outputfile = null;
		try
		{
			if (outputfilepath != null && !outputfilepath.trim().isEmpty())
			{
				outputfile = new File(outputfilepath);
			}
			else
			{
				outputfile = new File(new File(workingpath, "tomcat/logs"), "opencoderunner_" + System.currentTimeMillis() + ".log");
			}

			File parent = outputfile.getParentFile();
			if (parent != null && !parent.exists())
			{
				parent.mkdirs();
			}

			log.info("OpenCodeRunnerSkill running command: " + COMMAND + " in " + workingpath);

			List<String> args = new ArrayList<String>();
			args.add(COMMAND);

			FinalizedProcessBuilder builder = new FinalizedProcessBuilder(args);
			builder.keepProcess(false);
			builder.logInputtStream(true);
			builder.directory(new File(workingpath));

			FinalizedProcess process = builder.start(getExec().getExecutorManager());
			try
			{
				int exitcode = process.waitFor(getExec().getTimeLimit());
				String stdout = process.getStandardOutputs();
				String stderr = process.getErrorOutputs();

				appendToFile(outputfile, stdout);
				if (stderr != null && !stderr.trim().isEmpty())
				{
					appendToFile(outputfile, stderr);
				}

				log.info("OpenCodeRunnerSkill command exited with code: " + exitcode);
				if (exitcode != 0)
				{
					inContext.put("errormessage", "Command exited with code " + exitcode);
				}
			}
			finally
			{
				process.close();
			}
		}
		catch (IOException e)
		{
			log.error("OpenCodeRunnerSkill failed running command: " + COMMAND + " in " + workingpath, e);
			inContext.put("errormessage", "Failed running command: " + e.getMessage());
		}
		catch (InterruptedException e)
		{
			log.error("OpenCodeRunnerSkill interrupted running command: " + COMMAND + " in " + workingpath, e);
			Thread.currentThread().interrupt();
			inContext.put("errormessage", "Command execution was interrupted");
		}

		if (outputfile != null && outputfile.exists())
		{
			String filecontents = readFileContents(outputfile);
			inContext.put("commandoutput", filecontents);
			inContext.put("outputfilepath", outputfile.getAbsolutePath());

			LlmResponse response = new BasicLlmResponse();
			response.setMessage(filecontents);
			inContext.setLastResponse(response);
		}

		super.process(inContext);
	}

	protected void appendToFile(File inFile, String inContent) throws IOException
	{
		if (inContent == null || inContent.isEmpty())
		{
			return;
		}
		FileOutputStream out = new FileOutputStream(inFile, true);
		try
		{
			out.write(inContent.getBytes("UTF-8"));
		}
		finally
		{
			out.close();
		}
	}

	protected String readFileContents(File inFile)
	{
		FileInputStream in = null;
		try
		{
			in = new FileInputStream(inFile);
			String contents = getExec().getFiller().readAllText(in);
			if (contents == null)
			{
				return "";
			}
			return contents;
		}
		catch (IOException e)
		{
			log.error("OpenCodeRunnerSkill failed reading output file: " + inFile.getAbsolutePath(), e);
			return "";
		}
		finally
		{
			getExec().getFiller().close(in);
		}
	}

	public Exec getExec()
	{
		return fieldExec;
	}

	public void setExec(Exec inExec)
	{
		fieldExec = inExec;
	}

}
