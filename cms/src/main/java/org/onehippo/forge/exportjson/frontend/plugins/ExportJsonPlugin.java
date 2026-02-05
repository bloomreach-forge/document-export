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
package org.onehippo.forge.exportjson.frontend.plugins;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.wicket.Component;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.hippoecm.addon.workflow.StdWorkflow;
import org.hippoecm.addon.workflow.WorkflowDescriptorModel;
import org.hippoecm.frontend.dialog.IDialogService;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.icon.HippoIcon;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.frontend.skin.Icon;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.WorkflowDescriptor;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.api.WorkflowManager;
import org.onehippo.forge.exportjson.frontend.dialog.ExportHtmlDialog;
import org.onehippo.forge.exportjson.frontend.dialog.ExportJsonDialog;
import org.onehippo.forge.exportjson.frontend.dialog.ExportPdfDialog;
import org.onehippo.forge.exportjson.frontend.dialog.ExportTabularDialog;
import org.onehippo.forge.exportjson.frontend.dialog.ExportXmlDialog;
import org.onehippo.forge.exportjson.frontend.pages.ExportHtmlDownloadPage;
import org.onehippo.forge.exportjson.frontend.pages.ExportJsonDownloadPage;
import org.onehippo.forge.exportjson.frontend.pages.ExportPdfDownloadPage;
import org.onehippo.forge.exportjson.frontend.pages.ExportTabularDownloadPage;
import org.onehippo.forge.exportjson.frontend.pages.ExportXmlDownloadPage;
import org.onehippo.forge.exportjson.repository.workflow.ExportHtmlWorkflow;
import org.onehippo.forge.exportjson.repository.workflow.ExportJsonWorkflow;
import org.onehippo.forge.exportjson.repository.workflow.ExportPdfWorkflow;
import org.onehippo.forge.exportjson.repository.workflow.ExportTabularWorkflow;
import org.onehippo.forge.exportjson.repository.workflow.ExportXmlWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Plugin for exporting documents in multiple formats: JSON, CSV, TSV, and XML.
 * Registers as the frontend:renderer for the exportjson workflow category.
 */
public class ExportJsonPlugin extends RenderPlugin<WorkflowDescriptor> {

    private static Logger log = LoggerFactory.getLogger(ExportJsonPlugin.class);
    private IDialogService dialogService;

    public ExportJsonPlugin(final IPluginContext context, IPluginConfig config) {
        super(context, config);
        this.dialogService = context.getService(IDialogService.class.getName(), IDialogService.class);
    }

    @Override
    protected void onStart() {
        super.onStart();
        modelChanged();
    }


    @Override
    protected void onModelChanged() {
        super.onModelChanged();
        removeAll();

        try {
            WorkflowDescriptorModel workflowDescriptorModel = (WorkflowDescriptorModel) getDefaultModel();
            WorkflowDescriptor workflowDescriptor = (WorkflowDescriptor) getDefaultModelObject();

            if (workflowDescriptor != null) {
                final Node node = workflowDescriptorModel.getNode();
                final Node handle = node.getParent();
                final String name = handle.getName();
                final String handlePath = handle.getPath();
                final String path = handlePath + '/' + name;

                if (node.getPath().equals(handlePath) || node.getPath().equals(path)) {
                    addExportOption(workflowDescriptorModel);
                }
            } else {
                log.warn("Workflow descriptor is null");
            }
        } catch (RepositoryException ex) {
            log.error(ex.getMessage(), ex);
        }
    }

    private void addExportOption(final WorkflowDescriptorModel workflowDescriptorModel) throws RepositoryException {
        final Node node = workflowDescriptorModel.getNode();
        final String nodeId = node.getIdentifier();
        final String nodeName = node.getName();

        // Add JSON export
        addJsonExport(workflowDescriptorModel, nodeId, nodeName);

        // Add CSV export
        addCsvExport(workflowDescriptorModel, nodeId, nodeName);

        // Add TSV export
        addTsvExport(workflowDescriptorModel, nodeId, nodeName);

        // Add XML export
        addXmlExport(workflowDescriptorModel, nodeId, nodeName);

        // Add PDF export
        addPdfExport(workflowDescriptorModel, nodeId, nodeName);

        // Add HTML export
        addHtmlExport(workflowDescriptorModel, nodeId, nodeName);
    }

    private void addJsonExport(final WorkflowDescriptorModel workflowDescriptorModel,
            final String nodeId, final String nodeName) {
        StringResourceModel buttonLabel = new StringResourceModel("download-json-label", this, null);

        StdWorkflow<ExportJsonWorkflow> workflow = new StdWorkflow<ExportJsonWorkflow>("exportjson", buttonLabel, getPluginContext(), workflowDescriptorModel) {
            @Override
            protected String execute(ExportJsonWorkflow wf) throws Exception {
                try {
                    String jsonContent = wf.exportJsonDocument(nodeId);
                    // Store in session for download page
                    getSession().setAttribute("exportJson_" + nodeId, jsonContent);
                    // Show the export dialog
                    String downloadUrl = urlFor(ExportJsonDownloadPage.class,
                        new PageParameters()
                            .add("nodeId", nodeId)
                            .add("nodeName", nodeName)
                        ).toString();

                    ExportJsonDialog dialog = new ExportJsonDialog(getPluginContext(), downloadUrl);
                    dialogService.show(dialog);

                    return null;
                } catch (Exception e) {
                    log.error("Error exporting JSON: {}", e.getMessage(), e);
                    throw e;
                }
            }

            @Override
            protected Component getIcon(final String id) {
                return HippoIcon.fromSprite(id, Icon.ARROW_DOWN);
            }
        };

        add(workflow);
    }

    private void addCsvExport(final WorkflowDescriptorModel workflowDescriptorModel,
            final String nodeId, final String nodeName) {
        StringResourceModel buttonLabel = new StringResourceModel("download-csv-label", this, null);

        StdWorkflow<ExportJsonWorkflow> workflow = new StdWorkflow<ExportJsonWorkflow>("exportcsv", buttonLabel, getPluginContext(), workflowDescriptorModel) {
            @Override
            protected String execute(ExportJsonWorkflow wf) throws Exception {
                try {
                    // Get the correct CSV workflow using WorkflowManager
                    Session jcrSession = ((UserSession) getSession()).getJcrSession();
                    HippoWorkspace workspace = (HippoWorkspace) jcrSession.getWorkspace();
                    WorkflowManager workflowManager = workspace.getWorkflowManager();
                    Node node = jcrSession.getNodeByIdentifier(nodeId);

                    ExportTabularWorkflow csvWorkflow = (ExportTabularWorkflow) workflowManager.getWorkflow("exportcsv", node);

                    if (csvWorkflow == null) {
                        throw new WorkflowException("CSV export workflow not found");
                    }

                    String csvContent = csvWorkflow.exportTabularDocument(nodeId, "csv", true, false);
                    // Store in session for download page
                    getSession().setAttribute("exportCsv_" + nodeId, csvContent);
                    // Show the export dialog
                    String downloadUrl = urlFor(ExportTabularDownloadPage.class,
                        new PageParameters()
                            .add("nodeId", nodeId)
                            .add("nodeName", nodeName)
                            .add("format", "csv")
                        ).toString();

                    ExportTabularDialog dialog = new ExportTabularDialog(getPluginContext(), downloadUrl, "CSV");
                    dialogService.show(dialog);

                    return null;
                } catch (Exception e) {
                    log.error("Error exporting CSV: {}", e.getMessage(), e);
                    throw e;
                }
            }

            @Override
            protected Component getIcon(final String id) {
                return HippoIcon.fromSprite(id, Icon.ARROW_DOWN);
            }
        };

        add(workflow);
    }

    private void addTsvExport(final WorkflowDescriptorModel workflowDescriptorModel,
            final String nodeId, final String nodeName) {
        StringResourceModel buttonLabel = new StringResourceModel("download-tsv-label", this, null);

        StdWorkflow<ExportJsonWorkflow> workflow = new StdWorkflow<ExportJsonWorkflow>("exporttsv", buttonLabel, getPluginContext(), workflowDescriptorModel) {
            @Override
            protected String execute(ExportJsonWorkflow wf) throws Exception {
                try {
                    // Get the correct TSV workflow using WorkflowManager
                    Session jcrSession = ((UserSession) getSession()).getJcrSession();
                    HippoWorkspace workspace = (HippoWorkspace) jcrSession.getWorkspace();
                    WorkflowManager workflowManager = workspace.getWorkflowManager();
                    Node node = jcrSession.getNodeByIdentifier(nodeId);

                    ExportTabularWorkflow tsvWorkflow = (ExportTabularWorkflow) workflowManager.getWorkflow("exporttsv", node);

                    if (tsvWorkflow == null) {
                        throw new WorkflowException("TSV export workflow not found");
                    }

                    String tsvContent = tsvWorkflow.exportTabularDocument(nodeId, "tsv", true, false);
                    // Store in session for download page
                    getSession().setAttribute("exportTsv_" + nodeId, tsvContent);
                    // Show the export dialog
                    String downloadUrl = urlFor(ExportTabularDownloadPage.class,
                        new PageParameters()
                            .add("nodeId", nodeId)
                            .add("nodeName", nodeName)
                            .add("format", "tsv")
                        ).toString();

                    ExportTabularDialog dialog = new ExportTabularDialog(getPluginContext(), downloadUrl, "TSV");
                    dialogService.show(dialog);

                    return null;
                } catch (Exception e) {
                    log.error("Error exporting TSV: {}", e.getMessage(), e);
                    throw e;
                }
            }

            @Override
            protected Component getIcon(final String id) {
                return HippoIcon.fromSprite(id, Icon.ARROW_DOWN);
            }
        };

        add(workflow);
    }

    private void addXmlExport(final WorkflowDescriptorModel workflowDescriptorModel,
            final String nodeId, final String nodeName) {
        StringResourceModel buttonLabel = new StringResourceModel("download-xml-label", this, null);

        StdWorkflow<ExportJsonWorkflow> workflow = new StdWorkflow<ExportJsonWorkflow>("exportxml", buttonLabel, getPluginContext(), workflowDescriptorModel) {
            @Override
            protected String execute(ExportJsonWorkflow wf) throws Exception {
                try {
                    // Get the correct XML workflow using WorkflowManager
                    Session jcrSession = ((UserSession) getSession()).getJcrSession();
                    HippoWorkspace workspace = (HippoWorkspace) jcrSession.getWorkspace();
                    WorkflowManager workflowManager = workspace.getWorkflowManager();
                    Node node = jcrSession.getNodeByIdentifier(nodeId);

                    ExportXmlWorkflow xmlWorkflow = (ExportXmlWorkflow) workflowManager.getWorkflow("exportxml", node);

                    if (xmlWorkflow == null) {
                        throw new WorkflowException("XML export workflow not found");
                    }

                    String xmlContent = xmlWorkflow.exportXmlDocument(nodeId, true, false, false);
                    getSession().setAttribute("exportXml_" + nodeId, xmlContent);

                    // Show the export dialog
                    String downloadUrl = urlFor(ExportXmlDownloadPage.class,
                        new PageParameters()
                            .add("nodeId", nodeId)
                            .add("nodeName", nodeName)
                        ).toString();

                    ExportXmlDialog dialog = new ExportXmlDialog(getPluginContext(), downloadUrl);
                    dialogService.show(dialog);

                    return null;
                } catch (Exception e) {
                    log.error("Error exporting XML: {}", e.getMessage(), e);
                    throw e;
                }
            }

            @Override
            protected Component getIcon(final String id) {
                return HippoIcon.fromSprite(id, Icon.ARROW_DOWN);
            }
        };

        add(workflow);
    }

    private void addPdfExport(final WorkflowDescriptorModel workflowDescriptorModel,
            final String nodeId, final String nodeName) {
        StringResourceModel buttonLabel = new StringResourceModel("download-pdf-label", this, null);

        StdWorkflow<ExportJsonWorkflow> workflow = new StdWorkflow<ExportJsonWorkflow>("exportpdf", buttonLabel, getPluginContext(), workflowDescriptorModel) {
            @Override
            protected String execute(ExportJsonWorkflow wf) throws Exception {
                try {
                    // Get the correct PDF workflow using WorkflowManager
                    Session jcrSession = ((UserSession) getSession()).getJcrSession();
                    HippoWorkspace workspace = (HippoWorkspace) jcrSession.getWorkspace();
                    WorkflowManager workflowManager = workspace.getWorkflowManager();
                    Node node = jcrSession.getNodeByIdentifier(nodeId);

                    ExportPdfWorkflow pdfWorkflow = (ExportPdfWorkflow) workflowManager.getWorkflow("exportpdf", node);

                    if (pdfWorkflow == null) {
                        throw new WorkflowException("PDF export workflow not found");
                    }

                    byte[] pdfContent = pdfWorkflow.exportPdfDocument(nodeId, "A4", "portrait", "medium", true);
                    getSession().setAttribute("exportPdf_" + nodeId, pdfContent);

                    // Show the export dialog
                    String downloadUrl = urlFor(ExportPdfDownloadPage.class,
                        new PageParameters()
                            .add("nodeId", nodeId)
                            .add("nodeName", nodeName)
                        ).toString();

                    ExportPdfDialog dialog = new ExportPdfDialog(getPluginContext(), downloadUrl);
                    dialogService.show(dialog);

                    return null;
                } catch (Exception e) {
                    log.error("Error exporting PDF: {}", e.getMessage(), e);
                    throw e;
                }
            }

            @Override
            protected Component getIcon(final String id) {
                return HippoIcon.fromSprite(id, Icon.ARROW_DOWN);
            }
        };

        add(workflow);
    }

    private void addHtmlExport(final WorkflowDescriptorModel workflowDescriptorModel,
            final String nodeId, final String nodeName) {
        StringResourceModel buttonLabel = new StringResourceModel("download-html-label", this, null);

        StdWorkflow<ExportJsonWorkflow> workflow = new StdWorkflow<ExportJsonWorkflow>("exporthtml", buttonLabel, getPluginContext(), workflowDescriptorModel) {
            @Override
            protected String execute(ExportJsonWorkflow wf) throws Exception {
                try {
                    // Get the correct HTML workflow using WorkflowManager
                    Session jcrSession = ((UserSession) getSession()).getJcrSession();
                    HippoWorkspace workspace = (HippoWorkspace) jcrSession.getWorkspace();
                    WorkflowManager workflowManager = workspace.getWorkflowManager();
                    Node node = jcrSession.getNodeByIdentifier(nodeId);

                    ExportHtmlWorkflow htmlWorkflow = (ExportHtmlWorkflow) workflowManager.getWorkflow("exporthtml", node);

                    if (htmlWorkflow == null) {
                        throw new WorkflowException("HTML export workflow not found");
                    }

                    String htmlContent = htmlWorkflow.exportHtmlDocument(nodeId, true, true);
                    getSession().setAttribute("exportHtml_" + nodeId, htmlContent);

                    // Show the export dialog
                    String downloadUrl = urlFor(ExportHtmlDownloadPage.class,
                        new PageParameters()
                            .add("nodeId", nodeId)
                            .add("nodeName", nodeName)
                        ).toString();

                    ExportHtmlDialog dialog = new ExportHtmlDialog(getPluginContext(), downloadUrl);
                    dialogService.show(dialog);

                    return null;
                } catch (Exception e) {
                    log.error("Error exporting HTML: {}", e.getMessage(), e);
                    throw e;
                }
            }

            @Override
            protected Component getIcon(final String id) {
                return HippoIcon.fromSprite(id, Icon.ARROW_DOWN);
            }
        };

        add(workflow);
    }

}
