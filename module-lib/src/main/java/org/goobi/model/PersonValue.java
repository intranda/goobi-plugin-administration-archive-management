package org.goobi.model;

import org.goobi.interfaces.IValue;

import lombok.Data;

@Data
public class PersonValue implements IValue {

    private String name;
    private String firstname;
    private String lastname;
    private String authorityType;
    private String authorityValue;

}
