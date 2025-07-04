package org.goobi.model;

import org.goobi.interfaces.IValue;

import lombok.Data;

@Data
public class CorporateValue implements IValue {

    private String name;
    private String value;
    private String subvalue;
    private String authorityType;
    private String authorityValue;

}
