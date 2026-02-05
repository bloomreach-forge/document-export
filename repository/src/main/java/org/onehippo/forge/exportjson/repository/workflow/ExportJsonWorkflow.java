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

import javax.jcr.RepositoryException;

import org.hippoecm.addon.workflow.IWorkflowInvoker;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowException;

import com.fasterxml.jackson.core.JsonProcessingException;

/**
 * Workflow interface for exporting documents as JSON
 */
public interface ExportJsonWorkflow extends Workflow, IWorkflowInvoker {

    /**
     * Exports the document as JSON with full metadata
     *
     * @param subjectId the document ID to export
     * @return JSON string of the document
     * @throws WorkflowException
     * @throws RepositoryException
     * @throws RemoteException
     * @throws JsonProcessingException
     */
    String exportJsonDocument(String subjectId) throws WorkflowException, RepositoryException, RemoteException, JsonProcessingException;

}
