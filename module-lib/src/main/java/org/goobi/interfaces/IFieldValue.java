package org.goobi.interfaces;

import java.util.List;

import org.goobi.production.properties.GeonamesSearchProperty;
import org.goobi.production.properties.GndSearchProperty;
import org.goobi.production.properties.ViafSearchProperty;

public interface IFieldValue extends GndSearchProperty, GeonamesSearchProperty, ViafSearchProperty {

    @Override
    public String getValue();

    public List<String> getMultiselectSelectedValues();

    public IMetadataField getField();

    @Override
    public void setValue(String value);

    public void setMultiselectSelectedValues(List<String> values);

    public void setField(IMetadataField field);

    public List<String> getPossibleValues();

    public String getMultiselectValue();

    public void setMultiselectValue(String value);

    public void removeSelectedValue(String value);

    public String getValuesForXmlExport();

    public List<String> getSelectItemList();

    public String getAuthorityType();

    public void setAuthorityType(String type);

    public String getAuthorityValue();

    public void setAuthorityValue(String value);

    public String getFieldType();

    public String getFirstname();

    public String getLastname();

    public String getMainname();

    public String getSubname();

    public void setFirstname(String value);

    public void setLastname(String value);

    public void setMainname(String value);

    public void setSubname(String value);
}
