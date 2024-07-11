package com.ozonehis.eip.openmrs.senaite.model.analyses;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AnalysesResponse {

    @JsonProperty("items")
    private ArrayList<AnalysesItem> analysesItems;

    public Analyses analysesResponseToAnalyses(AnalysesResponse analysesResponse) {
        Analyses analyses = new Analyses();
        if (analysesResponse != null && !analysesResponse.getAnalysesItems().isEmpty()) {
            analyses.setResult(analysesResponse.getAnalysesItems().get(0).getResult());
            analyses.setResultCaptureDate(
                    analysesResponse.getAnalysesItems().get(0).getResultCaptureDate());
            analyses.setDescription(analysesResponse.getAnalysesItems().get(0).getDescription());
            return analyses;
        }

        return null;
    }
}
