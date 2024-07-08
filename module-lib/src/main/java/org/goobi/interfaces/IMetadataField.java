package org.goobi.interfaces;

import java.util.List;

public interface IMetadataField {

    public String getName();

    public Integer getLevel();

    public String getXpath();

    public String getXpathType();

    public boolean isRepeatable();

    public List<IFieldValue> getValues();

    public void setName(String value);

    public void setLevel(Integer value);

    public void setXpath(String value);

    public void setXpathType(String value);

    public void setRepeatable(boolean value);

    public void setValues(List<IFieldValue> values);

    public boolean isVisible();

    public boolean isValid();

    public void setVisible(boolean value);

    public boolean isShowField();

    public void setShowField(boolean value);

    public void setValid(boolean value);

    public List<String> getSelectItemList();

    public String getFieldType();

    public IEadEntry getEadEntry();

    public String getMetadataName();

    public boolean isImportMetadataInChild();

    public String getValidationType();

    public String getRegularExpression();

    public String getValidationError();

    public void setSelectItemList(List<String> values);

    public void setFieldType(String value);

    public void setEadEntry(IEadEntry value);

    public void setMetadataName(String value);

    public void setImportMetadataInChild(boolean value);

    public void setValidationType(String value);

    public void setRegularExpression(String value);

    public void setValidationError(String value);

    public boolean isFilled();

    public void addFieldValue(IFieldValue value);

    public void addValue();

    public void deleteValue(IFieldValue value);

    public boolean isSearchable();

    public IMetadataField copy(String prefix, String suffix);

    public IFieldValue createFieldValue();

    public String getViafSearchFields();

    public String getViafDisplayFields();

    // metadata groups / composite fields
    public boolean isGroup();

    public void setGroup(boolean group);

    public List<IMetadataGroup> getGroups();

    public void setGroups(List<IMetadataGroup> groups);

    public IMetadataGroup createGroup();

    public void addGroup(IMetadataGroup group);

    public void deleteGroup(IMetadataGroup group);

}
