/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
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

    public AnalysesDetails analysesResponseToAnalyses(AnalysesResponse analysesResponse) {
        AnalysesDetails analysesDetails = new AnalysesDetails();
        if (analysesResponse != null && !analysesResponse.getAnalysesItems().isEmpty()) {
            analysesDetails.setResult(analysesResponse.getAnalysesItems().get(0).getResult());
            analysesDetails.setResultCaptureDate(
                    analysesResponse.getAnalysesItems().get(0).getResultCaptureDate());
            analysesDetails.setDescription(
                    analysesResponse.getAnalysesItems().get(0).getDescription());
            return analysesDetails;
        }

        return null;
    }
}
