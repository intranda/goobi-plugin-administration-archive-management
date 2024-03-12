package de.intranda.goobi.plugins.model;

import java.util.ArrayList;
import java.util.List;

import org.goobi.interfaces.IConfiguration;
import org.goobi.interfaces.IEadEntry;
import org.goobi.interfaces.IMetadataField;
import org.goobi.interfaces.IParameter;

import lombok.Getter;

public class DuplicationConfiguration implements IConfiguration {

    @Getter
    private List<IParameter> identityStatementArea = new ArrayList<>();

    @Getter
    private List<IParameter> contextArea = new ArrayList<>();

    @Getter
    private List<IParameter> contentArea = new ArrayList<>();

    @Getter
    private List<IParameter> accessArea = new ArrayList<>();

    @Getter
    private List<IParameter> alliedMaterialsArea = new ArrayList<>();

    @Getter
    private List<IParameter> notesArea = new ArrayList<>();
    @Getter
    private List<IParameter> descriptionArea = new ArrayList<>();

    public DuplicationConfiguration(IEadEntry entry) {

        for (IMetadataField field : entry.getIdentityStatementAreaList()) {
            if (field.isSearchable()) {
                IParameter dp = new DuplicationParameter(field.getName());
                identityStatementArea.add(dp);
            }
        }
        for (IMetadataField field : entry.getContextAreaList()) {
            if (field.isSearchable()) {
                DuplicationParameter dp = new DuplicationParameter(field.getName());
                contextArea.add(dp);
            }
        }
        for (IMetadataField field : entry.getContentAndStructureAreaAreaList()) {
            if (field.isSearchable()) {
                DuplicationParameter dp = new DuplicationParameter(field.getName());
                contentArea.add(dp);
            }
        }
        for (IMetadataField field : entry.getAccessAndUseAreaList()) {
            if (field.isSearchable()) {
                DuplicationParameter dp = new DuplicationParameter(field.getName());
                accessArea.add(dp);
            }
        }
        for (IMetadataField field : entry.getAlliedMaterialsAreaList()) {
            if (field.isSearchable()) {
                DuplicationParameter dp = new DuplicationParameter(field.getName());
                alliedMaterialsArea.add(dp);
            }
        }
        for (IMetadataField field : entry.getNotesAreaList()) {
            if (field.isSearchable()) {
                DuplicationParameter dp = new DuplicationParameter(field.getName());
                notesArea.add(dp);
            }
        }
        for (IMetadataField field : entry.getDescriptionControlAreaList()) {
            if (field.isSearchable()) {
                DuplicationParameter dp = new DuplicationParameter(field.getName());
                descriptionArea.add(dp);
            }
        }
    }
}
