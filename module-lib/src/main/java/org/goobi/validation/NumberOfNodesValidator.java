package org.goobi.validation;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.validator.FacesValidator;
import javax.faces.validator.Validator;
import javax.faces.validator.ValidatorException;

import org.apache.commons.lang.StringUtils;

import de.sub.goobi.helper.Helper;

@FacesValidator(value = "numberOfNodesValidator")

public class NumberOfNodesValidator implements Validator {

    @Override
    public void validate(FacesContext context, UIComponent component, Object value) throws ValidatorException {
        String data = String.valueOf(value);

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
