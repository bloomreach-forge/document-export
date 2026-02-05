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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.apps.Fop;
import org.apache.fop.apps.FopFactory;
import org.hippoecm.repository.api.WorkflowContext;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.ext.WorkflowImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExportPdfWorkflowImpl extends WorkflowImpl implements ExportPdfWorkflow {

    private final static Logger log = LoggerFactory.getLogger(ExportPdfWorkflowImpl.class);

    public ExportPdfWorkflowImpl() throws RemoteException {
    }

    @Override
    public byte[] exportPdfDocument(final String subjectId, final String pageSize, final String orientation,
                                    final String fontSize, final boolean includeMetadata)
        throws RepositoryException, IOException {

        final WorkflowContext workflowContext = getWorkflowContext();
        final Session internalWorkflowSession = workflowContext.getInternalWorkflowSession();
        final Node documentNode = internalWorkflowSession.getNodeByIdentifier(subjectId);

        // Build property list
        List<String> propertyLines = buildPropertyList(documentNode, includeMetadata);

        // Generate XSL-FO content
        String xslFo = generateXslFo(documentNode, propertyLines, pageSize, orientation, fontSize);

        // Convert XSL-FO to PDF using FOP
        return convertFoToPdf(xslFo);
    }

    private List<String> buildPropertyList(Node documentNode, boolean includeMetadata)
        throws RepositoryException {
        List<String> lines = new ArrayList<>();

        // Add header information
        lines.add("DOCUMENT: " + documentNode.getName());
        lines.add("PATH: " + documentNode.getPath());
        lines.add("TYPE: " + documentNode.getPrimaryNodeType().getName());
        lines.add("");
        lines.add("PROPERTIES:");
        lines.add("");

        // Add properties
        PropertyIterator properties = documentNode.getProperties();
        while (properties.hasNext()) {
            javax.jcr.Property property = properties.nextProperty();
            String propertyName = property.getName();

            try {
                String value;
                if (property.isMultiple()) {
                    Value[] values = property.getValues();
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < values.length; i++) {
                        if (i > 0) sb.append(", ");
                        sb.append(values[i].getString());
                    }
                    value = sb.toString();
                } else {
                    value = property.getValue().getString();
                }
                lines.add(propertyName + ": " + value);
            } catch (Exception e) {
                log.warn("Could not serialize property {}: {}", propertyName, e.getMessage());
                lines.add(propertyName + ": [Unable to serialize]");
            }
        }

        // Add metadata section if requested
        if (includeMetadata) {
            lines.add("");
            lines.add("METADATA:");
            lines.add("");

            if (documentNode.hasProperty("jcr:created")) {
                String created = documentNode.getProperty("jcr:created").getValue().getString();
                lines.add("Created: " + created);
            }

            if (documentNode.hasProperty("jcr:lastModified")) {
                String modified = documentNode.getProperty("jcr:lastModified").getValue().getString();
                lines.add("Last Modified: " + modified);
            }

            if (documentNode.hasProperty("hippo:author")) {
                String author = documentNode.getProperty("hippo:author").getValue().getString();
                lines.add("Author: " + author);
            }
        }

        return lines;
    }

    private String generateXslFo(Node documentNode, List<String> lines, String pageSize,
                                 String orientation, String fontSize) throws RepositoryException {
        StringBuilder fo = new StringBuilder();

        // Determine page dimensions
        String pageHeight = "297mm";  // A4 height
        String pageWidth = "210mm";   // A4 width

        if ("LETTER".equalsIgnoreCase(pageSize)) {
            pageHeight = "279mm";
            pageWidth = "216mm";
        } else if ("LEGAL".equalsIgnoreCase(pageSize)) {
            pageHeight = "356mm";
            pageWidth = "216mm";
        }

        // Swap dimensions for landscape
        if ("landscape".equalsIgnoreCase(orientation)) {
            String temp = pageHeight;
            pageHeight = pageWidth;
            pageWidth = temp;
        }

        // Determine font size
        String fontSizePt = "11pt";
        if ("small".equalsIgnoreCase(fontSize)) {
            fontSizePt = "9pt";
        } else if ("large".equalsIgnoreCase(fontSize)) {
            fontSizePt = "13pt";
        }

        // Build XSL-FO document
        fo.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        fo.append("<fo:root xmlns:fo=\"http://www.w3.org/1999/XSL/Format\">\n");
        fo.append("  <fo:layout-master-set>\n");
        fo.append("    <fo:simple-page-master master-name=\"A4\" page-height=\"").append(pageHeight)
          .append("\" page-width=\"").append(pageWidth).append("\">\n");
        fo.append("      <fo:region-body margin=\"18mm\"/>\n");
        fo.append("    </fo:simple-page-master>\n");
        fo.append("  </fo:layout-master-set>\n");
        fo.append("  <fo:page-sequence master-reference=\"A4\">\n");
        fo.append("    <fo:flow flow-name=\"xsl-region-body\">\n");

        // Add content
        fo.append("      <fo:block font-size=\"").append(fontSizePt).append("\" font-family=\"monospace\">\n");

        for (String line : lines) {
            String escaped = escapeXml(line);
            fo.append("        <fo:block>").append(escaped).append("</fo:block>\n");
        }

        fo.append("      </fo:block>\n");
        fo.append("    </fo:flow>\n");
        fo.append("  </fo:page-sequence>\n");
        fo.append("</fo:root>\n");

        return fo.toString();
    }

    private byte[] convertFoToPdf(String xslFo) throws IOException {
        try {
            FopFactory fopFactory = FopFactory.newInstance(new java.io.File(".").toURI());
            FOUserAgent userAgent = fopFactory.newFOUserAgent();

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Fop fop = fopFactory.newFop("application/pdf", userAgent, out);

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();

            Source source = new StreamSource(new java.io.StringReader(xslFo));
            Result result = new SAXResult(fop.getDefaultHandler());

            transformer.transform(source, result);

            return out.toByteArray();

        } catch (Exception e) {
            log.error("Error converting XSL-FO to PDF: {}", e.getMessage(), e);
            throw new IOException("Failed to generate PDF", e);
        }
    }

    private String escapeXml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&apos;");
    }

    /**
     * This method is required by the WorkflowImpl interface but is not used in this implementation.
     * The actual workflow execution is handled by the {@link #exportPdfDocument(String, String, String, String, boolean)} method.
     */
    @Override
    public void invokeWorkflow() throws Exception {
        // No-op: workflow logic is handled by exportPdfDocument()
    }
}
