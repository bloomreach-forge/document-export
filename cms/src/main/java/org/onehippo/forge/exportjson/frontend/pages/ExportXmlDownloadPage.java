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
import org.onehippo.forge.exportjson.repository.workflow.ExportXmlWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExportXmlDownloadPage extends Page {

    private static final Logger log = LoggerFactory.getLogger(ExportXmlDownloadPage.class);

    public ExportXmlDownloadPage(PageParameters parameters) {
        super(parameters);

        String nodeId = parameters.get("nodeId").toString();
        String nodeName = parameters.get("nodeName").toString();
        boolean prettyPrint = parameters.get("prettyPrint").toBoolean(true);
        boolean includeNamespaces = parameters.get("includeNamespaces").toBoolean(false);
        boolean includeMetadata = parameters.get("includeMetadata").toBoolean(false);

        if (nodeId == null || nodeId.isEmpty() || nodeName == null || nodeName.isEmpty()) {
            return;
        }

        try {
            org.apache.wicket.Session wicketSession = org.apache.wicket.Session.get();
            javax.jcr.Session jcrSession = ((UserSession) wicketSession).getJcrSession();
            HippoWorkspace workspace = (HippoWorkspace) jcrSession.getWorkspace();
            WorkflowManager workflowManager = workspace.getWorkflowManager();

            Node node = jcrSession.getNodeByIdentifier(nodeId);
            ExportXmlWorkflow xmlWorkflow = (ExportXmlWorkflow) workflowManager.getWorkflow("exportxml", node);

            if (xmlWorkflow == null) {
                throw new RuntimeException("XML export workflow not found");
            }

            // Try to retrieve from session first
            String xmlContent = (String) wicketSession.getAttribute("exportXml_" + nodeId);

            // Measure workflow execution time
            long startTime = System.currentTimeMillis();

            // If dialog options differ from defaults, re-export with new options
            if (xmlContent != null && (prettyPrint != true || includeNamespaces != false || includeMetadata != false)) {
                xmlContent = xmlWorkflow.exportXmlDocument(nodeId, prettyPrint, includeNamespaces, includeMetadata);
            } else if (xmlContent == null) {
                // If not in session, export with current parameters
                xmlContent = xmlWorkflow.exportXmlDocument(nodeId, prettyPrint, includeNamespaces, includeMetadata);
            }

            long executionTime = System.currentTimeMillis() - startTime;
            log.info("XML workflow execution took {} ms", executionTime);

            if (executionTime > 5000) {
                log.warn("XML export took a long time: {} ms - consider optimization", executionTime);
            }

            String fileName = nodeName + ".xml";
            byte[] xmlBytes = xmlContent.getBytes("UTF-8");

            // Validate size
            if (xmlBytes.length > 10_000_000) {
                log.warn("XML file is large: {} bytes for nodeId: {}", xmlBytes.length, nodeId);
            }

            RequestCycle cycle = RequestCycle.get();
            HttpServletResponse httpResponse = (HttpServletResponse) cycle.getResponse().getContainerResponse();

            httpResponse.setContentType("application/xml; charset=UTF-8");
            httpResponse.setHeader("Content-Disposition",
                    "attachment; filename=\"" + fileName + "\"");
            // Use chunked transfer encoding instead of setting content length
            httpResponse.setHeader("Transfer-Encoding", "chunked");

            try {
                httpResponse.getOutputStream().write(xmlBytes);
            } catch (IOException e) {
                // Handle client abort gracefully
                log.warn("Could not complete XML response write: {}", e.getMessage());
            }

            log.info("Served XML download for nodeId: {} (prettyPrint: {}, includeNamespaces: {}, includeMetadata: {})",
                    nodeId, prettyPrint, includeNamespaces, includeMetadata);

        } catch (RepositoryException e) {
            log.error("Repository error in XML download: {}", e.getMessage(), e);
        } catch (IOException e) {
            // Handles broken pipe and other IO errors (including client aborts)
            if (e.getMessage() != null && e.getMessage().contains("Broken pipe")) {
                log.warn("Client disconnected during XML download for nodeId: {}", nodeId);
            } else {
                log.error("Error writing response: {}", e.getMessage(), e);
            }
        } catch (Exception e) {
            log.error("Error in XML download page: {}", e.getMessage(), e);
        }
    }

}
