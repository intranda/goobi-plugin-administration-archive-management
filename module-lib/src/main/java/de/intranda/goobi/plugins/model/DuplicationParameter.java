package de.intranda.goobi.plugins.model;

import java.io.Serializable;

import org.goobi.interfaces.IParameter;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode
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
    private boolean selected;

    public DuplicationParameter(String fieldName) {
        this.fieldName = fieldName;
    }

    public DuplicationParameter(DuplicationParameter other) {
        this.prefix = other.getPrefix();
        this.suffix = other.getSuffix();
        this.fieldName = other.getFieldName();
        this.counter = other.isCounter();
        this.counterFormat = other.getCounterFormat();
        this.counterStartValue = other.getCounterStartValue();
        this.generated = other.isGenerated();
        this.fieldType = other.getFieldType();
        this.selected = other.isSelected();
    }
}
