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

/**
 * Options controlling bulk export behavior.
 *
 * @param pageSize      PDF page size: "A4", "Letter", or "Legal"
 * @param orientation   PDF orientation: "portrait" or "landscape"
 * @param fontSize      PDF font size: "small", "medium", or "large"
 * @param includeHeaders whether to include a header row in CSV/TSV output
 * @param quoteAll      whether to quote all values in CSV/TSV output
 * @param includeMetadata whether to include document metadata in PDF/HTML/XML output
 * @param zipTabular    when true, CSV/TSV export produces a ZIP of individual per-document
 *                      files instead of a single merged file
 */
public record BulkExportOptions(
        String pageSize,
        String orientation,
        String fontSize,
        boolean includeHeaders,
        boolean quoteAll,
        boolean includeMetadata,
        boolean zipTabular
) {
    public static BulkExportOptions defaults() {
        return new BulkExportOptions("A4", "portrait", "medium", true, false, true, false);
    }
}
