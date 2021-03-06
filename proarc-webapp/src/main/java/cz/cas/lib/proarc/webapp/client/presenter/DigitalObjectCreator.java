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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package cz.cas.lib.proarc.webapp.client.presenter;

import com.google.gwt.place.shared.PlaceController;
import com.smartgwt.client.data.DSCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.rpc.RPCResponse;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.Canvas;
import cz.cas.lib.proarc.common.object.model.DatastreamEditorType;
import cz.cas.lib.proarc.webapp.client.ClientMessages;
import cz.cas.lib.proarc.webapp.client.ClientUtils;
import cz.cas.lib.proarc.webapp.client.ds.DigitalObjectDataSource;
import cz.cas.lib.proarc.webapp.client.ds.MetaModelDataSource.MetaModelRecord;
import cz.cas.lib.proarc.webapp.client.presenter.DigitalObjectEditing.DigitalObjectEditorPlace;
import cz.cas.lib.proarc.webapp.client.widget.NewDigObject;
import cz.cas.lib.proarc.webapp.client.widget.StatusView;
import cz.cas.lib.proarc.webapp.client.widget.Wizard;
import cz.cas.lib.proarc.webapp.client.widget.Wizard.StepKind;
import cz.cas.lib.proarc.webapp.client.widget.Wizard.WizardStep;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Creates new digital object.
 *
 * @author Jan Pokorsky
 */
public final class DigitalObjectCreator {

    private static final Logger LOG = Logger.getLogger(DigitalObjectCreator.class.getName());

    private final NewDigObjectStep newDigObjectStep;
    private final Wizard wizard;
    private WizardContext wc;
    private final ClientMessages i18n;
    private String modelId;
    private String parentPid;
    private final PlaceController places;

    public DigitalObjectCreator(ClientMessages i18n, PlaceController places) {
        this.i18n = i18n;
        this.places = places;
        newDigObjectStep = new NewDigObjectStep();
        wizard = new Wizard(i18n, newDigObjectStep, Wizard.emptyStep());
    }

    public void newObject() {
        newObject(null, null);
    }
    
    public void newObject(String modelId, String parentPid) {
        // bind object editor ui
        this.modelId = modelId;
        this.parentPid = parentPid;
        wizard.setShowButtons(false);
        wizard.moveAt(newDigObjectStep);
    }

    public Canvas getUI() {
        return wizard;
    }
    
    private void initContext() {
        this.wc = new WizardContext();
        wc.setParentPid(parentPid);
    }
    
    private WizardContext getContext() {
        return wc;
    }

    private static final class WizardContext {
        private String pid;
        private MetaModelRecord model;
        private String parentPid;
        private boolean modsInitialized;
        private boolean dcInitialized;

        public boolean isDcInitialized() {
            return dcInitialized;
        }

        public void setDcInitialized(boolean dcInitialized) {
            this.dcInitialized = dcInitialized;
        }

        public MetaModelRecord getModel() {
            return model;
        }

        public void setModel(MetaModelRecord model) {
            this.model = model;
        }

        public boolean isModsInitialized() {
            return modsInitialized;
        }

        public void setModsInitialized(boolean modsInitialized) {
            this.modsInitialized = modsInitialized;
        }

        public String getParentPid() {
            return parentPid;
        }

        public void setParentPid(String parentPid) {
            this.parentPid = parentPid;
        }

        public String getPid() {
            return pid;
        }

        public void setPid(String pid) {
            this.pid = pid;
        }

    }

    private final class NewDigObjectStep implements WizardStep, NewDigObject.Handler {

        private NewDigObject newDigObject;
        private Wizard wizard;

        @Override
        public void onShow(Wizard wizard) {
            this.wizard = wizard;
            initContext();
            wizard.setWizardLabel(i18n.DigitalObjectCreator_DescriptionPrefix_Title(),
                    i18n.DigitalObjectCreator_NewDigObjectStep_Description_Title());
            newDigObject.setHandler(this);
            newDigObject.bind(modelId, null);
        }

        @Override
        public void onHide(Wizard wizard) {
            newDigObject.setHandler(null);
            this.wizard = null;
        }

        @Override
        public boolean onStepAction(Wizard wizard, StepKind step) {
            if (step == StepKind.FORWARD) {
                if (!newDigObject.validate()) {
                    return false;
                }
                WizardContext wc = getContext();
                MetaModelRecord model = newDigObject.getModel();
                wc.setModel(model);
                String mods = newDigObject.getMods();
                String newPid = newDigObject.getNewPid();
                ClientUtils.fine(LOG, "NewDigObjectStep.onStepAction.FORWARD: model: %s pid: %s",
                        model.getId(), newPid);
                saveNewDigitalObject(model.getId(), newPid, mods);
                return false;

            }
            return true;
        }

        @Override
        public void onCreateObject() {
            onStepAction(wizard, StepKind.FORWARD);
        }

        private void saveNewDigitalObject(String modelId, String pid, String mods) {
            Record r = new Record();
            DigitalObjectDataSource ds = DigitalObjectDataSource.getInstance();
            r.setAttribute(DigitalObjectDataSource.FIELD_MODEL, modelId);
            if (mods != null) {
                r.setAttribute(DigitalObjectDataSource.FIELD_MODS, mods);
            }
            if (pid != null && !pid.isEmpty()) {
                r.setAttribute(DigitalObjectDataSource.FIELD_PID, pid);
            }
            DSRequest dsRequest = new DSRequest();
            dsRequest.setWillHandleError(true);
            ds.addData(r, new DSCallback() {

                @Override
                public void execute(DSResponse response, Object rawData, DSRequest request) {
                    if (response.getStatus() == RPCResponse.STATUS_VALIDATION_ERROR) {
                        Map errors = response.getErrors();
                        newDigObject.setValidationErrors(errors);
                        request.setWillHandleError(true);
                        return ;
                    }
                    if (response.getHttpResponseCode() >= 400) {
                        // failure
                        SC.warn("Failed to create digital object!");
                    } else {
                        Record[] data = response.getData();
                        if (data != null && data.length > 0) {
                            String pid = data[0].getAttribute(DigitalObjectDataSource.FIELD_PID);
                            // here should be updated DigitalObject data stream
                            // caches to prevent server round-trip delays
                            StatusView.getInstance().show(i18n.DigitalObjectCreator_FinishedStep_Done_Msg());
                            places.goTo(new DigitalObjectEditorPlace(DatastreamEditorType.MODS, pid));
                        } else {
                            SC.warn("Failed to create digital object!");
                        }
                    }
                }
            }, dsRequest);
        }

        @Override
        public Canvas asWidget() {
            if (newDigObject == null) {
                newDigObject = new NewDigObject(i18n);
            }
            return newDigObject;
        }
    }


}
