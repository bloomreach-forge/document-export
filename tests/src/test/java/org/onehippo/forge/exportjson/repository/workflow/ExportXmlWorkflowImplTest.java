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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeType;

import org.hippoecm.repository.api.WorkflowContext;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.ext.WorkflowImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExportXmlWorkflowImplTest {

    @Mock private WorkflowContext mockContext;
    @Mock private Session mockSession;
    @Mock private Node mockDocument;
    @Mock private NodeType mockNodeType;
    @Mock private PropertyIterator mockPropertyIterator;

    private ExportXmlWorkflowImpl workflow;

    private static final String NODE_ID = "test-id";

    @BeforeEach
    void setUp() throws Exception {
        workflow = new ExportXmlWorkflowImpl();
        injectContext(workflow, mockContext);
    }

    @Test
    void exportXmlDocument_basicDocument_containsRequiredElements() throws Exception {
        stubEmptyDocument();

        String xml = workflow.exportXmlDocument(NODE_ID, false, false, false);

        assertTrue(xml.startsWith("<?xml version=\"1.0\""));
        assertTrue(xml.contains("<document>"));
        assertTrue(xml.contains("<nodeName>test-doc</nodeName>"));
        assertTrue(xml.contains("<path>/content/documents/test-doc</path>"));
        assertTrue(xml.contains("<nodeType>content:document</nodeType>"));
        assertTrue(xml.contains("</document>"));
    }

    @Test
    void exportXmlDocument_withMetadataAllPresent_includesMetadataSection() throws Exception {
        Property created = mock(Property.class);
        Property modified = mock(Property.class);
        Value createdVal = mock(Value.class);
        Value modifiedVal = mock(Value.class);

        stubEmptyDocument();
        when(mockDocument.hasProperty("jcr:created")).thenReturn(true);
        when(mockDocument.getProperty("jcr:created")).thenReturn(created);
        when(created.getValue()).thenReturn(createdVal);
        when(createdVal.getString()).thenReturn("2024-01-01");
        when(mockDocument.hasProperty("jcr:lastModified")).thenReturn(true);
        when(mockDocument.getProperty("jcr:lastModified")).thenReturn(modified);
        when(modified.getValue()).thenReturn(modifiedVal);
        when(modifiedVal.getString()).thenReturn("2024-06-01");

        String xml = workflow.exportXmlDocument(NODE_ID, false, false, true);

        assertTrue(xml.contains("<metadata>"));
        assertTrue(xml.contains("<created>2024-01-01</created>"));
        assertTrue(xml.contains("<lastModified>2024-06-01</lastModified>"));
    }

    @Test
    void exportXmlDocument_withMetadataAllAbsent_metadataBlockIsEmpty() throws Exception {
        stubEmptyDocument();
        when(mockDocument.hasProperty("jcr:created")).thenReturn(false);
        when(mockDocument.hasProperty("jcr:lastModified")).thenReturn(false);

        String xml = workflow.exportXmlDocument(NODE_ID, false, false, true);

        assertTrue(xml.contains("<metadata>"));
        assertFalse(xml.contains("<created>"));
        assertFalse(xml.contains("<lastModified>"));
    }

    @Test
    void exportXmlDocument_withoutMetadata_omitsMetadataBlock() throws Exception {
        stubEmptyDocument();

        String xml = workflow.exportXmlDocument(NODE_ID, false, false, false);

        assertFalse(xml.contains("<metadata>"));
    }

    @Test
    void exportXmlDocument_jcrPrefixedProperty_isSkipped() throws Exception {
        Property prop = mock(Property.class);
        stubDocumentWithProperty(prop);
        when(prop.getName()).thenReturn("jcr:created");

        String xml = workflow.exportXmlDocument(NODE_ID, false, false, false);

        assertFalse(xml.contains("<jcr_created>"));
    }

    @Test
    void exportXmlDocument_hippoPrefixedProperty_isSkipped() throws Exception {
        Property prop = mock(Property.class);
        stubDocumentWithProperty(prop);
        when(prop.getName()).thenReturn("hippo:author");

        String xml = workflow.exportXmlDocument(NODE_ID, false, false, false);

        assertFalse(xml.contains("<hippo_author>"));
    }

    @Test
    void exportXmlDocument_singleValuedProperty_createsOneElement() throws Exception {
        Property prop = mock(Property.class);
        Value val = mock(Value.class);
        stubDocumentWithProperty(prop);
        when(prop.getName()).thenReturn("content:title");
        when(prop.isMultiple()).thenReturn(false);
        when(prop.getValue()).thenReturn(val);
        when(val.getString()).thenReturn("Hello World");

        String xml = workflow.exportXmlDocument(NODE_ID, false, false, false);

        assertTrue(xml.contains("<content_title>Hello World</content_title>"));
    }

    @Test
    void exportXmlDocument_multiValuedProperty_createsOneElementPerValue() throws Exception {
        Property prop = mock(Property.class);
        Value v1 = mock(Value.class);
        Value v2 = mock(Value.class);
        stubDocumentWithProperty(prop);
        when(prop.getName()).thenReturn("content:tags");
        when(prop.isMultiple()).thenReturn(true);
        when(prop.getValues()).thenReturn(new Value[]{v1, v2});
        when(v1.getString()).thenReturn("alpha");
        when(v2.getString()).thenReturn("beta");

        String xml = workflow.exportXmlDocument(NODE_ID, false, false, false);

        assertTrue(xml.contains("<content_tags>alpha</content_tags>"));
        assertTrue(xml.contains("<content_tags>beta</content_tags>"));
    }

    @Test
    void exportXmlDocument_unserializableProperty_noClosingTagAndNoException() throws Exception {
        Property prop = mock(Property.class);
        stubDocumentWithProperty(prop);
        when(prop.getName()).thenReturn("content:title");
        when(prop.isMultiple()).thenReturn(false);
        when(prop.getValue()).thenThrow(new RepositoryException("Cannot read"));

        String xml = workflow.exportXmlDocument(NODE_ID, false, false, false);

        // Method must not throw; the value and closing tag are absent even if the opening tag was buffered
        assertNotNull(xml);
        assertTrue(xml.contains("</properties>"));
        assertFalse(xml.contains("</content_title>"), "Closing tag should not appear for a failed property");
    }

    @Test
    void exportXmlDocument_xmlSpecialCharsInValue_escaped() throws Exception {
        Property prop = mock(Property.class);
        Value val = mock(Value.class);
        stubDocumentWithProperty(prop);
        when(prop.getName()).thenReturn("content:body");
        when(prop.isMultiple()).thenReturn(false);
        when(prop.getValue()).thenReturn(val);
        when(val.getString()).thenReturn("<b>bold & 'quoted'</b>");

        String xml = workflow.exportXmlDocument(NODE_ID, false, false, false);

        assertTrue(xml.contains("&lt;b&gt;bold &amp; &apos;quoted&apos;&lt;/b&gt;"));
    }

    @Test
    void exportXmlDocument_invalidNodeId_throwsRepositoryException() throws Exception {
        when(mockContext.getInternalWorkflowSession()).thenReturn(mockSession);
        when(mockSession.getNodeByIdentifier(NODE_ID))
                .thenThrow(new RepositoryException("Node not found"));

        assertThrows(RepositoryException.class,
                () -> workflow.exportXmlDocument(NODE_ID, false, false, false));
    }

    @Test
    void invokeWorkflow_isNoOp() throws Exception {
        assertDoesNotThrow(() -> workflow.invokeWorkflow());
    }

    // --- helpers ---

    private void stubEmptyDocument() throws RepositoryException {
        when(mockContext.getInternalWorkflowSession()).thenReturn(mockSession);
        when(mockSession.getNodeByIdentifier(NODE_ID)).thenReturn(mockDocument);
        when(mockDocument.getName()).thenReturn("test-doc");
        when(mockDocument.getPath()).thenReturn("/content/documents/test-doc");
        when(mockDocument.getPrimaryNodeType()).thenReturn(mockNodeType);
        when(mockNodeType.getName()).thenReturn("content:document");
        when(mockDocument.getIdentifier()).thenReturn(NODE_ID);
        when(mockDocument.getProperties()).thenReturn(mockPropertyIterator);
        when(mockPropertyIterator.hasNext()).thenReturn(false);
    }

    private void stubDocumentWithProperty(Property prop) throws RepositoryException {
        when(mockContext.getInternalWorkflowSession()).thenReturn(mockSession);
        when(mockSession.getNodeByIdentifier(NODE_ID)).thenReturn(mockDocument);
        when(mockDocument.getName()).thenReturn("test-doc");
        when(mockDocument.getPath()).thenReturn("/content/documents/test-doc");
        when(mockDocument.getPrimaryNodeType()).thenReturn(mockNodeType);
        when(mockNodeType.getName()).thenReturn("content:document");
        when(mockDocument.getIdentifier()).thenReturn(NODE_ID);
        when(mockDocument.getProperties()).thenReturn(mockPropertyIterator);
        when(mockPropertyIterator.hasNext()).thenReturn(true, false);
        when(mockPropertyIterator.nextProperty()).thenReturn(prop);
    }

    private static void injectContext(WorkflowImpl target, WorkflowContext context) throws Exception {
        Field field = WorkflowImpl.class.getDeclaredField("context");
        field.setAccessible(true);
        field.set(target, context);
    }
}
