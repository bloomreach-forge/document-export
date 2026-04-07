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
package org.onehippo.forge.exportjson.bulk;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.jcr.Node;
import javax.jcr.Session;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.hippoecm.repository.api.WorkflowManager;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.forge.exportjson.repository.bulk.BulkExportOptions;
import org.onehippo.forge.exportjson.repository.bulk.BulkExportService;
import org.onehippo.forge.exportjson.repository.workflow.ExportHtmlWorkflow;
import org.onehippo.forge.exportjson.repository.workflow.ExportJsonWorkflow;
import org.onehippo.forge.exportjson.repository.workflow.ExportPdfWorkflow;
import org.onehippo.forge.exportjson.repository.workflow.ExportTabularWorkflow;
import org.onehippo.forge.exportjson.repository.workflow.ExportXmlWorkflow;

public class BulkExportServiceTest {

    private WorkflowManager mockWorkflowManager;
    private Session mockSession;
    private Node mockNode1;
    private Node mockNode2;

    @Before
    public void setUp() {
        mockWorkflowManager = createMock(WorkflowManager.class);
        mockSession = createMock(Session.class);
        mockNode1 = createMock(Node.class);
        mockNode2 = createMock(Node.class);
    }

    // --- CSV ---

    @Test
    public void testExportAsCsvAddsDocumentColumnAndAggregatesRows() throws Exception {
        ExportTabularWorkflow mockCsvWf1 = createMock(ExportTabularWorkflow.class);
        ExportTabularWorkflow mockCsvWf2 = createMock(ExportTabularWorkflow.class);

        expect(mockSession.getNodeByIdentifier("id-1")).andReturn(mockNode1);
        expect(mockNode1.getName()).andReturn("article-one");
        expect(mockWorkflowManager.getWorkflow("exportcsv", mockNode1)).andReturn(mockCsvWf1);
        expect(mockCsvWf1.exportTabularDocument("id-1", "csv", false, false))
                .andReturn("jcr:primaryType,hippostd:article,String\ncontent:title,Hello,String\n");

        expect(mockSession.getNodeByIdentifier("id-2")).andReturn(mockNode2);
        expect(mockNode2.getName()).andReturn("article-two");
        expect(mockWorkflowManager.getWorkflow("exportcsv", mockNode2)).andReturn(mockCsvWf2);
        expect(mockCsvWf2.exportTabularDocument("id-2", "csv", false, false))
                .andReturn("jcr:primaryType,hippostd:article,String\n");

        replay(mockWorkflowManager, mockSession, mockNode1, mockNode2, mockCsvWf1, mockCsvWf2);

        BulkExportService service = new BulkExportService(mockWorkflowManager, mockSession);
        String csv = service.exportAsCsv(List.of("id-1", "id-2"), BulkExportOptions.defaults());

        String[] lines = csv.split("\n");
        assertEquals("Header + 2 rows from doc1 + 1 row from doc2 = 4 lines", 4, lines.length);
        assertTrue("Header starts with Document column", lines[0].startsWith("Document,"));
        assertTrue("First data row has article-one as document name", lines[1].startsWith("article-one,"));
        assertTrue("Third data row has article-two as document name", lines[3].startsWith("article-two,"));

        verify(mockWorkflowManager, mockSession, mockNode1, mockNode2, mockCsvWf1, mockCsvWf2);
    }

    @Test
    public void testExportAsCsvEmptyListReturnsHeaderOnly() throws Exception {
        replay(mockWorkflowManager, mockSession);

        BulkExportService service = new BulkExportService(mockWorkflowManager, mockSession);
        String csv = service.exportAsCsv(Collections.emptyList(), BulkExportOptions.defaults());

        assertTrue("Should contain the Document header", csv.contains("Document"));
        verify(mockWorkflowManager, mockSession);
    }

    // --- TSV ---

    @Test
    public void testExportAsTsvUsesTabDelimiter() throws Exception {
        ExportTabularWorkflow mockTsvWf = createMock(ExportTabularWorkflow.class);

        expect(mockSession.getNodeByIdentifier("id-1")).andReturn(mockNode1);
        expect(mockNode1.getName()).andReturn("my-doc");
        expect(mockWorkflowManager.getWorkflow("exporttsv", mockNode1)).andReturn(mockTsvWf);
        expect(mockTsvWf.exportTabularDocument("id-1", "tsv", false, false))
                .andReturn("jcr:primaryType\thippostd:article\tString\n");

        replay(mockWorkflowManager, mockSession, mockNode1, mockTsvWf);

        BulkExportService service = new BulkExportService(mockWorkflowManager, mockSession);
        String tsv = service.exportAsTsv(List.of("id-1"), BulkExportOptions.defaults());

        assertTrue("Header should be tab-separated", tsv.contains("\t"));
        assertTrue("Data row should contain document name", tsv.contains("my-doc"));
        verify(mockWorkflowManager, mockSession, mockNode1, mockTsvWf);
    }

    // --- ZIP ---

    @Test
    public void testExportAsZipContainsOneEntryPerDocument() throws Exception {
        ExportJsonWorkflow mockJsonWf1 = createMock(ExportJsonWorkflow.class);
        ExportJsonWorkflow mockJsonWf2 = createMock(ExportJsonWorkflow.class);

        expect(mockSession.getNodeByIdentifier("id-1")).andReturn(mockNode1);
        expect(mockNode1.getName()).andReturn("doc-one");
        expect(mockWorkflowManager.getWorkflow("exportjson", mockNode1)).andReturn(mockJsonWf1);
        expect(mockJsonWf1.exportJsonDocument("id-1")).andReturn("{\"doc\":\"one\"}");

        expect(mockSession.getNodeByIdentifier("id-2")).andReturn(mockNode2);
        expect(mockNode2.getName()).andReturn("doc-two");
        expect(mockWorkflowManager.getWorkflow("exportjson", mockNode2)).andReturn(mockJsonWf2);
        expect(mockJsonWf2.exportJsonDocument("id-2")).andReturn("{\"doc\":\"two\"}");

        replay(mockWorkflowManager, mockSession, mockNode1, mockNode2, mockJsonWf1, mockJsonWf2);

        BulkExportService service = new BulkExportService(mockWorkflowManager, mockSession);
        byte[] zip = service.exportAsZip(List.of("id-1", "id-2"), "json", BulkExportOptions.defaults());

        List<String> entryNames = readZipEntryNames(zip);
        assertEquals("ZIP should have 2 entries", 2, entryNames.size());
        assertTrue("First entry is doc-one.json", entryNames.contains("doc-one.json"));
        assertTrue("Second entry is doc-two.json", entryNames.contains("doc-two.json"));

        verify(mockWorkflowManager, mockSession, mockNode1, mockNode2, mockJsonWf1, mockJsonWf2);
    }

    @Test
    public void testExportAsZipEmptyListReturnsEmptyZip() throws Exception {
        replay(mockWorkflowManager, mockSession);

        BulkExportService service = new BulkExportService(mockWorkflowManager, mockSession);
        byte[] zip = service.exportAsZip(Collections.emptyList(), "json", BulkExportOptions.defaults());

        assertNotNull("ZIP bytes should not be null", zip);
        assertTrue("ZIP should be non-empty (valid zip structure)", zip.length > 0);
        assertEquals("Empty ZIP should have no entries", 0, readZipEntryNames(zip).size());

        verify(mockWorkflowManager, mockSession);
    }

    // --- PDF Merge ---

    @Test
    public void testExportAsMergedPdfProducesValidPdf() throws Exception {
        ExportPdfWorkflow mockPdfWf1 = createMock(ExportPdfWorkflow.class);
        ExportPdfWorkflow mockPdfWf2 = createMock(ExportPdfWorkflow.class);

        byte[] pdf1 = createMinimalPdf();
        byte[] pdf2 = createMinimalPdf();

        expect(mockSession.getNodeByIdentifier("id-1")).andReturn(mockNode1);
        expect(mockWorkflowManager.getWorkflow("exportpdf", mockNode1)).andReturn(mockPdfWf1);
        expect(mockPdfWf1.exportPdfDocument("id-1", "A4", "portrait", "medium", true)).andReturn(pdf1);

        expect(mockSession.getNodeByIdentifier("id-2")).andReturn(mockNode2);
        expect(mockWorkflowManager.getWorkflow("exportpdf", mockNode2)).andReturn(mockPdfWf2);
        expect(mockPdfWf2.exportPdfDocument("id-2", "A4", "portrait", "medium", true)).andReturn(pdf2);

        replay(mockWorkflowManager, mockSession, mockNode1, mockNode2, mockPdfWf1, mockPdfWf2);

        BulkExportService service = new BulkExportService(mockWorkflowManager, mockSession);
        byte[] merged = service.exportAsMergedPdf(List.of("id-1", "id-2"), BulkExportOptions.defaults());

        assertTrue("Merged PDF should be non-empty", merged.length > 0);
        assertTrue("Result should start with PDF header", new String(merged, 0, 4).equals("%PDF"));

        verify(mockWorkflowManager, mockSession, mockNode1, mockNode2, mockPdfWf1, mockPdfWf2);
    }

    @Test
    public void testExportAsMergedPdfEmptyListReturnsEmptyBytes() throws Exception {
        replay(mockWorkflowManager, mockSession);

        BulkExportService service = new BulkExportService(mockWorkflowManager, mockSession);
        byte[] result = service.exportAsMergedPdf(Collections.emptyList(), BulkExportOptions.defaults());

        assertNotNull(result);
        assertEquals("Empty input should produce empty bytes", 0, result.length);

        verify(mockWorkflowManager, mockSession);
    }

    // --- Document cap ---

    @Test(expected = IllegalArgumentException.class)
    public void testExportAsCsvThrowsWhenExceedingCap() throws Exception {
        List<String> tooMany = new ArrayList<>();
        for (int i = 0; i < 501; i++) {
            tooMany.add("id-" + i);
        }
        replay(mockWorkflowManager, mockSession);

        new BulkExportService(mockWorkflowManager, mockSession)
                .exportAsCsv(tooMany, BulkExportOptions.defaults());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testExportAsZipThrowsWhenExceedingCap() throws Exception {
        List<String> tooMany = new ArrayList<>();
        for (int i = 0; i < 501; i++) {
            tooMany.add("id-" + i);
        }
        replay(mockWorkflowManager, mockSession);

        new BulkExportService(mockWorkflowManager, mockSession)
                .exportAsZip(tooMany, "json", BulkExportOptions.defaults());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testExportAsMergedPdfThrowsWhenExceedingCap() throws Exception {
        List<String> tooMany = new ArrayList<>();
        for (int i = 0; i < 501; i++) {
            tooMany.add("id-" + i);
        }
        replay(mockWorkflowManager, mockSession);

        new BulkExportService(mockWorkflowManager, mockSession)
                .exportAsMergedPdf(tooMany, BulkExportOptions.defaults());
    }

    // --- exportAsZipToStream ---

    @Test
    public void testExportAsZipToStreamProducesSameContentAsExportAsZip() throws Exception {
        // Use JSON format; the streaming and byte[] paths share resolveZipEntryContent.
        ExportJsonWorkflow mockJsonWf1 = createMock(ExportJsonWorkflow.class);
        ExportJsonWorkflow mockJsonWf2 = createMock(ExportJsonWorkflow.class);

        expect(mockSession.getNodeByIdentifier("id-1")).andReturn(mockNode1).times(2);
        expect(mockNode1.getName()).andReturn("doc-one").times(2);
        expect(mockWorkflowManager.getWorkflow("exportjson", mockNode1)).andReturn(mockJsonWf1).times(2);
        expect(mockJsonWf1.exportJsonDocument("id-1")).andReturn("{\"a\":1}").times(2);

        expect(mockSession.getNodeByIdentifier("id-2")).andReturn(mockNode2).times(2);
        expect(mockNode2.getName()).andReturn("doc-two").times(2);
        expect(mockWorkflowManager.getWorkflow("exportjson", mockNode2)).andReturn(mockJsonWf2).times(2);
        expect(mockJsonWf2.exportJsonDocument("id-2")).andReturn("{\"b\":2}").times(2);

        replay(mockWorkflowManager, mockSession, mockNode1, mockNode2, mockJsonWf1, mockJsonWf2);

        BulkExportService service = new BulkExportService(mockWorkflowManager, mockSession);
        List<String> ids = List.of("id-1", "id-2");

        byte[] fromByteArray = service.exportAsZip(ids, "json", BulkExportOptions.defaults());

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        service.exportAsZipToStream(ids, "json", BulkExportOptions.defaults(), baos);
        byte[] fromStream = baos.toByteArray();

        assertEquals("Entry count must match", readZipEntryNames(fromByteArray).size(),
                readZipEntryNames(fromStream).size());
        assertEquals("Entry names must match", readZipEntryNames(fromByteArray),
                readZipEntryNames(fromStream));

        verify(mockWorkflowManager, mockSession, mockNode1, mockNode2, mockJsonWf1, mockJsonWf2);
    }

    @Test
    public void testExportAsZipToStreamEmptyListProducesEmptyZip() throws Exception {
        replay(mockWorkflowManager, mockSession);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        new BulkExportService(mockWorkflowManager, mockSession)
                .exportAsZipToStream(List.of(), "json", BulkExportOptions.defaults(), baos);

        assertEquals("Empty ZIP should have no entries", 0, readZipEntryNames(baos.toByteArray()).size());
        verify(mockWorkflowManager, mockSession);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testExportAsZipToStreamThrowsWhenExceedingCap() throws Exception {
        List<String> tooMany = new ArrayList<>();
        for (int i = 0; i < 501; i++) { tooMany.add("id-" + i); }
        replay(mockWorkflowManager, mockSession);

        new BulkExportService(mockWorkflowManager, mockSession)
                .exportAsZipToStream(tooMany, "json", BulkExportOptions.defaults(), new ByteArrayOutputStream());
    }

    // --- ZIP sub-formats ---

    @Test
    public void testExportAsZipWithCsvSubFormatCallsCsvWorkflow() throws Exception {
        ExportTabularWorkflow mockWf = createMock(ExportTabularWorkflow.class);
        expect(mockSession.getNodeByIdentifier("id-1")).andReturn(mockNode1);
        expect(mockNode1.getName()).andReturn("my-doc");
        expect(mockWorkflowManager.getWorkflow("exportcsv", mockNode1)).andReturn(mockWf);
        expect(mockWf.exportTabularDocument("id-1", "csv", true, false)).andReturn("col,val\n");
        replay(mockWorkflowManager, mockSession, mockNode1, mockWf);

        byte[] zip = new BulkExportService(mockWorkflowManager, mockSession)
                .exportAsZip(List.of("id-1"), "csv", BulkExportOptions.defaults());

        assertTrue("ZIP entry should be .csv", readZipEntryNames(zip).contains("my-doc.csv"));
        verify(mockWorkflowManager, mockSession, mockNode1, mockWf);
    }

    @Test
    public void testExportAsZipWithTsvSubFormatCallsTsvWorkflow() throws Exception {
        ExportTabularWorkflow mockWf = createMock(ExportTabularWorkflow.class);
        expect(mockSession.getNodeByIdentifier("id-1")).andReturn(mockNode1);
        expect(mockNode1.getName()).andReturn("my-doc");
        expect(mockWorkflowManager.getWorkflow("exporttsv", mockNode1)).andReturn(mockWf);
        expect(mockWf.exportTabularDocument("id-1", "tsv", true, false)).andReturn("col\tval\n");
        replay(mockWorkflowManager, mockSession, mockNode1, mockWf);

        byte[] zip = new BulkExportService(mockWorkflowManager, mockSession)
                .exportAsZip(List.of("id-1"), "tsv", BulkExportOptions.defaults());

        assertTrue("ZIP entry should be .tsv", readZipEntryNames(zip).contains("my-doc.tsv"));
        verify(mockWorkflowManager, mockSession, mockNode1, mockWf);
    }

    @Test
    public void testExportAsZipWithXmlSubFormatCallsXmlWorkflow() throws Exception {
        ExportXmlWorkflow mockWf = createMock(ExportXmlWorkflow.class);
        expect(mockSession.getNodeByIdentifier("id-1")).andReturn(mockNode1);
        expect(mockNode1.getName()).andReturn("my-doc");
        expect(mockWorkflowManager.getWorkflow("exportxml", mockNode1)).andReturn(mockWf);
        expect(mockWf.exportXmlDocument("id-1", true, true, true)).andReturn("<doc/>");
        replay(mockWorkflowManager, mockSession, mockNode1, mockWf);

        byte[] zip = new BulkExportService(mockWorkflowManager, mockSession)
                .exportAsZip(List.of("id-1"), "xml", BulkExportOptions.defaults());

        assertTrue("ZIP entry should be .xml", readZipEntryNames(zip).contains("my-doc.xml"));
        verify(mockWorkflowManager, mockSession, mockNode1, mockWf);
    }

    @Test
    public void testExportAsZipWithHtmlSubFormatCallsHtmlWorkflow() throws Exception {
        ExportHtmlWorkflow mockWf = createMock(ExportHtmlWorkflow.class);
        expect(mockSession.getNodeByIdentifier("id-1")).andReturn(mockNode1);
        expect(mockNode1.getName()).andReturn("my-doc");
        expect(mockWorkflowManager.getWorkflow("exporthtml", mockNode1)).andReturn(mockWf);
        expect(mockWf.exportHtmlDocument("id-1", true, true)).andReturn("<html/>");
        replay(mockWorkflowManager, mockSession, mockNode1, mockWf);

        byte[] zip = new BulkExportService(mockWorkflowManager, mockSession)
                .exportAsZip(List.of("id-1"), "html", BulkExportOptions.defaults());

        assertTrue("ZIP entry should be .html", readZipEntryNames(zip).contains("my-doc.html"));
        verify(mockWorkflowManager, mockSession, mockNode1, mockWf);
    }

    @Test
    public void testExportAsZipWithPdfSubFormatCallsPdfWorkflow() throws Exception {
        ExportPdfWorkflow mockWf = createMock(ExportPdfWorkflow.class);
        expect(mockSession.getNodeByIdentifier("id-1")).andReturn(mockNode1);
        expect(mockNode1.getName()).andReturn("my-doc");
        expect(mockWorkflowManager.getWorkflow("exportpdf", mockNode1)).andReturn(mockWf);
        expect(mockWf.exportPdfDocument("id-1", "A4", "portrait", "medium", true))
                .andReturn(createMinimalPdf());
        replay(mockWorkflowManager, mockSession, mockNode1, mockWf);

        byte[] zip = new BulkExportService(mockWorkflowManager, mockSession)
                .exportAsZip(List.of("id-1"), "pdf", BulkExportOptions.defaults());

        assertTrue("ZIP entry should be .pdf", readZipEntryNames(zip).contains("my-doc.pdf"));
        verify(mockWorkflowManager, mockSession, mockNode1, mockWf);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testExportAsZipUnknownFormatThrows() throws Exception {
        expect(mockSession.getNodeByIdentifier("id-1")).andReturn(mockNode1);
        expect(mockNode1.getName()).andReturn("my-doc");
        replay(mockWorkflowManager, mockSession, mockNode1);

        new BulkExportService(mockWorkflowManager, mockSession)
                .exportAsZip(List.of("id-1"), "docx", BulkExportOptions.defaults());
    }

    // --- Helpers ---

    private List<String> readZipEntryNames(final byte[] zipBytes) throws Exception {
        List<String> names = new ArrayList<>();
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                names.add(entry.getName());
                zis.closeEntry();
            }
        }
        return names;
    }

    private byte[] createMinimalPdf() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (PDDocument doc = new PDDocument()) {
            doc.addPage(new PDPage());
            doc.save(baos);
        }
        return baos.toByteArray();
    }
}
