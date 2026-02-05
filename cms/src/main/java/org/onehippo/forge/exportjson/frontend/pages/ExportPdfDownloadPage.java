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
import org.onehippo.forge.exportjson.repository.workflow.ExportPdfWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExportPdfDownloadPage extends Page {

    private static final Logger log = LoggerFactory.getLogger(ExportPdfDownloadPage.class);

    public ExportPdfDownloadPage(PageParameters parameters) {
        super(parameters);

        String nodeId = parameters.get("nodeId").toString();
        String nodeName = parameters.get("nodeName").toString();
        String pageSize = parameters.get("pageSize").toString("A4");
        String orientation = parameters.get("orientation").toString("portrait");
        String fontSize = parameters.get("fontSize").toString("medium");
        boolean includeMetadata = parameters.get("includeMetadata").toBoolean(true);

        if (nodeId == null || nodeId.isEmpty() || nodeName == null || nodeName.isEmpty()) {
            return;
        }

        try {
            org.apache.wicket.Session wicketSession = org.apache.wicket.Session.get();
            javax.jcr.Session jcrSession = ((UserSession) wicketSession).getJcrSession();
            HippoWorkspace workspace = (HippoWorkspace) jcrSession.getWorkspace();
            WorkflowManager workflowManager = workspace.getWorkflowManager();

            Node node = jcrSession.getNodeByIdentifier(nodeId);
            ExportPdfWorkflow pdfWorkflow = (ExportPdfWorkflow) workflowManager.getWorkflow("exportpdf", node);

            if (pdfWorkflow == null) {
                throw new RuntimeException("PDF export workflow not found");
            }

            // Try to retrieve from session first
            byte[] pdfContent = (byte[]) wicketSession.getAttribute("exportPdf_" + nodeId);

            // Measure workflow execution time
            long startTime = System.currentTimeMillis();

            // If dialog options differ from defaults, re-export with new options
            if (pdfContent != null && (!pageSize.equals("A4") || !orientation.equals("portrait")
                    || !fontSize.equals("medium") || includeMetadata != true)) {
                pdfContent = pdfWorkflow.exportPdfDocument(nodeId, pageSize, orientation, fontSize, includeMetadata);
            } else if (pdfContent == null) {
                // If not in session, export with current parameters
                pdfContent = pdfWorkflow.exportPdfDocument(nodeId, pageSize, orientation, fontSize, includeMetadata);
            }

            long executionTime = System.currentTimeMillis() - startTime;
            log.info("PDF workflow execution took {} ms", executionTime);

            if (executionTime > 5000) {
                log.warn("PDF export took a long time: {} ms - consider optimization", executionTime);
            }

            String fileName = nodeName + ".pdf";

            // Validate size
            if (pdfContent.length > 50_000_000) {
                log.warn("PDF file is large: {} bytes for nodeId: {}", pdfContent.length, nodeId);
            }

            RequestCycle cycle = RequestCycle.get();
            HttpServletResponse httpResponse = (HttpServletResponse) cycle.getResponse().getContainerResponse();

            httpResponse.setContentType("application/pdf");
            httpResponse.setHeader("Content-Disposition",
                    "attachment; filename=\"" + fileName + "\"");
            httpResponse.setContentLength(pdfContent.length);

            try {
                httpResponse.getOutputStream().write(pdfContent);
            } catch (IOException e) {
                // Handle client abort gracefully
                log.warn("Could not complete PDF response write: {}", e.getMessage());
            }

            log.info("Served PDF download for nodeId: {} (pageSize: {}, orientation: {}, fontSize: {}, includeMetadata: {})",
                    nodeId, pageSize, orientation, fontSize, includeMetadata);

            // Clean up session attribute
            wicketSession.removeAttribute("exportPdf_" + nodeId);

        } catch (RepositoryException e) {
            log.error("Repository error in PDF download: {}", e.getMessage(), e);
        } catch (IOException e) {
            // Handles broken pipe and other IO errors (including client aborts)
            if (e.getMessage() != null && e.getMessage().contains("Broken pipe")) {
                log.warn("Client disconnected during PDF download for nodeId: {}", nodeId);
            } else {
                log.error("Error writing response: {}", e.getMessage(), e);
            }
        } catch (Exception e) {
            log.error("Error in PDF download page: {}", e.getMessage(), e);
        }
    }

}
