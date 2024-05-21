package de.intranda.goobi.plugins.model;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.goobi.interfaces.IFieldValue;
import org.goobi.interfaces.IMetadataField;

import de.intranda.digiverso.normdataimporter.NormDataImporter;
import de.intranda.digiverso.normdataimporter.model.NormData;
import de.sub.goobi.config.ConfigurationHelper;
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

    // e.g. gnd, viaf, geonames
    private String authorityType;

    private String authorityValue;

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
        if (field.getXpath() != null && field.getXpath().contains("unittitle")) {
            field.getEadEntry().setLabel(value);
        }

    }

    /* authority data */

    private String searchValue;
    private String searchOption;
    private List<List<NormData>> dataList;
    private List<NormData> currentData;
    private boolean showNoHits;

    @Override
    public String getGndNumber() {
        return authorityValue;
    }

    @Override
    public void setGndNumber(String arg0) {
        // do nothing
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
        URL url = convertToURLEscapingIllegalCharacters("http://normdata.intranda.com/normdata/gnd/woe/" + val);
        String string = url.toString()
                .replace("Ä", "%C3%84")
                .replace("Ö", "%C3%96")
                .replace("Ü", "%C3%9C")
                .replace("ä", "%C3%A4")
                .replace("ö", "%C3%B6")
                .replace("ü", "%C3%BC")
                .replace("ß", "%C3%9F");
        if (ConfigurationHelper.getInstance().isUseProxy()) {
            setDataList(NormDataImporter.importNormDataList(string, 3, ConfigurationHelper.getInstance().getProxyUrl(),
                    ConfigurationHelper.getInstance().getProxyPort()));
        } else {
            setDataList(NormDataImporter.importNormDataList(string, 3, null, 0));
        }
        setShowNoHits(getDataList() == null || getDataList().isEmpty());
    }

}
