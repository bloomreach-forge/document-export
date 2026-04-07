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

import static org.junit.Assert.*;

import org.junit.Test;
import org.onehippo.forge.exportjson.repository.bulk.BulkExportOptions;
import org.onehippo.forge.exportjson.repository.bulk.BulkExportService;

public class BulkExportOptionsTest {

    @Test
    public void testDefaultsReturnExpectedValues() {
        BulkExportOptions opts = BulkExportOptions.defaults();
        assertEquals("A4", opts.pageSize());
        assertEquals("portrait", opts.orientation());
        assertEquals("medium", opts.fontSize());
        assertTrue(opts.includeHeaders());
        assertFalse(opts.quoteAll());
        assertTrue(opts.includeMetadata());
        assertFalse("zipTabular must default to false (flat merged file)", opts.zipTabular());
    }

    @Test
    public void testCustomValuesRoundTripViaRecord() {
        BulkExportOptions opts = new BulkExportOptions("Letter", "landscape", "small", false, true, false, true);
        assertEquals("Letter", opts.pageSize());
        assertEquals("landscape", opts.orientation());
        assertEquals("small", opts.fontSize());
        assertFalse(opts.includeHeaders());
        assertTrue(opts.quoteAll());
        assertFalse(opts.includeMetadata());
        assertTrue(opts.zipTabular());
    }

    @Test
    public void testRecordEquality() {
        BulkExportOptions a = BulkExportOptions.defaults();
        BulkExportOptions b = BulkExportOptions.defaults();
        assertEquals("Two defaults() instances must be equal", a, b);
        assertEquals("Hash codes must match", a.hashCode(), b.hashCode());
    }

    /**
     * Documents the soft PDF cap used by BulkExportDownloadPage to pre-truncate
     * before calling exportAsMergedPdf. If this constant changes, the download
     * page's truncation logic must be reviewed.
     */
    @Test
    public void testPdfSoftCapConstantValue() {
        assertEquals("PDF soft cap must be 50 — BulkExportDownloadPage truncates to this before calling service",
                50, BulkExportService.MAX_PDF_DOCUMENTS);
    }

    /**
     * Documents the hard cap enforced by BulkExportService across all export methods.
     * BulkExportDownloadPage sends at most MAX_PDF_DOCUMENTS to exportAsMergedPdf and
     * at most MAX_DOCUMENTS to CSV/TSV/ZIP — so the service hard cap is a safety net only.
     */
    @Test
    public void testHardCapConstantValue() {
        assertEquals("Hard cap must be 500", 500, BulkExportService.MAX_DOCUMENTS);
        assertTrue("PDF soft cap must be less than or equal to hard cap",
                BulkExportService.MAX_PDF_DOCUMENTS <= BulkExportService.MAX_DOCUMENTS);
    }
}
