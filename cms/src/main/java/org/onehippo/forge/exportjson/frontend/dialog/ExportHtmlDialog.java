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

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExportHtmlDialog extends AbstractDialog<String> {

    private static final Logger log = LoggerFactory.getLogger(ExportHtmlDialog.class);
    private static final long serialVersionUID = 1L;

    private boolean includeMetadata = true;
    private boolean includeStyles = true;
    private String downloadUrl;

    public ExportHtmlDialog(IPluginContext context, String downloadUrl) {
        super(Model.of(""));
        this.downloadUrl = downloadUrl;

        // Message
        add(new Label("message", "Your HTML file is ready to download. Configure options below:"));

        // Include metadata checkbox
        add(new AjaxCheckBox("includeMetadata", new PropertyModel<>(this, "includeMetadata")) {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                log.info("Include metadata toggled to: {}", includeMetadata);
            }
        });

        // Include styles checkbox
        add(new AjaxCheckBox("includeStyles", new PropertyModel<>(this, "includeStyles")) {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                log.info("Include styles toggled to: {}", includeStyles);
            }
        });

        // Download button
        add(new AjaxLink<Void>("downloadButton") {
            @Override
            public void onClick(AjaxRequestTarget target) {
                String url = downloadUrl + (downloadUrl.contains("?") ? "&" : "?") +
                    "includeMetadata=" + includeMetadata +
                    "&includeStyles=" + includeStyles;
                target.appendJavaScript("window.location.href = '" + url + "';");
                log.info("Triggering HTML download with includeMetadata={}, includeStyles={}",
                        includeMetadata, includeStyles);
            }
        });
    }

    @Override
    public IModel<String> getTitle() {
        return Model.of("HTML Export Successful");
    }

    @Override
    protected void onOk() {
        // Close dialog on OK
    }

}
