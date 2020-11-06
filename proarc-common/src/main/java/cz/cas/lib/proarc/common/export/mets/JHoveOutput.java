/*
 * Copyright (C) 2014 Robert Simonovsky
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

package cz.cas.lib.proarc.common.export.mets;

import cz.cas.lib.proarc.aes57.Aes57Utils;
import cz.cas.lib.proarc.mix.Mix;
import cz.cas.lib.proarc.mix.MixUtils;
import org.aes.audioobject.AudioObject;
import org.w3c.dom.Node;
import javax.xml.transform.dom.DOMResult;

public class JHoveOutput {
    String formatVersion;

    public String getFormatVersion() {
        return formatVersion;
    }

    public void setFormatVersion(String formatVersion) {
        this.formatVersion = formatVersion;
    }

    public Node getMixNode() {
        if (this.mix != null) {
            DOMResult result = new DOMResult();
            MixUtils.marshal(result, mix, true);
            return result.getNode().getFirstChild();
        }
        return null;
    }

    public void setMixNode(Node mixNode) {
        this.mixNode = mixNode;
    }

    Node mixNode;
    Mix mix;

    public Mix getMix() {
        return mix;
    }

    public void setMix(Mix mix) {
        this.mix = mix;
    }

    Node aesNode;
    AudioObject aes;

    public Node getAesNode() {
        if (this.aes != null) {
            DOMResult result = new DOMResult();
            Aes57Utils.marshal(result, aes, true);
            return result.getNode().getFirstChild();
        }
        return null;
    }

    public void setAesNode(Node aesNode) {
        this.aesNode = aesNode;
    }

    public AudioObject getAes() {
        return  aes;
    }

    public void setAes(AudioObject aes) {
        this.aes = aes;
    }
}
