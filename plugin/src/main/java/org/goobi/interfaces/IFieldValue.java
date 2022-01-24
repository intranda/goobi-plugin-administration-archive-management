package org.goobi.interfaces;

import java.util.List;

public interface IFieldValue {

    public String getValue();

    public List<String> getMultiselectSelectedValues();

    public IMetadataField getField();

    public void setValue(String value);

    public void setMultiselectSelectedValues(List<String> values);

    public void setField(IMetadataField field);

    public List<String> getPossibleValues();

    public String getMultiselectValue();

    public void setMultiselectValue(String value);

    public void removeSelectedValue(String value);

    public String getValuesForXmlExport();

    public List<String> getSelectItemList();
}
