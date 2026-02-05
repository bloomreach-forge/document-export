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

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ExportJsonUtils {

    private ExportJsonUtils() {
    }

    private static Logger log = LoggerFactory.getLogger(ExportJsonUtils.class);

    /**
     * Extract the handle node from a document node (or return it if it's already a handle)
     *
     * @param node a document node or handle node
     * @return the handle node, or null if extraction fails
     */
    public static Node extractHandle(final Node node) throws RepositoryException {
        if (node == null) {
            return null;
        }
        if (node.isNodeType(ExportJsonConstants.HIPPO_HANDLE)) {
            return node;
        } else {
            final Node parent = node.getParent();
            if (parent == null) {
                return null;
            }
            if (parent.isNodeType(ExportJsonConstants.HIPPO_HANDLE)) {
                return parent;
            }
        }
        return null;
    }
}
