//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.4 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2014.01.21 at 01:10:09 AM CET 
//


package cz.cas.lib.proarc.desa.nsesss2;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * Sada elementů pro zatřídění typového spisu do hierarchie spisového plánu.
 * 
 * <p>Java class for tTrideniTypovehoSpisu complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="tTrideniTypovehoSpisu">
 *   &lt;complexContent>
 *     &lt;extension base="{http://www.mvcr.cz/nsesss/v2}tTrideni">
 *       &lt;sequence>
 *         &lt;element name="MaterskaEntita" type="{http://www.mvcr.cz/nsesss/v2}tMaterskaEntitaTypovehoSpisu"/>
 *       &lt;/sequence>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "tTrideniTypovehoSpisu", namespace = "http://www.mvcr.cz/nsesss/v2", propOrder = {
    "materskaEntita"
})
public class TTrideniTypovehoSpisu
    extends TTrideni
{

    @XmlElement(name = "MaterskaEntita", namespace = "http://www.mvcr.cz/nsesss/v2", required = true)
    protected TMaterskaEntitaTypovehoSpisu materskaEntita;

    /**
     * Gets the value of the materskaEntita property.
     * 
     * @return
     *     possible object is
     *     {@link TMaterskaEntitaTypovehoSpisu }
     *     
     */
    public TMaterskaEntitaTypovehoSpisu getMaterskaEntita() {
        return materskaEntita;
    }

    /**
     * Sets the value of the materskaEntita property.
     * 
     * @param value
     *     allowed object is
     *     {@link TMaterskaEntitaTypovehoSpisu }
     *     
     */
    public void setMaterskaEntita(TMaterskaEntitaTypovehoSpisu value) {
        this.materskaEntita = value;
    }

}