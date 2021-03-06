/*
 * Copyright (C) 2015 Jan Pokorsky
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

import com.smartgwt.client.data.AdvancedCriteria;
import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.types.JSONDateFormat;
import com.smartgwt.client.util.JSONEncoder;
import com.smartgwt.client.util.Offline;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.events.FilterEditorSubmitEvent;
import com.smartgwt.client.widgets.grid.events.FilterEditorSubmitHandler;
import com.smartgwt.client.widgets.grid.events.ViewStateChangedEvent;
import com.smartgwt.client.widgets.grid.events.ViewStateChangedHandler;

/**
 * The helper that keeps track of {@link ListGrid} properties.
 *
 * @author Jan Pokorsky
 */
public class ListGridPersistance {

    private static final String VIEW = ".view";
    private static final String CRITERIA = ".criteria";
    private final String dbPrefix;
    private final ListGrid grid;
    private static final JSONEncoder JSON_ENCODER = new JSONEncoder();
    static {
        JSON_ENCODER.setDateFormat(JSONDateFormat.DATE_CONSTRUCTOR);
    }

    public ListGridPersistance(String dbPrefix, ListGrid grid) {
        this.dbPrefix = dbPrefix;
        this.grid = grid;
        grid.addViewStateChangedHandler(new ViewStateChangedHandler() {

            @Override
            public void onViewStateChanged(ViewStateChangedEvent event) {
                setViewState();
            }
        });
        grid.addFilterEditorSubmitHandler(new FilterEditorSubmitHandler() {

            @Override
            public void onFilterEditorSubmit(FilterEditorSubmitEvent event) {
                setFilterCriteria(event.getCriteria());
            }
        });
    }

    /**
     * See {@link ListGrid#getViewState() }.
     */
    public String getViewState() {
        return (String) Offline.get(dbPrefix + VIEW);
    }

    /**
     * Returns criteria for an initial fetch.
     */
    public Criteria getFilterCriteria() {
        String criteriaState = (String) Offline.get(dbPrefix + CRITERIA);
        Criteria criteria = null;
        if (criteriaState != null) {
            criteria = AdvancedCriteria.fromJSON(criteriaState);
        }
        return criteria;
    }

    public void setViewState() {
        String viewState = grid.getViewState();
        Offline.put(dbPrefix + VIEW, viewState);
    }

    public void setFilterCriteria(Criteria criteria) {
        if (criteria != null) {
            String json = JSON_ENCODER.encode(criteria);
            Offline.put(dbPrefix + CRITERIA, json);
        }
    }
}
