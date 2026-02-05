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
package org.onehippo.forge.exportjson.repository.workflow;

import java.io.IOException;
import java.rmi.RemoteException;

import javax.jcr.Node;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;

import org.hippoecm.repository.api.WorkflowContext;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.ext.WorkflowImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExportHtmlWorkflowImpl extends WorkflowImpl implements ExportHtmlWorkflow {

    private final static Logger log = LoggerFactory.getLogger(ExportHtmlWorkflowImpl.class);

    public ExportHtmlWorkflowImpl() throws RemoteException {
    }

    @Override
    public String exportHtmlDocument(final String subjectId, final boolean includeMetadata,
                                     final boolean includeStyles)
        throws RepositoryException, IOException {

        final WorkflowContext workflowContext = getWorkflowContext();
        final Session internalWorkflowSession = workflowContext.getInternalWorkflowSession();
        final Node documentNode = internalWorkflowSession.getNodeByIdentifier(subjectId);

        return generateHtml(documentNode, includeMetadata, includeStyles);
    }

    private String generateHtml(Node documentNode, boolean includeMetadata, boolean includeStyles)
        throws RepositoryException {
        StringBuilder html = new StringBuilder();

        // HTML header
        html.append("<!DOCTYPE html>\n");
        html.append("<html lang=\"en\">\n");
        html.append("<head>\n");
        html.append("  <meta charset=\"UTF-8\">\n");
        html.append("  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
        html.append("  <title>").append(escapeHtml(documentNode.getName())).append("</title>\n");

        // Optional CSS styling
        if (includeStyles) {
            html.append("  <style>\n");
            html.append("    body { font-family: Arial, sans-serif; margin: 20px; line-height: 1.6; }\n");
            html.append("    .header { background-color: #f0f0f0; padding: 15px; margin-bottom: 20px; border-radius: 4px; }\n");
            html.append("    .header h1 { margin: 0 0 10px 0; color: #333; }\n");
            html.append("    .header p { margin: 5px 0; color: #666; font-size: 0.9em; }\n");
            html.append("    .properties { margin-bottom: 20px; }\n");
            html.append("    .properties h2 { border-bottom: 2px solid #007bff; padding-bottom: 10px; }\n");
            html.append("    .property-row { display: grid; grid-template-columns: 200px 1fr; padding: 10px; border-bottom: 1px solid #eee; }\n");
            html.append("    .property-row:nth-child(odd) { background-color: #f9f9f9; }\n");
            html.append("    .property-name { font-weight: bold; color: #333; }\n");
            html.append("    .property-value { color: #666; word-break: break-word; }\n");
            html.append("    .metadata { margin-top: 20px; background-color: #f9f9f9; padding: 15px; border-radius: 4px; }\n");
            html.append("    .metadata h2 { margin-top: 0; }\n");
            html.append("    .metadata-item { margin: 10px 0; }\n");
            html.append("    .metadata-label { font-weight: bold; color: #333; }\n");
            html.append("  </style>\n");
        }

        html.append("</head>\n");
        html.append("<body>\n");

        // Document header
        html.append("  <div class=\"header\">\n");
        html.append("    <h1>").append(escapeHtml(documentNode.getName())).append("</h1>\n");
        html.append("    <p><strong>Path:</strong> ").append(escapeHtml(documentNode.getPath())).append("</p>\n");
        html.append("    <p><strong>Type:</strong> ").append(escapeHtml(documentNode.getPrimaryNodeType().getName())).append("</p>\n");
        html.append("  </div>\n");

        // Properties section
        html.append("  <div class=\"properties\">\n");
        html.append("    <h2>Properties</h2>\n");

        PropertyIterator properties = documentNode.getProperties();
        while (properties.hasNext()) {
            javax.jcr.Property property = properties.nextProperty();
            String propertyName = property.getName();

            try {
                String value;
                if (property.isMultiple()) {
                    Value[] values = property.getValues();
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < values.length; i++) {
                        if (i > 0) sb.append("<br>");
                        sb.append(escapeHtml(values[i].getString()));
                    }
                    value = sb.toString();
                } else {
                    value = escapeHtml(property.getValue().getString());
                }

                html.append("    <div class=\"property-row\">\n");
                html.append("      <div class=\"property-name\">").append(escapeHtml(propertyName)).append("</div>\n");
                html.append("      <div class=\"property-value\">").append(value).append("</div>\n");
                html.append("    </div>\n");
            } catch (Exception e) {
                log.warn("Could not serialize property {}: {}", propertyName, e.getMessage());
                html.append("    <div class=\"property-row\">\n");
                html.append("      <div class=\"property-name\">").append(escapeHtml(propertyName)).append("</div>\n");
                html.append("      <div class=\"property-value\">[Unable to serialize]</div>\n");
                html.append("    </div>\n");
            }
        }

        html.append("  </div>\n");

        // Metadata section if requested
        if (includeMetadata) {
            html.append("  <div class=\"metadata\">\n");
            html.append("    <h2>Metadata</h2>\n");

            if (documentNode.hasProperty("jcr:created")) {
                String created = documentNode.getProperty("jcr:created").getValue().getString();
                html.append("    <div class=\"metadata-item\">\n");
                html.append("      <span class=\"metadata-label\">Created:</span> ").append(escapeHtml(created)).append("\n");
                html.append("    </div>\n");
            }

            if (documentNode.hasProperty("jcr:lastModified")) {
                String modified = documentNode.getProperty("jcr:lastModified").getValue().getString();
                html.append("    <div class=\"metadata-item\">\n");
                html.append("      <span class=\"metadata-label\">Last Modified:</span> ").append(escapeHtml(modified)).append("\n");
                html.append("    </div>\n");
            }

            if (documentNode.hasProperty("hippo:author")) {
                String author = documentNode.getProperty("hippo:author").getValue().getString();
                html.append("    <div class=\"metadata-item\">\n");
                html.append("      <span class=\"metadata-label\">Author:</span> ").append(escapeHtml(author)).append("\n");
                html.append("    </div>\n");
            }

            html.append("  </div>\n");
        }

        html.append("</body>\n");
        html.append("</html>\n");

        return html.toString();
    }

    private String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;");
    }

    /**
     * This method is required by the WorkflowImpl interface but is not used in this implementation.
     * The actual workflow execution is handled by the {@link #exportHtmlDocument(String, boolean, boolean)} method.
     */
    @Override
    public void invokeWorkflow() throws Exception {
        // No-op: workflow logic is handled by exportHtmlDocument()
    }
}
