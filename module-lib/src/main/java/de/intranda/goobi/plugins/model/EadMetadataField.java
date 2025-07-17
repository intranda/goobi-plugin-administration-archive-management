package de.intranda.goobi.plugins.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.goobi.interfaces.IEadEntry;
import org.goobi.interfaces.IFieldValue;
import org.goobi.interfaces.IMetadataField;
import org.goobi.interfaces.IMetadataGroup;

import lombok.Data;
import lombok.ToString;

@Data
public class EadMetadataField implements IMetadataField {

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
    private List<IFieldValue> values;

    /** defines if the field is displayed on the UI, values can be true/false, default is true */
    private boolean visible;

    /** defines if the field is displayed as input field (true) or badge (false, default), affects only visible metadata */
    private boolean showField;

    /** contains the list of possible values for dropdown or multiselect */
    private List<String> selectItemList;

    private String vocabularyName;
    private List<String> searchParameter;

    /** defines the type of the input field. Posible values are input (default), textarea, dropdown, multiselect */
    private String fieldType;

    /** links to the ead node */
    @ToString.Exclude
    private IEadEntry eadEntry;

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

    private boolean searchable;

    private String viafSearchFields;
    private String viafDisplayFields;

    // metadata groups
    private List<IMetadataField> subfields = new ArrayList<>();

    private boolean group;
    private List<IMetadataGroup> groups = new ArrayList<>();

    private Map<String, String> subfieldMap;

    public EadMetadataField(String name, Integer level, String xpath, String xpathType, boolean repeatable, boolean visible, boolean showField, //NOSONAR
            String fieldType, String metadataName, boolean importMetadataInChild, String validationType, String regularExpression,
            boolean searchable, String viafSearchFields, String viafDisplayFields, boolean group, String vocabularyName) {
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
        this.searchable = searchable;
        this.viafSearchFields = viafSearchFields;
        this.viafDisplayFields = viafDisplayFields;
        this.group = group;
        this.vocabularyName = vocabularyName;
    }

    @Override
    public boolean isFilled() {
        if (isGroup()) {
            for (IMetadataGroup grp : groups) {
                for (IMetadataField f : grp.getFields()) {
                    if (f.isFilled()) {
                        return true;
                    }
                }
            }
        }
        if (values == null || values.isEmpty()) {
            return false;
        }
        for (IFieldValue val : values) {
            if (StringUtils.isNotBlank(val.getValue()) || StringUtils.isNotBlank(val.getFirstname()) || StringUtils.isNotBlank(val.getLastname())
                    || StringUtils.isNotBlank(val.getMainName())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void addFieldValue(IFieldValue value) {
        if (values == null) {
            values = new ArrayList<>();
        }
        if (values.isEmpty() || repeatable) {
            values.add(value);
        }
    }

    @Override
    public void addValue() {
        if (values == null) {
            values = new ArrayList<>();
        }
        if (values.isEmpty() || repeatable) {
            values.add(new FieldValue(this));
        }
    }

    @Override
    public void deleteValue(IFieldValue value) {
        IFieldValue valueToDelete = null;
        for (IFieldValue fv : values) {
            if ("person".equals(fieldType)) {
                if (((fv.getLastname() == null && value.getLastname() == null)
                        || fv.getLastname() != null && fv.getLastname().equals(value.getLastname()))
                        && ((fv.getFirstname() == null && value.getFirstname() == null)
                                || fv.getFirstname() != null && fv.getFirstname().equals(value.getFirstname()))) {
                    valueToDelete = fv;
                    break;
                }
            } else if ("corporate".equals(fieldType)) {
                if (((fv.getMainName() == null && value.getMainName() == null)
                        || (fv.getMainName() != null && fv.getMainName().equals(value.getMainName())))
                        && ((fv.getSubName() == null && value.getSubName() == null)
                                || (fv.getSubName() != null && fv.getSubName().equals(value.getSubName())))
                        && ((fv.getPartName() == null && value.getPartName() == null)
                                || (fv.getPartName() != null && fv.getPartName().equals(value.getPartName())))) {
                    valueToDelete = fv;
                    break;
                }

            } else if ((fv.getValue() == null && value.getValue() == null) || (fv.getValue() != null && fv.getValue().equals(value.getValue()))) {
                valueToDelete = fv;
                break;
            }
        }
        if (valueToDelete != null) {
            if (values.size() > 1) {
                values.remove(valueToDelete);
            } else {
                valueToDelete.setValue("");
                valueToDelete.setAuthorityValue("");
                valueToDelete.setAuthorityType("");
                showField = false;
            }
        }

    }

    @Override
    public IMetadataField copy(String prefix, String suffix, boolean copyValue) {
        IMetadataField field = new EadMetadataField(name, level, xpath, xpathType, repeatable, visible, showField,
                fieldType, metadataName, importMetadataInChild, validationType, regularExpression, searchable, viafSearchFields, viafDisplayFields,
                group, vocabularyName);
        field.setSelectItemList(selectItemList);
        field.setSearchParameter(searchParameter);
        field.setSubfieldMap(subfieldMap);
        if (isGroup()) {
            for (IMetadataGroup grp : groups) {
                IMetadataGroup newGroup = new EadMetadataGroup(this);
                field.getGroups().add(newGroup);
                for (IMetadataField f : grp.getFields()) {
                    IMetadataField newSubfield = f.copy(prefix, suffix, copyValue);
                    newGroup.getFields().add(newSubfield);
                }
            }
        } else if (copyValue) {
            for (IFieldValue val : values) {

                IFieldValue newValue = new FieldValue(field);
                if ("person".equals(fieldType)) {
                    newValue.setFirstname(val.getFirstname());
                    newValue.setLastname(val.getLastname());
                } else if ("corporate".equals(fieldType)) {
                    newValue.setMainName(val.getMainName());
                    newValue.setSubName(val.getSubName());
                    newValue.setPartName(val.getPartName());
                } else if (val.getValue() != null) {
                    newValue.setValue(prefix + val.getValue() + suffix);
                } else {
                    newValue.setValue(prefix + suffix);
                }
                newValue.setAuthorityType(val.getAuthorityType());
                newValue.setAuthorityValue(val.getAuthorityValue());
                field.addFieldValue(newValue);
            }
        } else {
            IFieldValue newValue = new FieldValue(field);
            field.addFieldValue(newValue);
        }
        return field;
    }

    @Override
    public IFieldValue createFieldValue() {
        return new FieldValue(this);
    }

    @Override
    public void addSubfield(IMetadataField field) {
        subfields.add(field);
    }

    @Override
    public IMetadataGroup createGroup() {
        if (!groups.isEmpty() && !isFilled()) {
            return groups.get(0);
        }

        IMetadataGroup newGroup = new EadMetadataGroup(this);

        for (IMetadataField f : subfields) {

            IMetadataField field = new EadMetadataField(f.getName(), f.getLevel(), f.getXpath(), f.getXpathType(), f.isRepeatable(),
                    f.isVisible(), f.isShowField(), f.getFieldType(), f.getMetadataName(), f.isImportMetadataInChild(), f.getValidationType(),
                    f.getRegularExpression(), f.isSearchable(), f.getViafSearchFields(), f.getViafDisplayFields(), f.isGroup(),
                    f.getVocabularyName());
            field.setSelectItemList(f.getSelectItemList());
            field.setSearchParameter(f.getSearchParameter());
            field.setSubfieldMap(subfieldMap);
            field.addValue();

            newGroup.getFields().add(field);

        }
        groups.add(newGroup);
        return newGroup;

    }

    @Override
    public void addGroup(IMetadataGroup group) {
        groups.add(group);
    }

    @Override
    public void deleteGroup(IMetadataGroup group) {
        // if more than one group exists, remove it
        if (groups.size() > 1) {
            groups.remove(group);
        } else {
            // otherwise clear all fields
            for (IMetadataField f : group.getFields()) {
                if (f.getValues() != null) {
                    for (IFieldValue val : f.getValues()) {
                        f.deleteValue(val);
                    }
                }
            }
            showField = false;
        }

    }
}
