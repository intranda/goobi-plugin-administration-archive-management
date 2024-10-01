package org.goobi.interfaces;

public interface IParameter {

    public String getFieldName();

    public String getPrefix();

    public void setPrefix(String prefix);

    public String getSuffix();

    public void setSuffix(String suffix);

    public boolean isCounter();

    public void setCounter(boolean counter);

    public boolean isGenerated();

    public void setGenerated(boolean counter);

    public String getCounterFormat();

    public void setCounterFormat(String counterFormat);

    public int getCounterStartValue();

    public void setCounterStartValue(int counterStartValue);

    public String getFieldType();

    public void setFieldType(String fieldType);

    public boolean isSelected();

    public void setSelected(boolean selected);

}
