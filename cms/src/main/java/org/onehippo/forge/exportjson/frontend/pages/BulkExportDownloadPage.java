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
package org.onehippo.forge.exportjson.frontend.pages;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.jcr.Session;

import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.request.handler.resource.ResourceStreamRequestHandler;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.ContentDisposition;
import org.apache.wicket.util.resource.AbstractResourceStream;
import org.apache.wicket.util.resource.ResourceStreamNotFoundException;
import org.apache.wicket.util.resource.StringResourceStream;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.WorkflowManager;
import org.onehippo.forge.exportjson.frontend.plugins.BulkExportSearchPlugin;
import org.onehippo.forge.exportjson.repository.bulk.BulkExportOptions;
import org.onehippo.forge.exportjson.repository.bulk.BulkExportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Streams a bulk export file to the browser. Reads node IDs from the Wicket session
 * (stored by {@link BulkExportSearchPlugin}) and delegates to {@link BulkExportService}.
 *
 * <p>Uses {@link ResourceStreamRequestHandler} scheduled via
 * {@code getRequestCycle().scheduleRequestHandlerAfterCurrent()} so that Wicket never
 * attempts to render this page as HTML after the binary stream is written.
 *
 * <p>URL parameters: {@code exportId}, {@code format} (csv/tsv/json/xml/html/pdf),
 * {@code pageSize}, {@code orientation}, {@code fontSize},
 * {@code includeHeaders}, {@code quoteAll}, {@code includeMetadata}.
 */
public class BulkExportDownloadPage extends WebPage {

    private static final Logger log = LoggerFactory.getLogger(BulkExportDownloadPage.class);

    @SuppressWarnings("unchecked")
    public BulkExportDownloadPage(final PageParameters parameters) {
        super(parameters);

        final String exportId = parameters.get("exportId").toString();
        if (exportId == null || exportId.isEmpty()) {
            log.warn("BulkExportDownloadPage: missing exportId parameter");
            return;
        }

        final org.apache.wicket.Session wicketSession = org.apache.wicket.Session.get();
        final String sessionKey = BulkExportSearchPlugin.SESSION_IDS_PREFIX + exportId;
        final List<String> nodeIds = (List<String>) wicketSession.getAttribute(sessionKey);

        if (nodeIds == null || nodeIds.isEmpty()) {
            log.warn("BulkExportDownloadPage: no node IDs in session for exportId={}", exportId);
            return;
        }

        final String format = parameters.get("format").toString("csv");
        final BulkExportOptions options = new BulkExportOptions(
                parameters.get("pageSize").toString("A4"),
                parameters.get("orientation").toString("portrait"),
                parameters.get("fontSize").toString("medium"),
                parameters.get("includeHeaders").toBoolean(true),
                parameters.get("quoteAll").toBoolean(false),
                parameters.get("includeMetadata").toBoolean(true));

        try {
            final Session jcrSession = ((UserSession) wicketSession).getJcrSession();
            final WorkflowManager workflowManager =
                    ((HippoWorkspace) jcrSession.getWorkspace()).getWorkflowManager();
            final BulkExportService service = new BulkExportService(workflowManager, jcrSession);

            final ResourceStreamRequestHandler handler;
            switch (format) {
                case "csv" -> {
                    handler = new ResourceStreamRequestHandler(
                            new StringResourceStream(service.exportAsCsv(nodeIds, options), "text/csv"),
                            "bulk-export.csv");
                }
                case "tsv" -> {
                    handler = new ResourceStreamRequestHandler(
                            new StringResourceStream(service.exportAsTsv(nodeIds, options),
                                    "text/tab-separated-values"),
                            "bulk-export.tsv");
                }
                case "json", "xml", "html" -> {
                    handler = new ResourceStreamRequestHandler(
                            tempFileZipStream(service, nodeIds, format, options),
                            "bulk-export-" + format + ".zip");
                }
                case "pdf" -> {
                    final List<String> pdfIds = nodeIds.size() > BulkExportService.MAX_PDF_DOCUMENTS
                            ? nodeIds.subList(0, BulkExportService.MAX_PDF_DOCUMENTS)
                            : nodeIds;
                    final byte[] pdfBytes = service.exportAsMergedPdf(pdfIds, options);
                    handler = new ResourceStreamRequestHandler(
                            bytesStream(pdfBytes, "application/pdf"),
                            "bulk-export.pdf");
                }
                default -> {
                    log.warn("Unknown bulk export format: {}", format);
                    return;
                }
            }

            handler.setContentDisposition(ContentDisposition.ATTACHMENT);
            getRequestCycle().scheduleRequestHandlerAfterCurrent(handler);

            wicketSession.removeAttribute(sessionKey);
            log.info("Bulk export completed: format={}, documents={}", format, nodeIds.size());

        } catch (Exception e) {
            log.error("Error in bulk export download for exportId={}: {}", exportId, e.getMessage(), e);
        }
    }

    private static AbstractResourceStream bytesStream(final byte[] data, final String contentType) {
        return new AbstractResourceStream() {
            @Override
            public InputStream getInputStream() throws ResourceStreamNotFoundException {
                return new ByteArrayInputStream(data);
            }
            @Override
            public String getContentType() {
                return contentType;
            }
            @Override
            public void close() {
            }
        };
    }

    /**
     * Writes ZIP content to a temp file first, then streams from that file.
     * Peak heap = largest single document's bytes rather than the full archive.
     * The temp file is deleted when the stream is closed by Wicket after sending.
     */
    private static AbstractResourceStream tempFileZipStream(
            final BulkExportService service,
            final List<String> nodeIds,
            final String format,
            final BulkExportOptions options) throws Exception {
        final File tempFile = File.createTempFile("bulk-export-", ".zip");
        try (final FileOutputStream fos = new FileOutputStream(tempFile)) {
            service.exportAsZipToStream(nodeIds, format, options, fos);
        }
        return new AbstractResourceStream() {
            private InputStream stream;

            @Override
            public InputStream getInputStream() throws ResourceStreamNotFoundException {
                try {
                    stream = new FileInputStream(tempFile);
                    return stream;
                } catch (IOException e) {
                    throw new ResourceStreamNotFoundException(e);
                }
            }

            @Override
            public String getContentType() {
                return "application/zip";
            }

            @Override
            public void close() throws IOException {
                if (stream != null) {
                    stream.close();
                }
                tempFile.delete();
            }
        };
    }
}
