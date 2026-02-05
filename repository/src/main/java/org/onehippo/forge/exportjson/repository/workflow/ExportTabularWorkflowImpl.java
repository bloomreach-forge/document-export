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

import java.io.StringWriter;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.QuoteMode;
import org.hippoecm.repository.api.WorkflowContext;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.ext.WorkflowImpl;
import org.onehippo.forge.exportjson.repository.ExportTabularConstants;
import org.onehippo.forge.exportjson.repository.ExportTabularUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExportTabularWorkflowImpl extends WorkflowImpl implements ExportTabularWorkflow {

    private static final Logger log = LoggerFactory.getLogger(ExportTabularWorkflowImpl.class);

    public ExportTabularWorkflowImpl() throws RemoteException {
    }

    @Override
    public String exportTabularDocument(final String subjectId, final String format,
            final boolean includeHeaders, final boolean quoteAll)
            throws RepositoryException, WorkflowException, RemoteException {

        final WorkflowContext workflowContext = getWorkflowContext();
        final Session internalWorkflowSession = workflowContext.getInternalWorkflowSession();
        final Node documentNode = internalWorkflowSession.getNodeByIdentifier(subjectId);

        try {
            // Build rows of [Property Name, Property Value, Property Type]
            List<String[]> rows = new ArrayList<>();

            PropertyIterator properties = documentNode.getProperties();
            while (properties.hasNext()) {
                javax.jcr.Property property = properties.nextProperty();
                String propertyName = property.getName();

                try {
                    String propertyValue = ExportTabularUtils.propertyValueToString(property);
                    String propertyType = ExportTabularUtils.getPropertyTypeName(property.getType());

                    rows.add(new String[]{propertyName, propertyValue, propertyType});
                } catch (Exception e) {
                    log.warn("Could not serialize property {}: {}", propertyName, e.getMessage());
                    rows.add(new String[]{propertyName, "[Unable to serialize]", "Unknown"});
                }
            }

            // Format as CSV or TSV
            return formatAsTabular(rows, format, includeHeaders, quoteAll);

        } catch (Exception e) {
            log.error("Error exporting tabular document: {}", e.getMessage(), e);
            throw new WorkflowException("Failed to export document as " + format.toUpperCase(), e);
        }
    }

    /**
     * Formats the rows as CSV or TSV.
     */
    private String formatAsTabular(List<String[]> rows, String format,
            boolean includeHeaders, boolean quoteAll) throws Exception {

        CSVFormat csvFormat;
        if (ExportTabularConstants.FORMAT_TSV.equals(format)) {
            csvFormat = CSVFormat.TDF;
        } else {
            csvFormat = CSVFormat.DEFAULT.withQuote('"');
        }

        if (quoteAll) {
            csvFormat = csvFormat.withQuoteMode(QuoteMode.ALL);
        } else {
            csvFormat = csvFormat.withQuoteMode(QuoteMode.MINIMAL);
        }

        StringWriter writer = new StringWriter();
        try (CSVPrinter printer = new CSVPrinter(writer, csvFormat)) {
            if (includeHeaders) {
                printer.printRecord(
                    ExportTabularConstants.COLUMN_PROPERTY_NAME,
                    ExportTabularConstants.COLUMN_PROPERTY_VALUE,
                    ExportTabularConstants.COLUMN_PROPERTY_TYPE
                );
            }

            for (String[] row : rows) {
                printer.printRecord((Object[]) row);
            }

            printer.flush();
        }

        return writer.toString();
    }

    /**
     * This method is required by the WorkflowImpl interface but is not used in this implementation.
     * The actual workflow execution is handled by the {@link #exportTabularDocument(String, String, boolean, boolean)} method.
     */
    @Override
    public void invokeWorkflow() throws Exception {
        // No-op: workflow logic is handled by exportTabularDocument()
    }

}
