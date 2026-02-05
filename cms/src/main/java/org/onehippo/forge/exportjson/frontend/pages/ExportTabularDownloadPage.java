/*
 * Copyright 2024 Bloomreach B.V. (http://www.bloomreach.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.forge.exportjson.frontend.pages;

import java.io.IOException;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.wicket.Page;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.cycle.RequestCycle;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.WorkflowManager;
import org.onehippo.forge.exportjson.repository.ExportTabularConstants;
import org.onehippo.forge.exportjson.repository.workflow.ExportTabularWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExportTabularDownloadPage extends Page {

    private static final Logger log = LoggerFactory.getLogger(ExportTabularDownloadPage.class);

    public ExportTabularDownloadPage(PageParameters parameters) {
        super(parameters);

        String nodeId = parameters.get("nodeId").toString();
        String nodeName = parameters.get("nodeName").toString();
        String format = parameters.get("format").toOptionalString();
        boolean includeHeaders = parameters.get("includeHeaders").toBoolean(true);
        boolean quoteAll = parameters.get("quoteAll").toBoolean(false);

        if (nodeId == null || nodeId.isEmpty() || nodeName == null || nodeName.isEmpty() || format == null || format.isEmpty()) {
            return;
        }

        try {
            // Try to get content from session first (pre-computed during workflow)
            org.apache.wicket.Session wicketSession = org.apache.wicket.Session.get();
            String sessionKey = "export" + (format.equals("csv") ? "Csv" : "Tsv") + "_" + nodeId;
            String tabularContent = (String) wicketSession.getAttribute(sessionKey);

            if (tabularContent == null) {
                // Try to execute the export if not in session
                try {
                    javax.jcr.Session jcrSession = ((UserSession) wicketSession).getJcrSession();
                    HippoWorkspace workspace = (HippoWorkspace) jcrSession.getWorkspace();
                    WorkflowManager workflowManager = workspace.getWorkflowManager();

                    Node node = jcrSession.getNodeByIdentifier(nodeId);
                    ExportTabularWorkflow workflow = (ExportTabularWorkflow) workflowManager.getWorkflow(
                        format.equals("csv") ? "exportcsv" : "exporttsv", node);

                    tabularContent = workflow.exportTabularDocument(nodeId, format, includeHeaders, quoteAll);
                } catch (Exception e) {
                    log.error("Failed to export on fallback: {}", e.getMessage());
                    return;
                }
            } else {
                // Clean up session
                wicketSession.removeAttribute(sessionKey);
                // Re-export with dialog parameters to respect user choices
                try {
                    javax.jcr.Session jcrSession = ((UserSession) wicketSession).getJcrSession();
                    HippoWorkspace workspace = (HippoWorkspace) jcrSession.getWorkspace();
                    WorkflowManager workflowManager = workspace.getWorkflowManager();

                    Node node = jcrSession.getNodeByIdentifier(nodeId);
                    ExportTabularWorkflow workflow = (ExportTabularWorkflow) workflowManager.getWorkflow(
                        format.equals("csv") ? "exportcsv" : "exporttsv", node);

                    tabularContent = workflow.exportTabularDocument(nodeId, format, includeHeaders, quoteAll);
                } catch (Exception e) {
                    log.warn("Failed to re-export with dialog parameters, using cached: {}", e.getMessage());
                }
            }

            String mimeType = format.equals("csv") ? "text/csv" : "text/tab-separated-values";
            String fileExtension = format.equals("csv") ? ".csv" : ".tsv";
            String fileName = nodeName + fileExtension;
            byte[] tabularBytes = tabularContent.getBytes("UTF-8");

            RequestCycle cycle = RequestCycle.get();
            HttpServletResponse httpResponse = (HttpServletResponse) cycle.getResponse().getContainerResponse();

            httpResponse.setContentType(mimeType);
            httpResponse.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
            httpResponse.setContentLength(tabularBytes.length);
            httpResponse.getOutputStream().write(tabularBytes);
            httpResponse.getOutputStream().flush();

            log.info("Served {} download for nodeId: {} (includeHeaders: {}, quoteAll: {})",
                format.toUpperCase(), nodeId, includeHeaders, quoteAll);

        } catch (IOException e) {
            log.error("Error writing response: {}", e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error in download page: {}", e.getMessage(), e);
        }
    }

}
