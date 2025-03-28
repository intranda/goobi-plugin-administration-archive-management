package org.goobi.validation;

import org.apache.commons.lang.StringUtils;

import de.sub.goobi.helper.Helper;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.validator.FacesValidator;
import jakarta.faces.validator.Validator;
import jakarta.faces.validator.ValidatorException;

@FacesValidator(value = "numberOfNodesValidator")

public class NumberOfNodesValidator implements Validator<String> {

    @Override
    public void validate(FacesContext context, UIComponent component, String data) throws ValidatorException {

        FacesMessage message = null;
        boolean valid = true;

        if (StringUtils.isBlank(data)) {
            valid = false;
            message = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Missing data",
                    Helper.getTranslation("plugin_administration_archive_requiredField"));
        }
        char[] numbers = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' };

        if (!StringUtils.containsOnly(data, numbers)) {
            valid = false;
            message =
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Missing data", Helper.getTranslation("plugin_administration_archive_onlyNumbers"));
        } else {
            try {
                int number = Integer.parseInt(data);
                if (number == 0) {
                    valid = false;
                    message = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Missing data",
                            Helper.getTranslation("plugin_administration_archive_zeroIsNotAllowed"));
                }
            } catch (Exception e) {
                valid = false;
                message = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Missing data",
                        Helper.getTranslation("plugin_administration_archive_onlyNumbers"));
            }
        }

        if (!valid) {
            throw new ValidatorException(message);
        }

    }

}
