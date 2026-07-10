package org.entermediadb.ai.llm;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openedit.ModuleManager;
import org.openedit.OpenEditException;
import org.openedit.WebPageRequest;
import org.openedit.page.Page;
import org.openedit.page.PageStreamer;
import org.openedit.page.manage.PageManager;
import org.openedit.servlet.OpenEditEngine;
import org.openedit.util.RequestUtils;

/**
 * Utility for rendering {@code .jte} templates to a {@link String} within the
 * OpenEdit request lifecycle — mirrors {@link VelocityRenderUtil} for use with
 * JTE-based templates.
 *
 * <p>
 * Typical usage from an action module:
 * 
 * <pre>
 * String rendered = jteRenderUtil.loadInputFromTemplate(inReq, "/ai/prompts/summarize.jte");
 * </pre>
 *
 * <p>
 * The path must point to a {@code .jte} file. If additional context values
 * are needed beyond what is already in the request's page-map, pass them via
 * the {@code Map} overload — they are merged into a copy of the page-map before
 * rendering.
 */
public class JteRenderUtil {
  private static final Log log = LogFactory.getLog(JteRenderUtil.class);

  protected ModuleManager fieldModuleManager;
  protected PageManager fieldPageManager;
  protected RequestUtils fieldRequestUtils;
  protected OpenEditEngine fieldEngine;

  public ModuleManager getModuleManager() {
    return fieldModuleManager;
  }

  public void setModuleManager(ModuleManager inModuleManager) {
    fieldModuleManager = inModuleManager;
  }

  public PageManager getPageManager() {
    return fieldPageManager;
  }

  public void setPageManager(PageManager inPageManager) {
    fieldPageManager = inPageManager;
  }

  public RequestUtils getRequestUtils() {
    return fieldRequestUtils;
  }

  public void setRequestUtils(RequestUtils inRequestUtils) {
    fieldRequestUtils = inRequestUtils;
  }

  public OpenEditEngine getEngine() {
    if (fieldEngine == null) {
      fieldEngine = (OpenEditEngine) getModuleManager().getBean("OpenEditEngine");
    }
    return fieldEngine;
  }

  /**
   * Renders the given {@code .jte} template in the context of {@code inReq}.
   */
  public String loadInputFromTemplate(WebPageRequest inReq, String inTemplate) {
    return loadInputFromTemplate(inReq, inTemplate, new HashMap<>());
  }

  /**
   * Renders the given {@code .jte} template in the context of {@code inReq},
   * merging {@code inMap} values into the context before rendering.
   */
  public String loadInputFromTemplate(WebPageRequest inReq, String inTemplate, Map<String, Object> inMap) {
    if (inTemplate == null) {
      throw new OpenEditException("Cannot load input, template is null" + inReq);
    }
    try {
      Page template = getPageManager().getPage(inTemplate);
      log.info("JTE loading input: " + inTemplate);

      WebPageRequest request = inReq.copy(template);

      // Merge any extra values provided by the caller
      if (inMap != null && !inMap.isEmpty()) {
        for (Map.Entry<String, Object> entry : inMap.entrySet()) {
          request.putPageValue(entry.getKey(), entry.getValue());
        }
      }

      StringWriter output = new StringWriter();
      request.setWriter(output);

      PageStreamer streamer = getEngine().createPageStreamer(template, request);
      getEngine().executePathActions(request);
      if (!request.hasRedirected()) {
        getModuleManager().executePageActions(template, request);
      }
      if (request.hasRedirected()) {
        log.info("JTE action was redirected");
      }

      streamer.include(template, request);
      return output.toString();
    } catch (OpenEditException e) {
      throw e;
    }
  }
}
