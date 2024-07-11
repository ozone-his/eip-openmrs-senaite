package com.ozonehis.eip.openmrs.senaite.model.analysisRequestTemplate;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Analyses {
    @JsonProperty("service_uid")
    private String serviceUid;

    @JsonProperty("partition")
    private String partition;
}