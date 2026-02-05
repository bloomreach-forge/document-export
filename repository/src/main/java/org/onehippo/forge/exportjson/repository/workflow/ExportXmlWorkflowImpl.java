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

import java.rmi.RemoteException;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;

import org.hippoecm.repository.api.WorkflowContext;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.ext.WorkflowImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ExportXmlWorkflowImpl extends WorkflowImpl implements ExportXmlWorkflow {

    private static final Logger log = LoggerFactory.getLogger(ExportXmlWorkflowImpl.class);

    public ExportXmlWorkflowImpl() throws RemoteException {
    }

    @Override
    public String exportXmlDocument(String subjectId, boolean prettyPrint, boolean includeNamespaces,
            boolean includeMetadata) throws RepositoryException, WorkflowException {
        try {
            final WorkflowContext workflowContext = getWorkflowContext();
            final Session internalWorkflowSession = workflowContext.getInternalWorkflowSession();
            final Node documentNode = internalWorkflowSession.getNodeByIdentifier(subjectId);

            StringBuilder xml = new StringBuilder();
            xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            xml.append("<document>\n");

            // Add node metadata
            xml.append("  <nodeName>").append(escapeXml(documentNode.getName())).append("</nodeName>\n");
            xml.append("  <nodeType>").append(escapeXml(documentNode.getPrimaryNodeType().getName())).append("</nodeType>\n");
            xml.append("  <path>").append(escapeXml(documentNode.getPath())).append("</path>\n");
            xml.append("  <identifier>").append(escapeXml(documentNode.getIdentifier())).append("</identifier>\n");

            // Add metadata if requested
            if (includeMetadata) {
                xml.append("  <metadata>\n");
                if (documentNode.hasProperty("jcr:created")) {
                    xml.append("    <created>").append(escapeXml(documentNode.getProperty("jcr:created").getValue().getString())).append("</created>\n");
                }
                if (documentNode.hasProperty("jcr:lastModified")) {
                    xml.append("    <lastModified>").append(escapeXml(documentNode.getProperty("jcr:lastModified").getValue().getString())).append("</lastModified>\n");
                }
                xml.append("  </metadata>\n");
            }

            // Add properties
            xml.append("  <properties>\n");
            PropertyIterator properties = documentNode.getProperties();
            while (properties.hasNext()) {
                Property property = properties.nextProperty();
                String propertyName = property.getName();

                // Skip internal JCR properties
                if (propertyName.startsWith("jcr:") || propertyName.startsWith("hippo:")) {
                    continue;
                }

                try {
                    String xmlSafeName = propertyName.replace(":", "_");
                    if (property.isMultiple()) {
                        Value[] values = property.getValues();
                        for (Value value : values) {
                            xml.append("    <").append(xmlSafeName).append(">").append(escapeXml(value.getString())).append("</").append(xmlSafeName).append(">\n");
                        }
                    } else {
                        xml.append("    <").append(xmlSafeName).append(">").append(escapeXml(property.getValue().getString())).append("</").append(xmlSafeName).append(">\n");
                    }
                } catch (Exception e) {
                    log.warn("Could not serialize property {}: {}", propertyName, e.getMessage());
                }
            }
            xml.append("  </properties>\n");

            xml.append("</document>");

            return xml.toString();
        } catch (RepositoryException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error exporting XML document: {}", e.getMessage(), e);
            throw new WorkflowException("Failed to export XML document: " + e.getMessage(), e);
        }
    }

    private String escapeXml(String input) {
        if (input == null) {
            return "";
        }
        return input.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&apos;");
    }


    /**
     * This method is required by the WorkflowImpl interface but is not used in this implementation.
     * The actual workflow execution is handled by the {@link #exportXmlDocument(String, boolean, boolean, boolean)} method.
     */
    @Override
    public void invokeWorkflow() throws Exception {
        // No-op: workflow logic is handled by exportXmlDocument()
    }

}
