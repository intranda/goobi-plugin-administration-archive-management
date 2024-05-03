package de.intranda.goobi.plugins.model;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.goobi.interfaces.IFieldValue;
import org.goobi.interfaces.IMetadataField;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class FieldValue implements IFieldValue {

    private String value;
    /** contains the list of selected values in multiselect */
    private List<String> multiselectSelectedValues = new ArrayList<>();
    @ToString.Exclude
    private IMetadataField field;

    public FieldValue(IMetadataField field) {
        this.field = field;
    }

    @Override
    public List<String> getPossibleValues() {
        List<String> answer = new ArrayList<>();
        for (String possibleValue : field.getSelectItemList()) {
            if (!multiselectSelectedValues.contains(possibleValue)) {
                answer.add(possibleValue);
            }
        }
        return answer;
    }

    @Override
    public String getMultiselectValue() {
        return "";
    }

    @Override
    public void setMultiselectValue(String value) {
        if (StringUtils.isNotBlank(value)) {
            multiselectSelectedValues.add(value);
        }
    }

    @Override
    public void removeSelectedValue(String value) {
        multiselectSelectedValues.remove(value);
    }

    @Override
    public String getValuesForXmlExport() {
        if (StringUtils.isBlank(field.getXpath())) {
            return null;
        } else if (!multiselectSelectedValues.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (String selectedValue : multiselectSelectedValues) {
                if (sb.length() > 0) {
                    sb.append("; ");
                }
                sb.append(selectedValue);
            }
            return sb.toString();
        } else {
            return value;
        }
    }

    @Override
    public List<String> getSelectItemList() {
        return field.getSelectItemList();
    }

    @Override
    public void setValue(String value) {
        this.value = value;
        if (field.getXpath()!= null && field.getXpath().contains("unittitle")) {
            field.getEadEntry().setLabel(value);
        }

    }

}
