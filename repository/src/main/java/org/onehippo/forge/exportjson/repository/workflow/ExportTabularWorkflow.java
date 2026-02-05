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

public interface ExportTabularWorkflow extends Workflow, IWorkflowInvoker {

    /**
     * Exports a document as CSV or TSV format.
     *
     * @param subjectId the document identifier
     * @param format "csv" or "tsv"
     * @param includeHeaders whether to include header row
     * @param quoteAll whether to quote all values
     * @return the formatted document as a string
     * @throws RepositoryException if unable to access the document
     * @throws WorkflowException if workflow execution fails
     * @throws RemoteException if a remote error occurs
     */
    String exportTabularDocument(String subjectId, String format, boolean includeHeaders, boolean quoteAll)
        throws RepositoryException, WorkflowException, RemoteException;

}
