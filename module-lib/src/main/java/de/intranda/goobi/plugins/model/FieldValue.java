package de.intranda.goobi.plugins.model;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.geonames.Style;
import org.geonames.Toponym;
import org.geonames.ToponymSearchCriteria;
import org.geonames.ToponymSearchResult;
import org.geonames.WebService;
import org.goobi.interfaces.IFieldValue;
import org.goobi.interfaces.IMetadataField;

import de.intranda.digiverso.normdataimporter.NormDataImporter;
import de.intranda.digiverso.normdataimporter.model.MarcRecord;
import de.intranda.digiverso.normdataimporter.model.NormData;
import de.intranda.digiverso.normdataimporter.model.TagDescription;
import de.sub.goobi.config.ConfigurationHelper;
import de.sub.goobi.helper.Helper;
import de.sub.goobi.metadaten.search.ViafSearch;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@ToString
@EqualsAndHashCode(exclude = { "field", "viafSearch" })
public class FieldValue implements IFieldValue {

    private String value;
    /** contains the list of selected values in multiselect */
    private List<String> multiselectSelectedValues = new ArrayList<>();
    @ToString.Exclude
    private IMetadataField field;

    // e.g. gnd, viaf, geonames
    private String authorityType;
    private String authorityValue;

    // gnd search fields
    private String searchValue;
    private String searchOption;
    @ToString.Exclude
    private List<List<NormData>> dataList;
    @ToString.Exclude
    private List<NormData> currentData;
    private boolean showNoHits;

    // can be metadata, corporate or person
    private String fieldType = "metadata";
    // fields for person
    private String firstname;
    private String lastname;
    // fields for corporations
    private String mainName;
    private String subName;
    private String partName;

    // geonames search fields
    @ToString.Exclude
    private Toponym currentToponym;
    @ToString.Exclude
    private List<Toponym> resultList;
    private int totalResults;

    // viaf search fields
    @ToString.Exclude
    private ViafSearch viafSearch = new ViafSearch();

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
        if (field.getXpath() != null && field.getXpath().contains("unittitle") && field.getEadEntry() != null) {
            field.getEadEntry().setLabel(value);
        }
    }

    /* authority data */

    @Override
    public String getGndNumber() {
        return authorityValue;
    }

    @Override
    public void setGndNumber(String number) {
        authorityValue = number;
    }

    @Override
    public void importGndData() {
        for (NormData normdata : currentData) {
            if ("NORM_IDENTIFIER".equals(normdata.getKey())) {
                String gndNumber = normdata.getValues().get(0).getText();
                authorityValue = gndNumber;
                authorityType = "gnd";
            } else if ("NORM_NAME".equals(normdata.getKey())) {
                value = normdata.getValues().get(0).getText();
            }
        }
    }

    @Override
    public void searchGnd() {
        String val = "";
        if (StringUtils.isBlank(getSearchOption()) && StringUtils.isBlank(getSearchValue())) {
            setShowNoHits(true);
            return;
        }
        if (StringUtils.isBlank(getSearchOption())) {
            val = "dnb.nid=" + searchValue;
        } else {
            val = searchValue + " and BBG=" + searchOption;
        }

        if (ConfigurationHelper.getInstance().isUseProxy()) {
            setDataList(NormDataImporter.getGndRecords("http://services.dnb.de/sru/authorities", val, ConfigurationHelper.getInstance().getProxyUrl(),
                    ConfigurationHelper.getInstance().getProxyPort()));
        } else {
            setDataList(NormDataImporter.getGndRecords("http://services.dnb.de/sru/authorities", val, null,
                    null));
        }
        setShowNoHits(getDataList() == null || getDataList().isEmpty());
    }

    @Override
    public String getGeonamesNumber() {
        return getGndNumber();
    }

    @Override
    public void setGeonamesNumber(String number) {
        setGndNumber(number);
    }

    @Override
    public void importGeonamesData() {
        value = currentToponym.getName();
        authorityValue = "" + currentToponym.getGeoNameId();
        authorityType = "geonames";
        currentToponym = null;
        resultList = null;
        totalResults = 0;
    }

    @Override
    public void searchGeonames() {
        String credentials = ConfigurationHelper.getInstance().getGeonamesCredentials();
        if (credentials != null) {
            WebService.setUserName(credentials);
            ToponymSearchCriteria searchCriteria = new ToponymSearchCriteria();
            searchCriteria.setNameEquals(searchValue);
            searchCriteria.setStyle(Style.FULL);
            try {
                ToponymSearchResult searchResult = WebService.search(searchCriteria);
                resultList = searchResult.getToponyms();
                totalResults = searchResult.getTotalResultsCount();
            } catch (Exception e) {
                // ignore errors
            }
            setShowNoHits(getResultList() == null || getResultList().isEmpty());
        } else {
            Helper.setFehlerMeldung("geonamesList", "Missing data", "mets_geoname_account_inactive");
        }
    }

    @Override
    public String getViafNumber() {
        return getGndNumber();
    }

    @Override
    public void setViafNumber(String number) {
        setGndNumber(number);
    }

    @Override
    public void importViafData() {
        if (viafSearch.getCurrentDatabase() != null) {
            MarcRecord recordToImport = NormDataImporter.getSingleMarcRecord(viafSearch.getCurrentDatabase().getMarcRecordUrl());
            // TODO: person corp
            List<String> names = new ArrayList<>();
            for (TagDescription tag : viafSearch.getMainTagList()) {
                if (tag.getSubfieldCode() == null) {
                    String val = recordToImport.getControlfieldValue(tag.getDatafieldTag());
                    if (StringUtils.isNotBlank(val)) {
                        names.add(val);
                    }
                } else {
                    List<String> list = recordToImport.getFieldValues(tag.getDatafieldTag(), tag.getInd1(), tag.getInd2(), tag.getSubfieldCode());
                    if (list != null) {
                        names.addAll(list);
                    }
                }
            }
            if (!names.isEmpty()) {
                authorityType = "viaf";
                authorityValue = viafSearch.getCurrentDatabase().getMarcRecordUrl();
                value = names.get(0);
            }
        }
    }

    @Override
    public void searchViaf() {
        viafSearch.setSource(field.getViafSearchFields());
        viafSearch.setField(field.getViafDisplayFields());
        viafSearch.performSearchRequest();
    }

    public boolean isBooleanValue() {
        return StringUtils.isNotBlank(value) && "true".equals(value);
    }

    public void setBooleanValue(boolean val) {
        if (val) {
            value = "true";
        } else {
            value = "false";
        }
    }
}
