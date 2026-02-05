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
import org.onehippo.forge.exportjson.repository.workflow.ExportHtmlWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExportHtmlDownloadPage extends Page {

    private static final Logger log = LoggerFactory.getLogger(ExportHtmlDownloadPage.class);

    public ExportHtmlDownloadPage(PageParameters parameters) {
        super(parameters);

        String nodeId = parameters.get("nodeId").toString();
        String nodeName = parameters.get("nodeName").toString();
        boolean includeMetadata = parameters.get("includeMetadata").toBoolean(true);
        boolean includeStyles = parameters.get("includeStyles").toBoolean(true);

        if (nodeId == null || nodeId.isEmpty() || nodeName == null || nodeName.isEmpty()) {
            return;
        }

        try {
            org.apache.wicket.Session wicketSession = org.apache.wicket.Session.get();
            javax.jcr.Session jcrSession = ((UserSession) wicketSession).getJcrSession();
            HippoWorkspace workspace = (HippoWorkspace) jcrSession.getWorkspace();
            WorkflowManager workflowManager = workspace.getWorkflowManager();

            Node node = jcrSession.getNodeByIdentifier(nodeId);
            ExportHtmlWorkflow htmlWorkflow = (ExportHtmlWorkflow) workflowManager.getWorkflow("exporthtml", node);

            if (htmlWorkflow == null) {
                throw new RuntimeException("HTML export workflow not found");
            }

            // Try to retrieve from session first
            String htmlContent = (String) wicketSession.getAttribute("exportHtml_" + nodeId);

            // Measure workflow execution time
            long startTime = System.currentTimeMillis();

            // If dialog options differ from defaults, re-export with new options
            if (htmlContent != null && (includeMetadata != true || includeStyles != true)) {
                htmlContent = htmlWorkflow.exportHtmlDocument(nodeId, includeMetadata, includeStyles);
            } else if (htmlContent == null) {
                // If not in session, export with current parameters
                htmlContent = htmlWorkflow.exportHtmlDocument(nodeId, includeMetadata, includeStyles);
            }

            long executionTime = System.currentTimeMillis() - startTime;
            log.info("HTML workflow execution took {} ms", executionTime);

            if (executionTime > 5000) {
                log.warn("HTML export took a long time: {} ms - consider optimization", executionTime);
            }

            String fileName = nodeName + ".html";
            byte[] htmlBytes = htmlContent.getBytes("UTF-8");

            // Validate size
            if (htmlBytes.length > 50_000_000) {
                log.warn("HTML file is large: {} bytes for nodeId: {}", htmlBytes.length, nodeId);
            }

            RequestCycle cycle = RequestCycle.get();
            HttpServletResponse httpResponse = (HttpServletResponse) cycle.getResponse().getContainerResponse();

            httpResponse.setContentType("text/html; charset=UTF-8");
            httpResponse.setHeader("Content-Disposition",
                    "attachment; filename=\"" + fileName + "\"");
            httpResponse.setContentLength(htmlBytes.length);

            try {
                httpResponse.getOutputStream().write(htmlBytes);
            } catch (IOException e) {
                // Handle client abort gracefully
                log.warn("Could not complete HTML response write: {}", e.getMessage());
            }

            log.info("Served HTML download for nodeId: {} (includeMetadata: {}, includeStyles: {})",
                    nodeId, includeMetadata, includeStyles);

            // Clean up session attribute
            wicketSession.removeAttribute("exportHtml_" + nodeId);

        } catch (RepositoryException e) {
            log.error("Repository error in HTML download: {}", e.getMessage(), e);
        } catch (IOException e) {
            // Handles broken pipe and other IO errors (including client aborts)
            if (e.getMessage() != null && e.getMessage().contains("Broken pipe")) {
                log.warn("Client disconnected during HTML download for nodeId: {}", nodeId);
            } else {
                log.error("Error writing response: {}", e.getMessage(), e);
            }
        } catch (Exception e) {
            log.error("Error in HTML download page: {}", e.getMessage(), e);
        }
    }

}
