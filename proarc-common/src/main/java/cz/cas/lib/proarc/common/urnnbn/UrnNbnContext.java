/*
 * Copyright (C) 2014 Jan Pokorsky
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
package cz.cas.lib.proarc.common.urnnbn;

import cz.cas.lib.proarc.common.export.mets.JhoveContext;
import cz.cas.lib.proarc.urnnbn.ResolverClient;

/**
 *
 * @author Jan Pokorsky
 */
public class UrnNbnContext {

    private UrnNbnStatusHandler status;
    private ResolverClient client;
    private JhoveContext jhoveContext;

    public ResolverClient getClient() {
        return client;
    }

    public void setClient(ResolverClient client) {
        this.client = client;
    }

    public UrnNbnStatusHandler getStatus() {
        return status == null ? UrnNbnStatusHandler.DEFAULT : status;
    }

    public void setStatus(UrnNbnStatusHandler status) {
        this.status = status;
    }

    public JhoveContext getJhoveContext() {
        return jhoveContext;
    }

    public void setJhoveContext(JhoveContext jhoveContext) {
        this.jhoveContext = jhoveContext;
    }

}
