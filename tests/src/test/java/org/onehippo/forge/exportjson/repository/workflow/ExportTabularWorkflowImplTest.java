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
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;

import org.hippoecm.repository.api.WorkflowContext;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.ext.WorkflowImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.onehippo.forge.exportjson.repository.ExportTabularConstants;

@ExtendWith(MockitoExtension.class)
class ExportTabularWorkflowImplTest {

    @Mock private WorkflowContext mockContext;
    @Mock private Session mockSession;
    @Mock private Node mockDocument;
    @Mock private NodeType mockNodeType;
    @Mock private PropertyIterator mockPropertyIterator;

    private ExportTabularWorkflowImpl workflow;

    private static final String NODE_ID = "test-id";

    @BeforeEach
    void setUp() throws Exception {
        workflow = new ExportTabularWorkflowImpl();
        injectContext(workflow, mockContext);
    }

    @Test
    void exportTabularDocument_csvFormatWithHeaders_containsHeaderRow() throws Exception {
        stubEmptyDocument();

        String result = workflow.exportTabularDocument(NODE_ID, ExportTabularConstants.FORMAT_CSV, true, false);

        assertTrue(result.contains(ExportTabularConstants.COLUMN_PROPERTY_NAME));
        assertTrue(result.contains(ExportTabularConstants.COLUMN_PROPERTY_VALUE));
        assertTrue(result.contains(ExportTabularConstants.COLUMN_PROPERTY_TYPE));
    }

    @Test
    void exportTabularDocument_csvFormatNoHeaders_omitsHeaderRow() throws Exception {
        stubEmptyDocument();

        String result = workflow.exportTabularDocument(NODE_ID, ExportTabularConstants.FORMAT_CSV, false, false);

        assertFalse(result.contains(ExportTabularConstants.COLUMN_PROPERTY_NAME));
    }

    @Test
    void exportTabularDocument_tsvFormat_usesTabs() throws Exception {
        Property prop = mock(Property.class);
        javax.jcr.Value val = mock(javax.jcr.Value.class);
        stubDocumentWithProperty(prop);
        when(prop.getName()).thenReturn("content:title");
        when(prop.isMultiple()).thenReturn(false);
        when(prop.getValue()).thenReturn(val);
        when(val.getString()).thenReturn("Hello");
        when(prop.getType()).thenReturn(PropertyType.STRING);

        String result = workflow.exportTabularDocument(NODE_ID, ExportTabularConstants.FORMAT_TSV, false, false);

        assertTrue(result.contains("\t"), "TSV output should contain tab delimiters");
    }

    @Test
    void exportTabularDocument_quoteAllTrue_quotesAllValues() throws Exception {
        stubEmptyDocument();

        String result = workflow.exportTabularDocument(NODE_ID, ExportTabularConstants.FORMAT_CSV, true, true);

        assertTrue(result.contains("\"" + ExportTabularConstants.COLUMN_PROPERTY_NAME + "\""));
    }

    @Test
    void exportTabularDocument_singleValuedProperty_rowContainsValue() throws Exception {
        Property prop = mock(Property.class);
        javax.jcr.Value val = mock(javax.jcr.Value.class);
        stubDocumentWithProperty(prop);
        when(prop.getName()).thenReturn("content:title");
        when(prop.isMultiple()).thenReturn(false);
        when(prop.getValue()).thenReturn(val);
        when(val.getString()).thenReturn("My Title");
        when(prop.getType()).thenReturn(PropertyType.STRING);

        String result = workflow.exportTabularDocument(NODE_ID, ExportTabularConstants.FORMAT_CSV, false, false);

        assertTrue(result.contains("My Title"));
        assertTrue(result.contains("content:title"));
    }

    @Test
    void exportTabularDocument_multiValuedProperty_rowContainsJoinedValues() throws Exception {
        Property prop = mock(Property.class);
        javax.jcr.Value v1 = mock(javax.jcr.Value.class);
        javax.jcr.Value v2 = mock(javax.jcr.Value.class);
        stubDocumentWithProperty(prop);
        when(prop.getName()).thenReturn("content:tags");
        when(prop.isMultiple()).thenReturn(true);
        when(prop.getValues()).thenReturn(new javax.jcr.Value[]{v1, v2});
        when(v1.getString()).thenReturn("tag1");
        when(v2.getString()).thenReturn("tag2");
        when(prop.getType()).thenReturn(PropertyType.STRING);

        String result = workflow.exportTabularDocument(NODE_ID, ExportTabularConstants.FORMAT_CSV, false, false);

        assertTrue(result.contains("tag1"));
        assertTrue(result.contains("tag2"));
    }

    @Test
    void exportTabularDocument_unserializableProperty_rowContainsErrorMarker() throws Exception {
        Property prop = mock(Property.class);
        stubDocumentWithProperty(prop);
        when(prop.getName()).thenReturn("bad:prop");
        when(prop.isMultiple()).thenReturn(false);
        when(prop.getValue()).thenThrow(new RepositoryException("Cannot read"));

        String result = workflow.exportTabularDocument(NODE_ID, ExportTabularConstants.FORMAT_CSV, false, false);

        assertTrue(result.contains("[Unable to serialize]"));
        assertTrue(result.contains("Unknown"));
    }

    @Test
    void exportTabularDocument_invalidNodeId_throwsRepositoryException() throws Exception {
        when(mockContext.getInternalWorkflowSession()).thenReturn(mockSession);
        when(mockSession.getNodeByIdentifier(NODE_ID))
                .thenThrow(new RepositoryException("Node not found"));

        assertThrows(RepositoryException.class,
                () -> workflow.exportTabularDocument(NODE_ID, ExportTabularConstants.FORMAT_CSV, false, false));
    }

    @Test
    void invokeWorkflow_isNoOp() throws Exception {
        assertDoesNotThrow(() -> workflow.invokeWorkflow());
    }

    // --- helpers ---

    private void stubEmptyDocument() throws Exception {
        when(mockContext.getInternalWorkflowSession()).thenReturn(mockSession);
        when(mockSession.getNodeByIdentifier(NODE_ID)).thenReturn(mockDocument);
        when(mockDocument.getProperties()).thenReturn(mockPropertyIterator);
        when(mockPropertyIterator.hasNext()).thenReturn(false);
    }

    private void stubDocumentWithProperty(Property prop) throws Exception {
        when(mockContext.getInternalWorkflowSession()).thenReturn(mockSession);
        when(mockSession.getNodeByIdentifier(NODE_ID)).thenReturn(mockDocument);
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
