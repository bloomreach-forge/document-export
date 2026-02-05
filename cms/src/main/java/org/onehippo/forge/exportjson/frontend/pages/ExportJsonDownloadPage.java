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
import org.apache.wicket.request.http.WebRequest;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.cycle.RequestCycle;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.WorkflowManager;
import org.onehippo.forge.exportjson.repository.workflow.ExportJsonWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

public class ExportJsonDownloadPage extends Page {

    private static final Logger log = LoggerFactory.getLogger(ExportJsonDownloadPage.class);

    public ExportJsonDownloadPage(PageParameters parameters) {
        super(parameters);

        String nodeId = parameters.get("nodeId").toString();
        String nodeName = parameters.get("nodeName").toString();
        String format = parameters.get("format").toOptionalString();
        boolean prettyPrint = format == null || !"compact".equals(format);

        if (nodeId == null || nodeId.isEmpty() || nodeName == null || nodeName.isEmpty()) {
            return;
        }

        try {
            // Try to get JSON from session first (pre-computed during workflow)
            org.apache.wicket.Session wicketSession = org.apache.wicket.Session.get();
            String jsonContent = (String) wicketSession.getAttribute("exportJson_" + nodeId);

            if (jsonContent == null) {
                // Try to execute the export if not in session
                try {
                    javax.jcr.Session jcrSession = ((UserSession) wicketSession).getJcrSession();
                    HippoWorkspace workspace = (HippoWorkspace) jcrSession.getWorkspace();
                    WorkflowManager workflowManager = workspace.getWorkflowManager();

                    Node node = jcrSession.getNodeByIdentifier(nodeId);
                    ExportJsonWorkflow workflow = (ExportJsonWorkflow) workflowManager.getWorkflow("exportjson", node);

                    jsonContent = workflow.exportJsonDocument(nodeId);
                } catch (Exception e) {
                    log.error("Failed to export on fallback: {}", e.getMessage());
                    return;
                }
            } else {
                // Clean up session
                wicketSession.removeAttribute("exportJson_" + nodeId);
            }

            // Format the JSON based on user preference
            String formattedJson = jsonContent;
            if (!prettyPrint) {
                try {
                    ObjectMapper mapper = new ObjectMapper();
                    // Parse the JSON and re-serialize without pretty printing
                    Object jsonObject = mapper.readValue(jsonContent, Object.class);
                    formattedJson = mapper.writeValueAsString(jsonObject);
                } catch (Exception e) {
                    log.warn("Failed to compact JSON, using original: {}", e.getMessage());
                    formattedJson = jsonContent;
                }
            }

            String fileName = nodeName + ".json";
            byte[] jsonBytes = formattedJson.getBytes("UTF-8");

            RequestCycle cycle = RequestCycle.get();
            HttpServletResponse httpResponse = (HttpServletResponse) cycle.getResponse().getContainerResponse();

            httpResponse.setContentType("application/json");
            httpResponse.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
            httpResponse.setContentLength(jsonBytes.length);
            httpResponse.getOutputStream().write(jsonBytes);
            httpResponse.getOutputStream().flush();

            log.info("Served JSON download for nodeId: {} (format: {})", nodeId, prettyPrint ? "pretty" : "compact");

        } catch (IOException e) {
            log.error("Error writing response: {}", e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error in download page: {}", e.getMessage(), e);
        }
    }
}
