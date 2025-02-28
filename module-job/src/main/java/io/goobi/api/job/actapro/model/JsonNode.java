package io.goobi.api.job.actapro.model;

import lombok.Getter;
import lombok.Setter;

/**
 * Document content
 **/
@Getter
@Setter
public class JsonNode {

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class JsonNode {\n");

        sb.append("}");
        return sb.toString();
    }

}
