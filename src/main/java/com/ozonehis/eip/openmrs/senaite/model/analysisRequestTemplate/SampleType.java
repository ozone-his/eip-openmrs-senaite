package com.ozonehis.eip.openmrs.senaite.model.analysisRequestTemplate;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

//             "SampleType": {
//                "url": "http://localhost:8081/bika_setup/bika_sampletypes/sampletype-1",
//                "uid": "0c5a92ea879a4dafa13f60762d93e52e",
//                "api_url":
// "http://localhost:8081/senaite/@@API/senaite/v1/sampletype/0c5a92ea879a4dafa13f60762d93e52e"
//            },
@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SampleType {
    @JsonProperty("url")
    private String url;

    @JsonProperty("uid")
    private String uid;

    @JsonProperty("api_url")
    private String apiUrl;
}
