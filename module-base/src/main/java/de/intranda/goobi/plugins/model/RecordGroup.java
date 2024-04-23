package de.intranda.goobi.plugins.model;

import org.goobi.interfaces.IEadEntry;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RecordGroup {

    private Integer id;
    private String title;

    private IEadEntry rootEntry;
}
