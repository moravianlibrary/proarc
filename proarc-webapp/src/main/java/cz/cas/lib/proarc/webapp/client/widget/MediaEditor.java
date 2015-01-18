/*
 * Copyright (C) 2012 Jan Pokorsky
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package cz.cas.lib.proarc.webapp.client.widget;

import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.data.ResultSet;
import com.smartgwt.client.data.SortSpecifier;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.SortDirection;
import com.smartgwt.client.util.BooleanCallback;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.form.ColorPicker;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.events.ColorSelectedEvent;
import com.smartgwt.client.widgets.form.events.ColorSelectedHandler;
import com.smartgwt.client.widgets.form.fields.SelectItem;
import com.smartgwt.client.widgets.form.fields.events.ChangedEvent;
import com.smartgwt.client.widgets.form.fields.events.ChangedHandler;
import com.smartgwt.client.widgets.form.fields.events.DataArrivedEvent;
import com.smartgwt.client.widgets.form.fields.events.DataArrivedHandler;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import cz.cas.lib.proarc.webapp.client.ClientMessages;
import cz.cas.lib.proarc.webapp.client.ClientUtils;
import cz.cas.lib.proarc.webapp.client.action.AbstractAction;
import cz.cas.lib.proarc.webapp.client.action.Action;
import cz.cas.lib.proarc.webapp.client.action.ActionEvent;
import cz.cas.lib.proarc.webapp.client.action.Actions;
import cz.cas.lib.proarc.webapp.client.action.RefreshAction.Refreshable;
import cz.cas.lib.proarc.webapp.client.ds.DigitalObjectDataSource.DigitalObject;
import cz.cas.lib.proarc.webapp.client.ds.MetaModelDataSource.MetaModelRecord;
import cz.cas.lib.proarc.webapp.client.ds.RelationDataSource;
import cz.cas.lib.proarc.webapp.client.ds.RestConfig;
import cz.cas.lib.proarc.webapp.client.ds.StreamProfileDataSource;
import cz.cas.lib.proarc.webapp.client.ds.StreamProfileDataSource.StreamProfile;
import cz.cas.lib.proarc.webapp.shared.rest.DigitalObjectResourceApi;
import java.util.ArrayList;

/**
 * Edits data streams containing digitized multimedia content.
 *
 * @author Jan Pokorsky
 */
public final class MediaEditor implements DatastreamEditor, Refreshable {

    private final ClientMessages i18n;
    private final DigitalObjectPreview doPreview;
    private String imgParams;
    private final ArrayList<Canvas> backgroundListeners = new ArrayList<Canvas>();
    private Action fullAction;
    private AbstractAction backgroundAction;
    private AbstractAction uploadAction;
    private DigitalObject digitalObject;
    private SelectItem streamMenu;

    public MediaEditor(ClientMessages i18n) {
        this.i18n = i18n;
        doPreview = new DigitalObjectPreview(i18n);
        initActions(i18n);
    }

    @Override
    public void edit(DigitalObject digitalObject) {
        if (digitalObject == null) {
            throw new NullPointerException();
        }
        this.digitalObject = digitalObject;
        updateStreamMenu(digitalObject);
    }

    @Override
    public void focus() {
        // no-op
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getCapability(Class<T> clazz) {
        T c = null;
        if (Refreshable.class.equals(clazz)) {
            c = (T) this;
        }
        return c;
    }

    @Override
    public Canvas[] getToolbarItems() {
        Canvas zoomer = doPreview.getPreviewZoomer();
        zoomer.setWidth(100);
        return new Canvas[] {
            Actions.asIconButton(fullAction, this),
            Actions.asIconButton(uploadAction, this),
            Actions.asIconButton(backgroundAction, this), zoomer,
            createStreamMenu(),
        };
    }

    @Override
    public Canvas getUI() {
        return doPreview.asCanvas();
    }

    @Override
    public void refresh() {
        edit(digitalObject);
    }

    public void addBackgroundColorListeners(Canvas c) {
        backgroundListeners.add(c);
    }

    public void setBackgroundColor(String color) {
        doPreview.setBackgroundColor(color);
        for (Canvas canvas : backgroundListeners) {
            canvas.setBackgroundColor(color);
        }
    }

    private void initActions(final ClientMessages i18n) {
        fullAction = new AbstractAction(
                i18n.DigitalObjectPreview_ViewFullAction_Title(),
                "[SKIN]/actions/view.png",
                i18n.DigitalObjectPreview_ViewFullAction_Hint()) {

            @Override
            public void performAction(ActionEvent event) {
                doPreview.showInWindow(getObjectTitle());
            }
        };

        backgroundAction = new AbstractAction(
                i18n.DigitalObjectPreview_ColorChooserAction_Title(),
                "[SKIN]/actions/color_swatch.png",
                i18n.DigitalObjectPreview_ColorChooserAction_Hint()) {

            @Override
            public void performAction(ActionEvent event) {
                ColorPicker picker = new ColorPicker();
                picker.addColorSelectedHandler(new ColorSelectedHandler() {

                    @Override
                    public void onColorSelected(ColorSelectedEvent event) {
                        setBackgroundColor(event.getColor());
                    }
                });
                picker.setDefaultColor(DigitalObjectPreview.BACKGROUND_COLOR);
                picker.setKeepInParentRect(true);
                picker.show();
            }
        };

        uploadAction = new AbstractAction(
                i18n.DigitalObjectEditor_MediaEditor_UploaderAction_Title(),
                "[SKIN]/MultiUploadItem/icon_add_files.png",
                i18n.DigitalObjectEditor_MediaEditor_UploaderAction_Hint()) {
            @Override
            public void performAction(ActionEvent event) {
                UploadFile uploadFile = new UploadFile(i18n);
                uploadFile.showWindow(digitalObject, new BooleanCallback() {

                    @Override
                    public void execute(Boolean value) {
                        if (value != null && value) {
                            RelationDataSource.getInstance().fireRelationChange(digitalObject.getPid());
                            refresh();
                        }
                    }
                });
            }
        };
    }

    private DynamicForm createStreamMenu() {
        streamMenu = new SelectItem();
        streamMenu.setShowTitle(Boolean.FALSE);
        streamMenu.setWidth(100);
        streamMenu.setPickListWidth(150);
        streamMenu.addChangedHandler(new ChangedHandler() {

            @Override
            public void onChanged(ChangedEvent event) {
                showStream();
            }
        });
        streamMenu.setAutoFetchData(Boolean.FALSE);
        streamMenu.setOptionDataSource(StreamProfileDataSource.getInstance());
        SortSpecifier sort = new SortSpecifier(StreamProfileDataSource.FIELD_ORDER, SortDirection.ASCENDING);
        streamMenu.setPickListSort(new SortSpecifier[] {sort});
        streamMenu.setDisplayField(StreamProfileDataSource.FIELD_LABEL);
        streamMenu.setValueField(StreamProfileDataSource.FIELD_ID);
        streamMenu.addDataArrivedHandler(new DataArrivedHandler() {

            @Override
            public void onDataArrived(DataArrivedEvent event) {
                updateStreamMenu(event.getData(), streamMenu);
            }
        });

        DynamicForm form = new DynamicForm();
        form.setFields(streamMenu);
        form.setLayoutAlign(Alignment.CENTER);
        return form;
    }

    private void updateStreamMenu(DigitalObject dobj) {
        Criteria streamMenuFilter = dobj.toCriteria();
        streamMenu.setPickListCriteria(streamMenuFilter);
        streamMenu.fetchData();
    }

    private void updateStreamMenu(ResultSet data, SelectItem view) {
        ListGridRecord lastViewSelection = view.getSelectedRecord();
        Boolean contains = lastViewSelection == null ? false : data.contains(lastViewSelection);
        if (!contains) {
            String dsId = data.isEmpty() ? null : data.get(0).getAttribute(StreamProfileDataSource.FIELD_ID);
            view.setValue(dsId);
        }
        showStream();
    }

    private void showStream() {
        StreamProfile stream = StreamProfile.get(streamMenu.getSelectedRecord());
        if (stream != null) {
            StringBuilder sb = new StringBuilder();
            sb.append(DigitalObjectResourceApi.DIGITALOBJECT_PID).append('=')
                    .append(digitalObject.getPid())
                    .append('&').append(DigitalObjectResourceApi.DISSEMINATION_DATASTREAM)
                    .append('=').append(stream.getId());
            String batchId = digitalObject.getBatchId();
            if (batchId != null) {
                sb.append('&').append(DigitalObjectResourceApi.BATCHID_PARAM).append('=').append(batchId);
            }
            imgParams = sb.toString();
            String previewUrl = buildResourceUrl(RestConfig.URL_DIGOBJECT_DISSEMINATION, imgParams);
            doPreview.show(previewUrl, stream.getMime());
        } else {
            doPreview.show(null);
        }
    }

    private String getObjectTitle() {
        String label = digitalObject.getRecord().getAttribute(DigitalObjectResourceApi.MEMBERS_ITEM_LABEL);
        MetaModelRecord model = digitalObject.getModel();
        String modelName = null;
        if (model != null) {
            modelName = model.getDisplayName();
        }
        StringBuilder sb = new StringBuilder();
        sb.append(i18n.DigitalObjectPreview_Window_Title());
        if (modelName != null) {
            sb.append(" - ").append(modelName);
        }
        if (label != null) {
            sb.append(": ").append(label);
        }
        return sb.toString();
    }

    /**
     *
     * @param datastreamUrl data stream URL
     * @param objectParams PID and batch ID as URL parameters
     * @return URL
     */
    private static String buildResourceUrl(String datastreamUrl, String objectParams) {
        String url = ClientUtils.format("%s?%s", datastreamUrl, objectParams);
        return url;
    }

}
