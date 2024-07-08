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
public class AnalysisProfile {
    @JsonProperty("url")
    private String url;

    @JsonProperty("uid")
    private String uid;

    @JsonProperty("api_url")
    private String apiUrl;
}
