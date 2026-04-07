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
package org.onehippo.forge.exportjson.repository.bulk;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.jcr.Node;
import javax.jcr.Session;

import org.apache.pdfbox.io.MemoryUsageSetting;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.hippoecm.repository.api.WorkflowManager;
import org.onehippo.forge.exportjson.repository.workflow.ExportHtmlWorkflow;
import org.onehippo.forge.exportjson.repository.workflow.ExportJsonWorkflow;
import org.onehippo.forge.exportjson.repository.workflow.ExportPdfWorkflow;
import org.onehippo.forge.exportjson.repository.workflow.ExportTabularWorkflow;
import org.onehippo.forge.exportjson.repository.workflow.ExportXmlWorkflow;

/**
 * Orchestrates bulk export of multiple documents from Advanced Search results.
 * Maximum of {@value #MAX_DOCUMENTS} documents per export.
 */
public class BulkExportService {

    public static final int MAX_DOCUMENTS = 500;
    /** PDF merge holds all source PDFs in heap simultaneously; keep this conservative. */
    public static final int MAX_PDF_DOCUMENTS = 50;

    private static final String CSV_HEADER = "Document,Property Name,Property Value,Property Type";
    private static final String TSV_HEADER = "Document\tProperty Name\tProperty Value\tProperty Type";

    private final WorkflowManager workflowManager;
    private final Session session;

    public BulkExportService(final WorkflowManager workflowManager, final Session session) {
        this.workflowManager = workflowManager;
        this.session = session;
    }

    public String exportAsCsv(final List<String> nodeIds, final BulkExportOptions options) throws Exception {
        validateCap(nodeIds);
        final StringBuilder out = new StringBuilder(CSV_HEADER).append('\n');
        for (final String nodeId : nodeIds) {
            final Node node = session.getNodeByIdentifier(nodeId);
            final String nodeName = node.getName();
            final ExportTabularWorkflow wf =
                    (ExportTabularWorkflow) workflowManager.getWorkflow("exportcsv", node);
            final String rows = wf.exportTabularDocument(nodeId, "csv", false, options.quoteAll());
            appendRowsWithDocName(out, nodeName, rows, ",");
        }
        return out.toString();
    }

    public String exportAsTsv(final List<String> nodeIds, final BulkExportOptions options) throws Exception {
        validateCap(nodeIds);
        final StringBuilder out = new StringBuilder(TSV_HEADER).append('\n');
        for (final String nodeId : nodeIds) {
            final Node node = session.getNodeByIdentifier(nodeId);
            final String nodeName = node.getName();
            final ExportTabularWorkflow wf =
                    (ExportTabularWorkflow) workflowManager.getWorkflow("exporttsv", node);
            final String rows = wf.exportTabularDocument(nodeId, "tsv", false, options.quoteAll());
            appendRowsWithDocName(out, nodeName, rows, "\t");
        }
        return out.toString();
    }

    public byte[] exportAsZip(final List<String> nodeIds, final String format,
                              final BulkExportOptions options) throws Exception {
        validateCap(nodeIds);
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (final ZipOutputStream zos = new ZipOutputStream(baos)) {
            for (final String nodeId : nodeIds) {
                final Node node = session.getNodeByIdentifier(nodeId);
                final String nodeName = node.getName();
                final byte[] content = resolveZipEntryContent(nodeId, node, format, options);
                final ZipEntry entry = new ZipEntry(nodeName + "." + format);
                zos.putNextEntry(entry);
                zos.write(content);
                zos.closeEntry();
            }
        }
        return baos.toByteArray();
    }

    /**
     * Writes the ZIP export directly to {@code outputStream} without accumulating all bytes in heap.
     * Peak memory = largest single document's content bytes.
     */
    public void exportAsZipToStream(final List<String> nodeIds, final String format,
                                    final BulkExportOptions options,
                                    final OutputStream outputStream) throws Exception {
        validateCap(nodeIds);
        try (final ZipOutputStream zos = new ZipOutputStream(outputStream)) {
            for (final String nodeId : nodeIds) {
                final Node node = session.getNodeByIdentifier(nodeId);
                final String nodeName = node.getName();
                final byte[] content = resolveZipEntryContent(nodeId, node, format, options);
                final ZipEntry entry = new ZipEntry(nodeName + "." + format);
                zos.putNextEntry(entry);
                zos.write(content);
                zos.closeEntry();
            }
        }
    }

    public byte[] exportAsMergedPdf(final List<String> nodeIds, final BulkExportOptions options) throws Exception {
        validateCap(nodeIds);
        if (nodeIds.isEmpty()) {
            return new byte[0];
        }
        final PDFMergerUtility merger = new PDFMergerUtility();
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        merger.setDestinationStream(out);
        for (final String nodeId : nodeIds) {
            final Node node = session.getNodeByIdentifier(nodeId);
            final ExportPdfWorkflow wf =
                    (ExportPdfWorkflow) workflowManager.getWorkflow("exportpdf", node);
            final byte[] pdf = wf.exportPdfDocument(
                    nodeId, options.pageSize(), options.orientation(),
                    options.fontSize(), options.includeMetadata());
            merger.addSource(new ByteArrayInputStream(pdf));
        }
        merger.mergeDocuments(MemoryUsageSetting.setupMainMemoryOnly());
        return out.toByteArray();
    }

    // --- private helpers ---

    private void validateCap(final List<String> nodeIds) {
        if (nodeIds.size() > MAX_DOCUMENTS) {
            throw new IllegalArgumentException(
                    "Cannot export more than " + MAX_DOCUMENTS + " documents at once; got " + nodeIds.size());
        }
    }

    private void appendRowsWithDocName(final StringBuilder out, final String docName,
                                       final String rows, final String delimiter) {
        for (final String line : rows.split("\n")) {
            if (!line.isBlank()) {
                out.append(docName).append(delimiter).append(line).append('\n');
            }
        }
    }

    private byte[] resolveZipEntryContent(final String nodeId, final Node node,
                                          final String format,
                                          final BulkExportOptions options) throws Exception {
        return switch (format) {
            case "json" -> {
                final ExportJsonWorkflow wf =
                        (ExportJsonWorkflow) workflowManager.getWorkflow("exportjson", node);
                yield wf.exportJsonDocument(nodeId).getBytes(StandardCharsets.UTF_8);
            }
            case "csv" -> {
                final ExportTabularWorkflow wf =
                        (ExportTabularWorkflow) workflowManager.getWorkflow("exportcsv", node);
                yield wf.exportTabularDocument(nodeId, "csv", options.includeHeaders(), options.quoteAll())
                        .getBytes(StandardCharsets.UTF_8);
            }
            case "tsv" -> {
                final ExportTabularWorkflow wf =
                        (ExportTabularWorkflow) workflowManager.getWorkflow("exporttsv", node);
                yield wf.exportTabularDocument(nodeId, "tsv", options.includeHeaders(), options.quoteAll())
                        .getBytes(StandardCharsets.UTF_8);
            }
            case "xml" -> {
                final ExportXmlWorkflow wf =
                        (ExportXmlWorkflow) workflowManager.getWorkflow("exportxml", node);
                yield wf.exportXmlDocument(nodeId, true, true, options.includeMetadata())
                        .getBytes(StandardCharsets.UTF_8);
            }
            case "html" -> {
                final ExportHtmlWorkflow wf =
                        (ExportHtmlWorkflow) workflowManager.getWorkflow("exporthtml", node);
                yield wf.exportHtmlDocument(nodeId, options.includeMetadata(), true)
                        .getBytes(StandardCharsets.UTF_8);
            }
            case "pdf" -> {
                final ExportPdfWorkflow wf =
                        (ExportPdfWorkflow) workflowManager.getWorkflow("exportpdf", node);
                yield wf.exportPdfDocument(nodeId, options.pageSize(), options.orientation(),
                        options.fontSize(), options.includeMetadata());
            }
            default -> throw new IllegalArgumentException("Unknown ZIP format: " + format);
        };
    }
}
