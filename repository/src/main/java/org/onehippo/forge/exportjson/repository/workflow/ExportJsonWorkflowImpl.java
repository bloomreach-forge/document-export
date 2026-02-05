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
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;

import org.hippoecm.repository.api.WorkflowContext;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.ext.WorkflowImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class ExportJsonWorkflowImpl extends WorkflowImpl implements ExportJsonWorkflow {

    private final static Logger log = LoggerFactory.getLogger(ExportJsonWorkflowImpl.class);

    public ExportJsonWorkflowImpl() throws RemoteException {
    }

    @Override
    public String exportJsonDocument(final String subjectId) throws RepositoryException, JsonProcessingException {

        final WorkflowContext workflowContext = getWorkflowContext();
        final Session internalWorkflowSession = workflowContext.getInternalWorkflowSession();
        final Node documentNode = internalWorkflowSession.getNodeByIdentifier(subjectId);

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode rootNode = mapper.createObjectNode();

        // Add basic document info
        rootNode.put("documentPath", documentNode.getPath());
        rootNode.put("documentName", documentNode.getName());
        rootNode.put("primaryType", documentNode.getPrimaryNodeType().getName());

        // Add all properties
        ObjectNode propertiesNode = mapper.createObjectNode();
        PropertyIterator properties = documentNode.getProperties();

        while (properties.hasNext()) {
            javax.jcr.Property property = properties.nextProperty();
            String propertyName = property.getName();

            try {
                if (property.isMultiple()) {
                    Value[] values = property.getValues();
                    if (values.length > 0) {
                        com.fasterxml.jackson.databind.node.ArrayNode arrayNode = mapper.createArrayNode();
                        for (Value value : values) {
                            arrayNode.add(value.getString());
                        }
                        propertiesNode.set(propertyName, arrayNode);
                    }
                } else {
                    propertiesNode.put(propertyName, property.getValue().getString());
                }
            } catch (Exception e) {
                log.warn("Could not serialize property {}: {}", propertyName, e.getMessage());
                propertiesNode.put(propertyName, "[Unable to serialize]");
            }
        }

        rootNode.set("properties", propertiesNode);

        // Add metadata
        ObjectNode metadataNode = mapper.createObjectNode();
        try {
            if (documentNode.hasProperty("jcr:created")) {
                metadataNode.put("created", documentNode.getProperty("jcr:created").getValue().getString());
            }
            if (documentNode.hasProperty("jcr:lastModified")) {
                metadataNode.put("lastModified", documentNode.getProperty("jcr:lastModified").getValue().getString());
            }
            if (documentNode.hasProperty("hippo:author")) {
                metadataNode.put("author", documentNode.getProperty("hippo:author").getValue().getString());
            }
        } catch (RepositoryException e) {
            log.warn("Could not retrieve metadata: {}", e.getMessage());
        }

        rootNode.set("metadata", metadataNode);

        // Pretty print the JSON
        String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(rootNode);

        return json;
    }

    /**
     * This method is required by the WorkflowImpl interface but is not used in this implementation.
     * The actual workflow execution is handled by the {@link #exportJsonDocument(String)} method.
     */
    @Override
    public void invokeWorkflow() throws Exception {
        // No-op: workflow logic is handled by exportJsonDocument()
    }
}
