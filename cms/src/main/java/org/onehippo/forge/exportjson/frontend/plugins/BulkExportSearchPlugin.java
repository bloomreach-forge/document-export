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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import com.onehippo.cms7.search.frontend.engine.model.DocumentSelection;
import com.onehippo.cms7.search.frontend.engine.model.QueryResultModel;
import org.hippoecm.frontend.dialog.IDialogService;
import org.hippoecm.frontend.model.event.IEvent;
import org.hippoecm.frontend.model.event.IObservable;
import org.hippoecm.frontend.model.event.IObserver;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.frontend.session.UserSession;
import org.onehippo.cms7.services.search.query.Query;
import org.onehippo.cms7.services.search.result.Hit;
import org.onehippo.cms7.services.search.result.HitIterator;
import org.onehippo.cms7.services.search.result.QueryResult;
import org.onehippo.forge.exportjson.frontend.dialog.BulkExportDialog;
import org.onehippo.forge.exportjson.frontend.pages.BulkExportDownloadPage;
import org.onehippo.forge.exportjson.repository.bulk.BulkExportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Adds an "Export Selected" action to the Advanced Search toolbar.
 *
 * <p>Document selection is driven by the Advanced Search checkbox UI
 * ({@code model.selected} service):
 * <ul>
 *   <li>"Select all" — re-executes the live query via {@link QueryResultModel}
 *       to collect up to {@value BulkExportService#MAX_DOCUMENTS} IDs.</li>
 *   <li>Explicit selection (individual or "Select all on page") — uses the
 *       identifiers already in {@link DocumentSelection} directly.</li>
 *   <li>Nothing selected — logs a warning and does nothing.</li>
 * </ul>
 *
 * <p>Register in CMS configuration at
 * {@code /hippo:configuration/hippo:frontend/cms/cms-advanced-search/bulkExport}
 * with {@code wicket.id: ${search.extensions}} and {@code wicket.model: ${model.selected}}.
 * Binding to {@code model.selected} causes {@code AbstractRenderService} to automatically
 * observe {@code DocumentSelectionModel} changes and re-render, which re-evaluates
 * the button's enabled state in {@code onConfigure()}.
 */
public class BulkExportSearchPlugin extends RenderPlugin<DocumentSelection> {

    private static final Logger log = LoggerFactory.getLogger(BulkExportSearchPlugin.class);
    private static final String MODEL_SEARCH_SERVICE = "model.search";
    private static final String MODEL_SELECTED_SERVICE = "model.selected";

    public static final String SESSION_IDS_PREFIX = "bulkExport_ids_";

    private IObservable observedSelectionModel;
    private IObserver<IObservable> selectionObserver;

    public BulkExportSearchPlugin(final IPluginContext context, final IPluginConfig config) {
        super(context, config);

        add(new AjaxLink<Void>("exportResults") {
            @Override
            protected void onComponentTag(final ComponentTag tag) {
                // Render as <button> so the markup matches the HTML template.
                // Do NOT call setEnabled(false) anywhere — Wicket omits the click handler
                // JavaScript when a link is disabled; visual disabled state is CSS-only (bxe-disabled).
                tag.setName("button");
                tag.put("type", "button");
                super.onComponentTag(tag);
            }

            @Override
            public void onClick(final AjaxRequestTarget target) {
                try {
                    final List<String> nodeIds = collectVariantNodeIds();
                    if (nodeIds.isEmpty()) {
                        log.warn("No documents selected for export — use Select all or check individual rows");
                        return;
                    }

                    final boolean truncated = nodeIds.size() > BulkExportService.MAX_DOCUMENTS;
                    final List<String> cappedIds = truncated
                            ? nodeIds.subList(0, BulkExportService.MAX_DOCUMENTS)
                            : nodeIds;

                    final String exportId = UUID.randomUUID().toString();
                    getSession().setAttribute(SESSION_IDS_PREFIX + exportId,
                            (Serializable) new ArrayList<>(cappedIds));

                    final String downloadUrl = urlFor(BulkExportDownloadPage.class,
                            new PageParameters().add("exportId", exportId)).toString();

                    final IDialogService dialogService = getPluginContext().getService(
                            IDialogService.class.getName(), IDialogService.class);
                    dialogService.show(new BulkExportDialog(
                            getPluginContext(), downloadUrl, cappedIds.size(), truncated));

                } catch (Exception e) {
                    log.error("Error initiating bulk export", e);
                }
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        // AbstractRenderService resolves the service name from config (${model.selected} → actual name).
        // Use the same resolved name so we look up the same service instance it bound.
        final String resolvedName = getPluginConfig().getString("wicket.model", MODEL_SELECTED_SERVICE);
        final IModel<?> selectionModel = getPluginContext().getService(resolvedName, IModel.class);
        if (selectionModel instanceof IObservable) {
            observedSelectionModel = (IObservable) selectionModel;
            selectionObserver = new IObserver<IObservable>() {
                private static final long serialVersionUID = 1L;

                @Override
                public IObservable getObservable() {
                    return observedSelectionModel;
                }

                @Override
                public void onEvent(final Iterator<? extends IEvent<IObservable>> events) {
                    redraw();
                }
            };
            getPluginContext().registerService(selectionObserver, IObserver.class.getName());
            log.debug("Registered selection observer for service '{}'", resolvedName);
        } else {
            log.warn("Could not obtain IObservable for service '{}' (resolved from 'wicket.model') "
                    + "— server-side reactive re-render disabled; JavaScript fallback handles button state",
                    resolvedName);
        }
    }

    @Override
    protected void onStop() {
        if (selectionObserver != null) {
            getPluginContext().unregisterService(selectionObserver, IObserver.class.getName());
            selectionObserver = null;
            observedSelectionModel = null;
        }
        super.onStop();
    }

    /**
     * Returns the variant node IDs to export, driven by the checkbox selection state:
     * <ol>
     *   <li>If "Select all" is active → executes the live query (up to cap+1 for truncation detection).</li>
     *   <li>If specific documents are checked → resolves each checked identifier to its variant.</li>
     *   <li>If nothing is selected → returns empty list.</li>
     * </ol>
     */
    private List<String> collectVariantNodeIds() {
        // wicket.model is bound to ${model.selected} — getModel() gives DocumentSelectionModel directly.
        final DocumentSelection selection = getModel().getObject();
        if (selection != null) {
            if (selection.isSelectedAll()) {
                log.debug("'Select all' active — collecting via live query");
                return collectFromQuery();
            }
            if (selection.hasDocuments()) {
                log.debug("Explicit selection of {} identifier(s)", selection.getIdentifiers().size());
                return resolveVariantIdsFromIdentifiers(selection.getIdentifiers());
            }
            // Nothing selected
            return List.of();
        }

        // Model object null — fall back to full query
        log.debug("Selection model object null, falling back to live query");
        return collectFromQuery();
    }

    /**
     * Executes the current search query via {@link QueryResultModel} and
     * resolves each hit to its published variant UUID.
     */
    @SuppressWarnings("unchecked")
    private List<String> collectFromQuery() {
        final IModel<Query> searchModel =
                getPluginContext().getService(MODEL_SEARCH_SERVICE, IModel.class);
        if (searchModel == null) {
            log.warn("Search model service '{}' not available — cannot collect from query", MODEL_SEARCH_SERVICE);
            return List.of();
        }
        final int fetchLimit = BulkExportService.MAX_DOCUMENTS + 1;
        final QueryResultModel queryResultModel = new QueryResultModel(searchModel, 0, fetchLimit);
        final QueryResult queryResult = queryResultModel.getObject();
        if (queryResult == null) {
            log.warn("QueryResultModel returned null — no search results available");
            return List.of();
        }

        final Session jcrSession = ((UserSession) getSession()).getJcrSession();
        final List<String> variantIds = new ArrayList<>();
        final HitIterator hits = queryResult.getHits();
        while (hits.hasNext()) {
            final Hit hit = hits.nextHit();
            final String identifier = hit.getSearchDocument().getContentId().toIdentifier();
            try {
                final Node node = jcrSession.getNodeByIdentifier(identifier);
                if (node.isNodeType("hippo:handle")) {
                    final String variantId = resolveVariantId(jcrSession, identifier);
                    if (variantId != null) {
                        variantIds.add(variantId);
                    }
                } else {
                    variantIds.add(identifier);
                }
            } catch (RepositoryException e) {
                log.warn("Could not resolve node for id {}: {}", identifier, e.getMessage());
            }
        }

        log.debug("Collected {} variant ID(s) from query", variantIds.size());
        return variantIds;
    }

    /**
     * Resolves a list of raw identifiers (may be handle or variant UUIDs)
     * to published variant UUIDs.
     */
    private List<String> resolveVariantIdsFromIdentifiers(final List<String> identifiers) {
        final Session jcrSession = ((UserSession) getSession()).getJcrSession();
        final List<String> variantIds = new ArrayList<>();
        for (final String identifier : identifiers) {
            try {
                final Node node = jcrSession.getNodeByIdentifier(identifier);
                if (node.isNodeType("hippo:handle")) {
                    final String variantId = resolveVariantId(jcrSession, identifier);
                    if (variantId != null) {
                        variantIds.add(variantId);
                    }
                } else {
                    variantIds.add(identifier);
                }
            } catch (RepositoryException e) {
                log.warn("Could not resolve node for id {}: {}", identifier, e.getMessage());
            }
        }
        log.debug("Resolved {} variant ID(s) from explicit selection", variantIds.size());
        return variantIds;
    }

    /**
     * Resolves a document handle UUID to the identifier of its published variant,
     * falling back to the first available variant.
     */
    private String resolveVariantId(final Session session, final String handleId) {
        try {
            final Node handle = session.getNodeByIdentifier(handleId);
            final NodeIterator variants = handle.getNodes(handle.getName());
            String published = null;
            String any = null;
            while (variants.hasNext()) {
                final Node v = variants.nextNode();
                if (any == null) {
                    any = v.getIdentifier();
                }
                if (v.hasProperty("hippostd:state")
                        && "published".equals(v.getProperty("hippostd:state").getString())) {
                    published = v.getIdentifier();
                }
            }
            return published != null ? published : any;
        } catch (RepositoryException e) {
            log.warn("Could not resolve variant for handle {}: {}", handleId, e.getMessage());
            return null;
        }
    }
}
