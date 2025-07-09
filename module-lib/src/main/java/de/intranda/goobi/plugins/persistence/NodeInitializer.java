package de.intranda.goobi.plugins.persistence;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.goobi.interfaces.IEadEntry;
import org.goobi.interfaces.IFieldValue;
import org.goobi.interfaces.IMetadataField;
import org.goobi.interfaces.IMetadataGroup;
import org.goobi.interfaces.IValue;
import org.goobi.model.ExtendendValue;
import org.goobi.model.GroupValue;
import org.goobi.model.PersonValue;

import de.intranda.goobi.plugins.model.EadMetadataField;
import de.intranda.goobi.plugins.model.FieldValue;

public class NodeInitializer {

    public static IEadEntry initEadNodeWithMetadata(IEadEntry entry, List<IMetadataField> configuredFields) {
        if (entry != null) {
            Map<String, List<IValue>> metadata = ArchiveManagementManager.loadMetadataForNode(entry.getDatabaseId());
            // TODO person,corp
            for (IMetadataField emf : configuredFields) {
                if (emf.isGroup()) {
                    List<IValue> groups = metadata.get(emf.getName());
                    loadGroupMetadata(entry, emf, groups);
                } else {
                    List<IValue> values = metadata.get(emf.getName());
                    IMetadataField toAdd = addFieldToEntry(entry, emf, values);
                    addFieldToNode(entry, toAdd);
                }
            }
            entry.calculateFingerprint();
        }

        return entry;
    }

    public static void loadGroupMetadata(IEadEntry entry, IMetadataField template, List<IValue> groups) {
        IMetadataField instance = new EadMetadataField(template.getName(), template.getLevel(), template.getXpath(), template.getXpathType(),
                template.isRepeatable(),
                template.isVisible(), template.isShowField(), template.getFieldType(), template.getMetadataName(), template.isImportMetadataInChild(),
                template.getValidationType(),
                template.getRegularExpression(), template.isSearchable(), template.getViafSearchFields(), template.getViafDisplayFields(),
                template.isGroup(), template.getVocabularyName());
        instance.setValidationError(template.getValidationError());
        instance.setSelectItemList(template.getSelectItemList());
        instance.setSearchParameter(template.getSearchParameter());
        instance.setEadEntry(entry);
        // TODO person,corp
        // sub fields
        for (IMetadataField sub : template.getSubfields()) {
            IMetadataField toAdd = addFieldToEntry(entry, sub, null);
            instance.getSubfields().add(toAdd);
        }

        addGroupData(instance, groups);
        addFieldToNode(entry, instance);
    }

    /**
     * Add the metadata to the configured level
     */

    public static IMetadataField addFieldToEntry(IEadEntry entry, IMetadataField emf, List<IValue> values) {

        IMetadataField toAdd = new EadMetadataField(emf.getName(), emf.getLevel(), emf.getXpath(), emf.getXpathType(), emf.isRepeatable(),
                emf.isVisible(), emf.isShowField(), emf.getFieldType(), emf.getMetadataName(), emf.isImportMetadataInChild(), emf.getValidationType(),
                emf.getRegularExpression(), emf.isSearchable(), emf.getViafSearchFields(), emf.getViafDisplayFields(), emf.isGroup(),
                emf.getVocabularyName());
        toAdd.setValidationError(emf.getValidationError());
        toAdd.setSelectItemList(emf.getSelectItemList());
        toAdd.setSearchParameter(emf.getSearchParameter());
        if (entry != null) {
            if (StringUtils.isBlank(entry.getLabel()) && emf.getXpath().contains("unittitle") && values != null && !values.isEmpty()) {
                IValue value = values.get(0);
                entry.setLabel(((ExtendendValue) value).getValue());
            }
            toAdd.setEadEntry(entry);
        }

        if (values != null && !values.isEmpty()) {
            toAdd.setShowField(true);

            // split single value into multiple fields
            for (IValue value : values) {
                // TODO person,corp
                if (value instanceof ExtendendValue) {

                    ExtendendValue val = (ExtendendValue) value;

                    IFieldValue fv = new FieldValue(toAdd);
                    String stringValue = val.getValue();
                    fv.setAuthorityType(val.getAuthorityType());
                    fv.setAuthorityValue(val.getAuthorityValue());

                    if ("multiselect".equals(toAdd.getFieldType()) && StringUtils.isNotBlank(stringValue)) {
                        String[] splittedValues = stringValue.split("; ");
                        for (String s : splittedValues) {
                            fv.setMultiselectValue(s);
                        }
                    } else {
                        fv.setValue(stringValue);
                    }
                    toAdd.addFieldValue(fv);
                } else if (value instanceof PersonValue) {
                    PersonValue pv = (PersonValue) value;

                    IFieldValue fv = new FieldValue(toAdd);
                    fv.setFirstname(pv.getFirstname());
                    fv.setLastname(pv.getLastname());
                    fv.setAuthorityType(pv.getAuthorityType());
                    fv.setAuthorityValue(pv.getAuthorityValue());
                    toAdd.addFieldValue(fv);
                }
            }
        } else {
            IFieldValue fv = new FieldValue(toAdd);
            toAdd.addFieldValue(fv);
        }
        return toAdd;

    }

    public static void addFieldToNode(IEadEntry entry, IMetadataField toAdd) {
        switch (toAdd.getLevel()) {
            case 1:
                entry.getIdentityStatementAreaList().add(toAdd);
                break;
            case 2:
                entry.getContextAreaList().add(toAdd);
                break;
            case 3:
                entry.getContentAndStructureAreaAreaList().add(toAdd);
                break;
            case 4:
                entry.getAccessAndUseAreaList().add(toAdd);
                break;
            case 5:
                entry.getAlliedMaterialsAreaList().add(toAdd);
                break;
            case 6:
                entry.getNotesAreaList().add(toAdd);
                break;
            case 7:
                entry.getDescriptionControlAreaList().add(toAdd);
                break;
            default:
        }
    }

    public static void addGroupData(IMetadataField instance, List<IValue> groups) {
        if (groups != null) {
            for (IValue groupData : groups) {
                IMetadataGroup eadGroup = instance.createGroup();
                GroupValue gv = (GroupValue) groupData;
                Map<String, List<IValue>> groupMetadata = gv.getSubfields();

                for (IMetadataField sub : eadGroup.getFields()) {
                    List<IValue> values = groupMetadata.get(sub.getName());

                    if (values != null && !values.isEmpty()) {
                        instance.setShowField(true);

                        // split single value into multiple fields
                        for (IValue value : values) {
                            // TODO person,corp
                            ExtendendValue val = (ExtendendValue) value;
                            String stringValue = val.getValue();
                            IFieldValue fv = null;
                            for (IFieldValue v : sub.getValues()) {
                                if (StringUtils.isBlank(v.getValue())) {
                                    fv = v;
                                }
                            }
                            if (fv == null) {
                                fv = new FieldValue(sub);
                                sub.addFieldValue(fv);
                            }
                            fv.setAuthorityType(val.getAuthorityType());
                            fv.setAuthorityValue(val.getAuthorityValue());

                            if ("multiselect".equals(sub.getFieldType()) && StringUtils.isNotBlank(stringValue)) {
                                String[] splittedValues = stringValue.split("; ");
                                for (String s : splittedValues) {
                                    fv.setMultiselectValue(s);
                                }
                            } else {
                                fv.setValue(stringValue);
                            }

                        }
                    } else {
                        IFieldValue fv = new FieldValue(sub);
                        sub.addFieldValue(fv);
                    }
                }
            }
        } else {
            instance.createGroup();
        }
    }
}
