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

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.RadioChoice;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExportPdfDialog extends AbstractDialog<String> {

    private static final Logger log = LoggerFactory.getLogger(ExportPdfDialog.class);
    private static final long serialVersionUID = 1L;

    private String pageSize = "A4";
    private String orientation = "portrait";
    private String fontSize = "medium";
    private boolean includeMetadata = true;
    private String downloadUrl;

    public ExportPdfDialog(IPluginContext context, String downloadUrl) {
        super(Model.of(""));
        this.downloadUrl = downloadUrl;

        // Message
        add(new Label("message", "Your PDF file is ready to download. Configure options below:"));

        // Page size dropdown
        List<String> pageSizeChoices = new ArrayList<>();
        pageSizeChoices.add("A4");
        pageSizeChoices.add("Letter");
        pageSizeChoices.add("Legal");
        add(new DropDownChoice<>("pageSize", new PropertyModel<>(this, "pageSize"), pageSizeChoices));

        // Orientation radio buttons
        List<String> orientationChoices = new ArrayList<>();
        orientationChoices.add("portrait");
        orientationChoices.add("landscape");
        add(new RadioChoice<>("orientation", new PropertyModel<>(this, "orientation"), orientationChoices));

        // Font size dropdown
        List<String> fontSizeChoices = new ArrayList<>();
        fontSizeChoices.add("small");
        fontSizeChoices.add("medium");
        fontSizeChoices.add("large");
        add(new DropDownChoice<>("fontSize", new PropertyModel<>(this, "fontSize"), fontSizeChoices));

        // Include metadata checkbox
        add(new AjaxCheckBox("includeMetadata", new PropertyModel<>(this, "includeMetadata")) {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                log.info("Include metadata toggled to: {}", includeMetadata);
            }
        });

        // Download button
        add(new AjaxLink<Void>("downloadButton") {
            @Override
            public void onClick(AjaxRequestTarget target) {
                String url = downloadUrl + (downloadUrl.contains("?") ? "&" : "?") +
                    "pageSize=" + pageSize +
                    "&orientation=" + orientation +
                    "&fontSize=" + fontSize +
                    "&includeMetadata=" + includeMetadata;
                target.appendJavaScript("window.location.href = '" + url + "';");
                log.info("Triggering PDF download with pageSize={}, orientation={}, fontSize={}, includeMetadata={}",
                        pageSize, orientation, fontSize, includeMetadata);
            }
        });
    }

    @Override
    public IModel<String> getTitle() {
        return Model.of("PDF Export Successful");
    }

    @Override
    protected void onOk() {
        // Close dialog on OK
    }

}
