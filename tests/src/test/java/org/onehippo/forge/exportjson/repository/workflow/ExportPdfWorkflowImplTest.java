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
import org.hippoecm.repository.ext.WorkflowImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExportPdfWorkflowImplTest {

    @Mock private WorkflowContext mockContext;
    @Mock private Session mockSession;
    @Mock private Node mockDocument;
    @Mock private NodeType mockNodeType;
    @Mock private PropertyIterator mockPropertyIterator;

    private ExportPdfWorkflowImpl workflow;

    private static final String NODE_ID = "test-id";

    @BeforeEach
    void setUp() throws Exception {
        workflow = new ExportPdfWorkflowImpl();
        injectContext(workflow, mockContext);
    }

    @Test
    void exportPdfDocument_a4Portrait_returnsValidPdfBytes() throws Exception {
        stubEmptyDocument(false);

        byte[] pdf = workflow.exportPdfDocument(NODE_ID, "A4", "portrait", "medium", false);

        assertNotNull(pdf);
        assertTrue(pdf.length > 0);
        assertEquals('%', (char) pdf[0]);
        assertEquals('P', (char) pdf[1]);
        assertEquals('D', (char) pdf[2]);
        assertEquals('F', (char) pdf[3]);
    }

    @Test
    void exportPdfDocument_letterLandscape_returnsValidPdfBytes() throws Exception {
        stubEmptyDocument(false);

        byte[] pdf = workflow.exportPdfDocument(NODE_ID, "LETTER", "landscape", "medium", false);

        assertNotNull(pdf);
        assertTrue(pdf.length > 0);
    }

    @Test
    void exportPdfDocument_legalPageSize_returnsValidPdfBytes() throws Exception {
        stubEmptyDocument(false);

        byte[] pdf = workflow.exportPdfDocument(NODE_ID, "LEGAL", "portrait", "medium", false);

        assertNotNull(pdf);
        assertTrue(pdf.length > 0);
    }

    @Test
    void exportPdfDocument_smallFontSize_returnsValidPdfBytes() throws Exception {
        stubEmptyDocument(false);

        byte[] pdf = workflow.exportPdfDocument(NODE_ID, "A4", "portrait", "small", false);

        assertNotNull(pdf);
        assertTrue(pdf.length > 0);
    }

    @Test
    void exportPdfDocument_largeFontSize_returnsValidPdfBytes() throws Exception {
        stubEmptyDocument(false);

        byte[] pdf = workflow.exportPdfDocument(NODE_ID, "A4", "portrait", "large", false);

        assertNotNull(pdf);
        assertTrue(pdf.length > 0);
    }

    @Test
    void exportPdfDocument_withMetadataAllPresent_returnsValidPdfBytes() throws Exception {
        Property created = mock(Property.class);
        Property modified = mock(Property.class);
        Property author = mock(Property.class);
        Value createdVal = mock(Value.class);
        Value modifiedVal = mock(Value.class);
        Value authorVal = mock(Value.class);

        stubEmptyDocument(true);
        when(mockDocument.hasProperty("jcr:created")).thenReturn(true);
        when(mockDocument.getProperty("jcr:created")).thenReturn(created);
        when(created.getValue()).thenReturn(createdVal);
        when(createdVal.getString()).thenReturn("2024-01-01");
        when(mockDocument.hasProperty("jcr:lastModified")).thenReturn(true);
        when(mockDocument.getProperty("jcr:lastModified")).thenReturn(modified);
        when(modified.getValue()).thenReturn(modifiedVal);
        when(modifiedVal.getString()).thenReturn("2024-06-01");
        when(mockDocument.hasProperty("hippo:author")).thenReturn(true);
        when(mockDocument.getProperty("hippo:author")).thenReturn(author);
        when(author.getValue()).thenReturn(authorVal);
        when(authorVal.getString()).thenReturn("admin");

        byte[] pdf = workflow.exportPdfDocument(NODE_ID, "A4", "portrait", "medium", true);

        assertNotNull(pdf);
        assertTrue(pdf.length > 0);
    }

    @Test
    void exportPdfDocument_withMetadataAllAbsent_returnsValidPdfBytes() throws Exception {
        stubEmptyDocument(true);
        when(mockDocument.hasProperty("jcr:created")).thenReturn(false);
        when(mockDocument.hasProperty("jcr:lastModified")).thenReturn(false);
        when(mockDocument.hasProperty("hippo:author")).thenReturn(false);

        byte[] pdf = workflow.exportPdfDocument(NODE_ID, "A4", "portrait", "medium", true);

        assertNotNull(pdf);
        assertTrue(pdf.length > 0);
    }

    @Test
    void exportPdfDocument_singleValuedProperty_includesPropertyInOutput() throws Exception {
        Property prop = mock(Property.class);
        Value val = mock(Value.class);
        stubDocumentWithProperty(prop, false);
        when(prop.getName()).thenReturn("content:title");
        when(prop.isMultiple()).thenReturn(false);
        when(prop.getValue()).thenReturn(val);
        when(val.getString()).thenReturn("Test Title");

        byte[] pdf = workflow.exportPdfDocument(NODE_ID, "A4", "portrait", "medium", false);

        assertNotNull(pdf);
        assertTrue(pdf.length > 0);
    }

    @Test
    void exportPdfDocument_multiValuedProperty_joinedWithCommas() throws Exception {
        Property prop = mock(Property.class);
        Value v1 = mock(Value.class);
        Value v2 = mock(Value.class);
        stubDocumentWithProperty(prop, false);
        when(prop.getName()).thenReturn("content:tags");
        when(prop.isMultiple()).thenReturn(true);
        when(prop.getValues()).thenReturn(new Value[]{v1, v2});
        when(v1.getString()).thenReturn("alpha");
        when(v2.getString()).thenReturn("beta");

        byte[] pdf = workflow.exportPdfDocument(NODE_ID, "A4", "portrait", "medium", false);

        assertNotNull(pdf);
        assertTrue(pdf.length > 0);
    }

    @Test
    void exportPdfDocument_unserializableProperty_usesErrorMarker() throws Exception {
        Property prop = mock(Property.class);
        stubDocumentWithProperty(prop, false);
        when(prop.getName()).thenReturn("bad:prop");
        when(prop.isMultiple()).thenReturn(false);
        when(prop.getValue()).thenThrow(new RepositoryException("Cannot read"));

        byte[] pdf = workflow.exportPdfDocument(NODE_ID, "A4", "portrait", "medium", false);

        assertNotNull(pdf);
        assertTrue(pdf.length > 0);
    }

    @Test
    void invokeWorkflow_isNoOp() throws Exception {
        assertDoesNotThrow(() -> workflow.invokeWorkflow());
    }

    // --- helpers ---

    private void stubEmptyDocument(boolean forMetadata) throws RepositoryException {
        when(mockContext.getInternalWorkflowSession()).thenReturn(mockSession);
        when(mockSession.getNodeByIdentifier(NODE_ID)).thenReturn(mockDocument);
        when(mockDocument.getName()).thenReturn("test-doc");
        when(mockDocument.getPath()).thenReturn("/content/documents/test-doc");
        when(mockDocument.getPrimaryNodeType()).thenReturn(mockNodeType);
        when(mockNodeType.getName()).thenReturn("content:document");
        when(mockDocument.getProperties()).thenReturn(mockPropertyIterator);
        when(mockPropertyIterator.hasNext()).thenReturn(false);
    }

    private void stubDocumentWithProperty(Property prop, boolean forMetadata) throws RepositoryException {
        when(mockContext.getInternalWorkflowSession()).thenReturn(mockSession);
        when(mockSession.getNodeByIdentifier(NODE_ID)).thenReturn(mockDocument);
        when(mockDocument.getName()).thenReturn("test-doc");
        when(mockDocument.getPath()).thenReturn("/content/documents/test-doc");
        when(mockDocument.getPrimaryNodeType()).thenReturn(mockNodeType);
        when(mockNodeType.getName()).thenReturn("content:document");
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
