package com.ozonehis.eip.openmrs.senaite.model.analysisRequest;

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
    @JsonProperty("url")
    private String analysesUrl;

    @JsonProperty("uid")
    private String analysesUid;

    @JsonProperty("api_url")
    private String analysesUrlApiUrl;
}
