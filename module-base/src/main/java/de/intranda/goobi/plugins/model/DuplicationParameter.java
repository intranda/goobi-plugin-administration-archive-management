package de.intranda.goobi.plugins.model;

import java.io.Serializable;

import org.goobi.interfaces.IParameter;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DuplicationParameter implements Serializable, IParameter {

    private static final long serialVersionUID = -4579421096881958611L;

    private String prefix = "";
    private String suffix = "";
    private String fieldName;

    private boolean counter;
    private String counterFormat = "%04d";
    private int counterStartValue = 1;

    private boolean generated;

    private String fieldType;

    public DuplicationParameter(String fieldName) {
        this.fieldName = fieldName;
    }

}
