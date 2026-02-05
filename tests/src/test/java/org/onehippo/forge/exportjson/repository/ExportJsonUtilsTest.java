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
package org.onehippo.forge.exportjson.repository;

import static org.junit.Assert.*;
import static org.easymock.EasyMock.*;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for ExportJsonUtils utility methods
 */
public class ExportJsonUtilsTest {

    private Node mockNode;
    private Node mockParent;

    @Before
    public void setUp() {
        mockNode = createMock(Node.class);
        mockParent = createMock(Node.class);
    }

    @Test
    public void testExtractHandleWithNullNode() throws RepositoryException {
        Node result = ExportJsonUtils.extractHandle(null);
        assertNull("Should return null for null input", result);
    }

    @Test
    public void testExtractHandleWithHandleNode() throws RepositoryException {
        expect(mockNode.isNodeType("hippo:handle")).andReturn(true);
        replay(mockNode);

        Node result = ExportJsonUtils.extractHandle(mockNode);
        assertEquals("Should return the same node if it's already a handle", mockNode, result);
        verify(mockNode);
    }

    @Test
    public void testExtractHandleWithDocumentNodeWhoseParentIsHandle() throws RepositoryException {
        expect(mockNode.isNodeType("hippo:handle")).andReturn(false);
        expect(mockNode.getParent()).andReturn(mockParent);
        expect(mockParent.isNodeType("hippo:handle")).andReturn(true);
        replay(mockNode, mockParent);

        Node result = ExportJsonUtils.extractHandle(mockNode);
        assertEquals("Should return the parent node when parent is a handle", mockParent, result);
        verify(mockNode, mockParent);
    }

    @Test
    public void testExtractHandleWithNodeWhoseParentIsNotHandle() throws RepositoryException {
        expect(mockNode.isNodeType("hippo:handle")).andReturn(false);
        expect(mockNode.getParent()).andReturn(mockParent);
        expect(mockParent.isNodeType("hippo:handle")).andReturn(false);
        replay(mockNode, mockParent);

        Node result = ExportJsonUtils.extractHandle(mockNode);
        assertNull("Should return null when neither node nor parent is a handle", result);
        verify(mockNode, mockParent);
    }

    @Test
    public void testExtractHandleWithNodeHavingNullParent() throws RepositoryException {
        expect(mockNode.isNodeType("hippo:handle")).andReturn(false);
        expect(mockNode.getParent()).andReturn(null);
        replay(mockNode);

        Node result = ExportJsonUtils.extractHandle(mockNode);
        assertNull("Should return null when node has no parent", result);
        verify(mockNode);
    }

    @Test
    public void testExtractHandleHandlesRepositoryException() throws RepositoryException {
        expect(mockNode.isNodeType("hippo:handle")).andThrow(new RepositoryException("Test error"));
        replay(mockNode);

        assertThrows("Should propagate RepositoryException", RepositoryException.class, () -> {
            ExportJsonUtils.extractHandle(mockNode);
        });
        verify(mockNode);
    }

    @Test
    public void testExtractHandleHandlesRepositoryExceptionOnParentCheck() throws RepositoryException {
        expect(mockNode.isNodeType("hippo:handle")).andReturn(false);
        expect(mockNode.getParent()).andThrow(new RepositoryException("Parent access failed"));
        replay(mockNode);

        assertThrows("Should propagate RepositoryException from getParent()", RepositoryException.class, () -> {
            ExportJsonUtils.extractHandle(mockNode);
        });
        verify(mockNode);
    }
}
