package org.goobi.interfaces;

import java.util.List;

import org.goobi.production.properties.GndSearchProperty;

public interface IFieldValue extends GndSearchProperty {

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
}
