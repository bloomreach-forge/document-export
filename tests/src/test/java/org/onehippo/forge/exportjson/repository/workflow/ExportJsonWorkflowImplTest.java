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

import static org.junit.Assert.*;
import static org.easymock.EasyMock.*;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeType;

import org.hippoecm.repository.api.WorkflowContext;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Unit tests for ExportJsonWorkflowImpl
 */
public class ExportJsonWorkflowImplTest {

    private WorkflowContext mockContext;
    private Session mockSession;
    private Node mockDocument;
    private NodeType mockNodeType;
    private PropertyIterator mockPropertyIterator;

    @Before
    public void setUp() throws Exception {
        mockContext = createMock(WorkflowContext.class);
        mockSession = createMock(Session.class);
        mockDocument = createMock(Node.class);
        mockNodeType = createMock(NodeType.class);
        mockPropertyIterator = createMock(PropertyIterator.class);
    }

    @Test
    public void testExportJsonDocumentBasicInfo() throws Exception {
        setupBasicMocks();

        ExportJsonWorkflowImpl workflow = createWorkflowWithMocks(mockContext);
        String json = workflow.exportJsonDocument("test-id");

        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(json);

        assertEquals("JSON should contain correct document path", "/content/documents/test",
            root.get("documentPath").asText());
        assertEquals("JSON should contain correct document name", "test",
            root.get("documentName").asText());
        assertEquals("JSON should contain correct primary type", "content:document",
            root.get("primaryType").asText());
        assertNotNull("JSON should contain properties node", root.get("properties"));
        assertNotNull("JSON should contain metadata node", root.get("metadata"));
    }

    @Test
    public void testExportJsonDocumentWithSingleValuedProperty() throws Exception {
        Property mockProperty = createMock(Property.class);
        Value mockValue = createMock(Value.class);

        expect(mockContext.getInternalWorkflowSession()).andReturn(mockSession);
        expect(mockSession.getNodeByIdentifier("test-id")).andReturn(mockDocument);
        expect(mockDocument.getPath()).andReturn("/content/documents/test");
        expect(mockDocument.getName()).andReturn("test");
        expect(mockDocument.getPrimaryNodeType()).andReturn(mockNodeType);
        expect(mockNodeType.getName()).andReturn("content:document");
        expect(mockDocument.getProperties()).andReturn(mockPropertyIterator);
        expect(mockPropertyIterator.hasNext()).andReturn(true).andReturn(false);
        expect(mockPropertyIterator.nextProperty()).andReturn(mockProperty);
        expect(mockProperty.getName()).andReturn("content:title");
        expect(mockProperty.isMultiple()).andReturn(false);
        expect(mockProperty.getValue()).andReturn(mockValue);
        expect(mockValue.getString()).andReturn("Test Document");
        expect(mockDocument.hasProperty("jcr:created")).andReturn(false);
        expect(mockDocument.hasProperty("jcr:lastModified")).andReturn(false);
        expect(mockDocument.hasProperty("hippo:author")).andReturn(false);
        replay(mockContext, mockSession, mockDocument, mockNodeType, mockPropertyIterator,
            mockProperty, mockValue);

        ExportJsonWorkflowImpl workflow = createWorkflowWithMocks(mockContext);
        String json = workflow.exportJsonDocument("test-id");

        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(json);

        assertEquals("JSON should contain property value", "Test Document",
            root.get("properties").get("content:title").asText());
    }

    @Test
    public void testExportJsonDocumentWithMultiValuedProperty() throws Exception {
        Property mockProperty = createMock(Property.class);
        Value mockValue1 = createMock(Value.class);
        Value mockValue2 = createMock(Value.class);
        Value mockValue3 = createMock(Value.class);
        Value[] values = {mockValue1, mockValue2, mockValue3};

        expect(mockContext.getInternalWorkflowSession()).andReturn(mockSession);
        expect(mockSession.getNodeByIdentifier("test-id")).andReturn(mockDocument);
        expect(mockDocument.getPath()).andReturn("/content/documents/test");
        expect(mockDocument.getName()).andReturn("test");
        expect(mockDocument.getPrimaryNodeType()).andReturn(mockNodeType);
        expect(mockNodeType.getName()).andReturn("content:document");
        expect(mockDocument.getProperties()).andReturn(mockPropertyIterator);
        expect(mockPropertyIterator.hasNext()).andReturn(true).andReturn(false);
        expect(mockPropertyIterator.nextProperty()).andReturn(mockProperty);
        expect(mockProperty.getName()).andReturn("content:tags");
        expect(mockProperty.isMultiple()).andReturn(true);
        expect(mockProperty.getValues()).andReturn(values);
        expect(mockValue1.getString()).andReturn("tag1");
        expect(mockValue2.getString()).andReturn("tag2");
        expect(mockValue3.getString()).andReturn("tag3");
        expect(mockDocument.hasProperty("jcr:created")).andReturn(false);
        expect(mockDocument.hasProperty("jcr:lastModified")).andReturn(false);
        expect(mockDocument.hasProperty("hippo:author")).andReturn(false);
        replay(mockContext, mockSession, mockDocument, mockNodeType, mockPropertyIterator,
            mockProperty, mockValue1, mockValue2, mockValue3);

        ExportJsonWorkflowImpl workflow = createWorkflowWithMocks(mockContext);
        String json = workflow.exportJsonDocument("test-id");

        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(json);
        JsonNode tagsArray = root.get("properties").get("content:tags");

        assertTrue("Multi-valued property should be array", tagsArray.isArray());
        assertEquals("Array should have 3 elements", 3, tagsArray.size());
        assertEquals("First element should be tag1", "tag1", tagsArray.get(0).asText());
        assertEquals("Second element should be tag2", "tag2", tagsArray.get(1).asText());
        assertEquals("Third element should be tag3", "tag3", tagsArray.get(2).asText());
    }

    @Test
    public void testExportJsonDocumentWithMetadata() throws Exception {
        Property createdProperty = createMock(Property.class);
        Property modifiedProperty = createMock(Property.class);
        Property authorProperty = createMock(Property.class);
        Value createdValue = createMock(Value.class);
        Value modifiedValue = createMock(Value.class);
        Value authorValue = createMock(Value.class);

        expect(mockContext.getInternalWorkflowSession()).andReturn(mockSession);
        expect(mockSession.getNodeByIdentifier("test-id")).andReturn(mockDocument);
        expect(mockDocument.getPath()).andReturn("/content/documents/test");
        expect(mockDocument.getName()).andReturn("test");
        expect(mockDocument.getPrimaryNodeType()).andReturn(mockNodeType);
        expect(mockNodeType.getName()).andReturn("content:document");
        expect(mockDocument.getProperties()).andReturn(mockPropertyIterator);
        expect(mockPropertyIterator.hasNext()).andReturn(false);

        // Metadata properties
        expect(mockDocument.hasProperty("jcr:created")).andReturn(true);
        expect(mockDocument.getProperty("jcr:created")).andReturn(createdProperty);
        expect(createdProperty.getValue()).andReturn(createdValue);
        expect(createdValue.getString()).andReturn("2024-01-15T10:30:00");

        expect(mockDocument.hasProperty("jcr:lastModified")).andReturn(true);
        expect(mockDocument.getProperty("jcr:lastModified")).andReturn(modifiedProperty);
        expect(modifiedProperty.getValue()).andReturn(modifiedValue);
        expect(modifiedValue.getString()).andReturn("2024-01-20T14:45:00");

        expect(mockDocument.hasProperty("hippo:author")).andReturn(true);
        expect(mockDocument.getProperty("hippo:author")).andReturn(authorProperty);
        expect(authorProperty.getValue()).andReturn(authorValue);
        expect(authorValue.getString()).andReturn("admin");

        replay(mockContext, mockSession, mockDocument, mockNodeType, mockPropertyIterator,
            createdProperty, modifiedProperty, authorProperty, createdValue, modifiedValue, authorValue);

        ExportJsonWorkflowImpl workflow = createWorkflowWithMocks(mockContext);
        String json = workflow.exportJsonDocument("test-id");

        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(json);
        JsonNode metadata = root.get("metadata");

        assertEquals("Created timestamp should be present", "2024-01-15T10:30:00",
            metadata.get("created").asText());
        assertEquals("Last modified timestamp should be present", "2024-01-20T14:45:00",
            metadata.get("lastModified").asText());
        assertEquals("Author should be present", "admin", metadata.get("author").asText());
    }

    @Test
    public void testExportJsonDocumentHandlesUnserializableProperty() throws Exception {
        Property mockProperty = createMock(Property.class);

        expect(mockContext.getInternalWorkflowSession()).andReturn(mockSession);
        expect(mockSession.getNodeByIdentifier("test-id")).andReturn(mockDocument);
        expect(mockDocument.getPath()).andReturn("/content/documents/test");
        expect(mockDocument.getName()).andReturn("test");
        expect(mockDocument.getPrimaryNodeType()).andReturn(mockNodeType);
        expect(mockNodeType.getName()).andReturn("content:document");
        expect(mockDocument.getProperties()).andReturn(mockPropertyIterator);
        expect(mockPropertyIterator.hasNext()).andReturn(true).andReturn(false);
        expect(mockPropertyIterator.nextProperty()).andReturn(mockProperty);
        expect(mockProperty.getName()).andReturn("problematic:property");
        expect(mockProperty.isMultiple()).andReturn(false);
        expect(mockProperty.getValue()).andThrow(new RepositoryException("Cannot access property"));
        expect(mockDocument.hasProperty("jcr:created")).andReturn(false);
        expect(mockDocument.hasProperty("jcr:lastModified")).andReturn(false);
        expect(mockDocument.hasProperty("hippo:author")).andReturn(false);
        replay(mockContext, mockSession, mockDocument, mockNodeType, mockPropertyIterator, mockProperty);

        ExportJsonWorkflowImpl workflow = createWorkflowWithMocks(mockContext);
        String json = workflow.exportJsonDocument("test-id");

        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(json);

        assertEquals("Unserializable property should be marked", "[Unable to serialize]",
            root.get("properties").get("problematic:property").asText());
    }

    @Test
    public void testExportJsonDocumentThrowsForInvalidNodeId() throws Exception {
        expect(mockContext.getInternalWorkflowSession()).andReturn(mockSession);
        expect(mockSession.getNodeByIdentifier("invalid-id"))
            .andThrow(new RepositoryException("Node not found"));
        replay(mockContext, mockSession);

        ExportJsonWorkflowImpl workflow = createWorkflowWithMocks(mockContext);

        assertThrows("Should propagate RepositoryException for invalid node ID",
            RepositoryException.class, () -> {
                workflow.exportJsonDocument("invalid-id");
            });
    }

    @Test
    public void testInvokeWorkflowIsNoOp() throws Exception {
        ExportJsonWorkflowImpl workflow = new ExportJsonWorkflowImpl();
        // Should not throw exception
        workflow.invokeWorkflow();
    }

    // Helper methods

    private void setupBasicMocks() throws Exception {
        expect(mockContext.getInternalWorkflowSession()).andReturn(mockSession);
        expect(mockSession.getNodeByIdentifier("test-id")).andReturn(mockDocument);
        expect(mockDocument.getPath()).andReturn("/content/documents/test");
        expect(mockDocument.getName()).andReturn("test");
        expect(mockDocument.getPrimaryNodeType()).andReturn(mockNodeType);
        expect(mockNodeType.getName()).andReturn("content:document");
        expect(mockDocument.getProperties()).andReturn(mockPropertyIterator);
        expect(mockPropertyIterator.hasNext()).andReturn(false);
        expect(mockDocument.hasProperty("jcr:created")).andReturn(false);
        expect(mockDocument.hasProperty("jcr:lastModified")).andReturn(false);
        expect(mockDocument.hasProperty("hippo:author")).andReturn(false);
        replay(mockContext, mockSession, mockDocument, mockNodeType, mockPropertyIterator);
    }

    private ExportJsonWorkflowImpl createWorkflowWithMocks(WorkflowContext context)
            throws Exception {
        ExportJsonWorkflowImpl workflow = new ExportJsonWorkflowImpl();
        // Mock the workflow context through reflection
        try {
            java.lang.reflect.Field field = workflow.getClass().getSuperclass().getDeclaredField("context");
            field.setAccessible(true);
            field.set(workflow, context);
        } catch (NoSuchFieldException e) {
            // Field not found, might be named differently or not exist
            throw new RuntimeException("Could not find context field in WorkflowImpl", e);
        }
        return workflow;
    }
}
