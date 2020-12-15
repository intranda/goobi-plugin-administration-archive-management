package de.intranda.goobi.plugins.model;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import lombok.Data;
import lombok.ToString;

@Data

public class EadMetadataField {

    /** contains the internal name of the field. The value can be used to translate the field in the messages files */
    private String name;

    /**
     * metadata level, allowed values are 1-7:
     * <ul>
     * <li>1: metadata for Identity Statement Area</li>
     * <li>2: Context Area</li>
     * <li>3: Content and Structure Area</li>
     * <li>4: Condition of Access and Use Area</li>
     * <li>5: Allied Materials Area</li>
     * <li>6: Note Area</li>
     * <li>7: Description Control Area</li>
     * </ul>
     */
    private Integer level;

    /** contains a relative path to the ead value. The root of the xpath is either the {@code<ead>} element or the {@code<c>} element */
    private String xpath;

    /** type of the xpath return value, can be text, attribute, element (default) */
    private String xpathType;

    /** defines if the field can exist once or multiple times, values can be true/false, default is false */
    private boolean repeatable;

    /** contains the metadata values */
    private List<FieldValue> values;

    /** defines if the field is displayed on the UI, values can be true/false, default is true */
    private boolean visible;

    /** defines if the field is displayed as input field (true) or badge (false, default), affects only visible metadata */
    private boolean showField;

    /** contains the list of possible values for dropdown or multiselect */
    private List<String> selectItemList;

    /** defines the type of the input field. Posible values are input (default), textarea, dropdown, multiselect */
    private String fieldType;

    /** links to the ead node */
    @ToString.Exclude
    private EadEntry eadEntry;

    /** internal name of the metadata field */
    private String metadataName;

    /** defines if this field gets inherited when a child node is created */
    private boolean importMetadataInChild;

    /** defines the validation type, possible values are unique, required, unique+required, regex, regex+required */
    private String validationType;

    /** contains the regular expression used to validate regex or regex+required */
    private String regularExpression;

    /** contains the result of the validation */
    private boolean valid = true;

    /** contains a human readable error text */
    private String validationError;

    public EadMetadataField(String name, Integer level, String xpath, String xpathType, boolean repeatable, boolean visible, boolean showField,
            String fieldType, String metadataName, boolean importMetadataInChild, String validationType, String regularExpression) {
        this.name = name;
        this.level = level;
        this.xpath = xpath;
        this.xpathType = xpathType;
        this.repeatable = repeatable;
        this.visible = visible;
        this.showField = showField;
        this.fieldType = fieldType;
        this.metadataName = metadataName;
        this.importMetadataInChild = importMetadataInChild;
        this.validationType = validationType;
        this.regularExpression = regularExpression;
    }

    public boolean isFilled() {
        if (values == null || values.isEmpty()) {
            return false;
        }
        for (FieldValue val : values) {
            if (StringUtils.isNotBlank(val.getValue())) {
                return true;
            }
        }
        return false;
    }

    public void addFieldValue(FieldValue value) {
        if (values == null) {
            values = new ArrayList<>();
        }
        if (values.isEmpty() || repeatable) {
            values.add(value);
        }
    }

    public void addValue() {
        if (values == null) {
            values = new ArrayList<>();
        }
        if (values.isEmpty() || repeatable) {
            values.add(new FieldValue(this));
        }
    }

    public void deleteValue(FieldValue value) {
        FieldValue valueToDelete = null;
        for (FieldValue fv : values) {
            if (fv.getValue().equals(value.getValue())) {
                valueToDelete = fv;
                break;
            }
        }
        if (valueToDelete!=null) {
            if (values.size()>1) {
                values.remove(valueToDelete);
            } else {
                valueToDelete.setValue("");
                showField = false;
            }
        }

    }

}
