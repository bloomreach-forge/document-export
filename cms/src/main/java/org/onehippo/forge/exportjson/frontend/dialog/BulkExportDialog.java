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
package org.onehippo.forge.exportjson.frontend.dialog;

import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormChoiceComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.RadioChoice;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.cycle.RequestCycle;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.onehippo.forge.exportjson.repository.bulk.BulkExportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Dialog for selecting bulk export format and options before downloading.
 * The OK button triggers the download and closes the dialog.
 * <p>
 * Formats:
 * <ul>
 *   <li>csv / tsv — merged flat file</li>
 *   <li>json / xml / html — ZIP of individual files</li>
 *   <li>pdf — merged single PDF</li>
 * </ul>
 */
public class BulkExportDialog extends AbstractDialog<String> {

    private static final Logger log = LoggerFactory.getLogger(BulkExportDialog.class);
    private static final long serialVersionUID = 1L;

    private String format = "csv";
    private String pageSize = "A4";
    private String orientation = "portrait";
    private String fontSize = "medium";
    private boolean includeHeaders = true;
    private boolean quoteAll = false;
    private boolean includeMetadata = true;
    private boolean zipTabular = false;

    private final String downloadUrl;
    private final int documentCount;

    public BulkExportDialog(final IPluginContext context, final String downloadUrl,
                            final int documentCount, final boolean truncated) {
        super(Model.of(""));
        this.downloadUrl = downloadUrl;
        this.documentCount = documentCount;

        final String countMsg = documentCount + " document(s) selected for export"
                + (truncated ? " (first 500 of a larger set)" : "");
        add(new Label("documentCount", countMsg));

        // --- Format radio ---
        final RadioChoice<String> formatChoice = new RadioChoice<>("format",
                new PropertyModel<>(this, "format"),
                List.of("csv", "tsv", "json", "xml", "html", "pdf"));

        // --- Conditional option sections ---
        final WebMarkupContainer csvTsvOptions = new WebMarkupContainer("csvTsvOptions");
        csvTsvOptions.setOutputMarkupPlaceholderTag(true);
        csvTsvOptions.setVisible(true);

        csvTsvOptions.add(new AjaxCheckBox("includeHeaders", new PropertyModel<>(this, "includeHeaders")) {
            @Override protected void onUpdate(final AjaxRequestTarget t) { }
        });
        csvTsvOptions.add(new AjaxCheckBox("quoteAll", new PropertyModel<>(this, "quoteAll")) {
            @Override protected void onUpdate(final AjaxRequestTarget t) { }
        });
        csvTsvOptions.add(new AjaxCheckBox("zipTabular", new PropertyModel<>(this, "zipTabular")) {
            @Override protected void onUpdate(final AjaxRequestTarget t) { }
        });

        final WebMarkupContainer pdfOptions = new WebMarkupContainer("pdfOptions");
        pdfOptions.setOutputMarkupPlaceholderTag(true);
        pdfOptions.setVisible(false);

        pdfOptions.add(new DropDownChoice<>("pageSize",
                new PropertyModel<>(this, "pageSize"), List.of("A4", "Letter", "Legal")));
        pdfOptions.add(new RadioChoice<>("orientation",
                new PropertyModel<>(this, "orientation"), List.of("portrait", "landscape")));
        pdfOptions.add(new DropDownChoice<>("fontSize",
                new PropertyModel<>(this, "fontSize"), List.of("small", "medium", "large")));

        final WebMarkupContainer metadataOption = new WebMarkupContainer("metadataOption");
        metadataOption.setOutputMarkupPlaceholderTag(true);
        metadataOption.setVisible(false);

        metadataOption.add(new AjaxCheckBox("includeMetadata", new PropertyModel<>(this, "includeMetadata")) {
            @Override protected void onUpdate(final AjaxRequestTarget t) { }
        });

        final WebMarkupContainer largeExportNotice = new WebMarkupContainer("largeExportNotice");
        largeExportNotice.setVisible(documentCount >= BulkExportService.MAX_DOCUMENTS);
        add(largeExportNotice);

        final WebMarkupContainer pdfCapWarning = new WebMarkupContainer("pdfCapWarning");
        pdfCapWarning.setOutputMarkupPlaceholderTag(true);
        pdfCapWarning.setVisible(false);
        pdfCapWarning.add(new Label("pdfCapMessage",
                "Only the first " + BulkExportService.MAX_PDF_DOCUMENTS
                        + " documents will be exported as PDF (memory limit). "
                        + "Use CSV, TSV, or ZIP to export all " + documentCount + "."));

        // Toggle sections when format changes
        formatChoice.add(new AjaxFormChoiceComponentUpdatingBehavior() {
            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                final boolean isPdf = "pdf".equals(format);
                final boolean isCsvTsv = "csv".equals(format) || "tsv".equals(format);
                final boolean hasMetadata = !isCsvTsv;
                final boolean showPdfCap = isPdf && documentCount > BulkExportService.MAX_PDF_DOCUMENTS;

                csvTsvOptions.setVisible(isCsvTsv);
                pdfOptions.setVisible(isPdf);
                metadataOption.setVisible(hasMetadata);
                pdfCapWarning.setVisible(showPdfCap);

                target.add(csvTsvOptions, pdfOptions, metadataOption, pdfCapWarning);
            }
        });

        add(formatChoice);
        add(csvTsvOptions);
        add(pdfOptions);
        add(metadataOption);
        add(pdfCapWarning);
    }

    @Override
    public IModel<String> getTitle() {
        return Model.of("Export Search Results");
    }

    @Override
    protected void onOk() {
        final String sep = downloadUrl.contains("?") ? "&" : "?";
        final String url = downloadUrl + sep
                + "format=" + format
                + "&pageSize=" + pageSize
                + "&orientation=" + orientation
                + "&fontSize=" + fontSize
                + "&includeHeaders=" + includeHeaders
                + "&quoteAll=" + quoteAll
                + "&includeMetadata=" + includeMetadata
                + "&zipTabular=" + zipTabular;
        RequestCycle.get().find(AjaxRequestTarget.class)
                .ifPresent(t -> t.appendJavaScript("window.location.href = '" + url + "';"));
        log.info("Triggering bulk export download: format={}, documents in session", format);
    }
}
