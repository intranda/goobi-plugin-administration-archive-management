package de.intranda.goobi.plugins.model;

import java.io.Serializable;

import org.goobi.interfaces.IProcessTemplate;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ProcessTemplate implements IProcessTemplate, Serializable {

    private static final long serialVersionUID = 1326314013524292276L;

    private String templateId;

    private String templateName;

    private String projectId;

}
