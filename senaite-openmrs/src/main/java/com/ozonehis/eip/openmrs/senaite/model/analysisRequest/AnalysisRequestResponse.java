/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openmrs.senaite.model.analysisRequest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ozonehis.eip.openmrs.senaite.model.SenaiteResource;
import java.util.ArrayList;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AnalysisRequestResponse implements SenaiteResource {

    @JsonProperty("items")
    private ArrayList<AnalysisRequestItem> analysisRequestItems;

    public AnalysisRequest analysisRequestResponseToAnalysisRequest(AnalysisRequestResponse analysisRequestResponse) {
        AnalysisRequest analysisRequest = new AnalysisRequest();
        if (analysisRequestResponse != null
                && analysisRequestResponse.getAnalysisRequestItems() != null
                && !analysisRequestResponse.getAnalysisRequestItems().isEmpty()) {
            analysisRequest.setContact(
                    analysisRequestResponse.getAnalysisRequestItems().get(0).getContactUid());
            analysisRequest.setSampleType(
                    analysisRequestResponse.getAnalysisRequestItems().get(0).getSampleTypeUid());
            analysisRequest.setDateSampled(
                    analysisRequestResponse.getAnalysisRequestItems().get(0).getDateSampled());
            analysisRequest.setTemplate(
                    analysisRequestResponse.getAnalysisRequestItems().get(0).getTemplateUid());
            if (analysisRequestResponse.getAnalysisRequestItems().get(0).getProfilesUid() != null
                    && analysisRequestResponse.getAnalysisRequestItems().get(0).getProfilesUid().length > 0) {
                analysisRequest.setProfiles(
                        analysisRequestResponse.getAnalysisRequestItems().get(0).getProfilesUid()[0]);
            }
            if (analysisRequestResponse.getAnalysisRequestItems().get(0).getAnalyses() != null
                    && analysisRequestResponse.getAnalysisRequestItems().get(0).getAnalyses().length > 0) {
                String[] uids = new String
                        [analysisRequestResponse
                                .getAnalysisRequestItems()
                                .get(0)
                                .getAnalyses()
                                .length];
                for (int i = 0;
                        i
                                < analysisRequestResponse
                                        .getAnalysisRequestItems()
                                        .get(0)
                                        .getAnalyses()
                                        .length;
                        i++) {
                    uids[i] = analysisRequestResponse
                            .getAnalysisRequestItems()
                            .get(0)
                            .getAnalyses()[i]
                            .getAnalysesUid();
                }
                analysisRequest.setAnalyses(uids);
            }
            analysisRequest.setClientSampleID(
                    analysisRequestResponse.getAnalysisRequestItems().get(0).getClientSampleID());
            analysisRequest.setReviewState(
                    analysisRequestResponse.getAnalysisRequestItems().get(0).getReviewState());
            return analysisRequest;
        }

        return null;
    }
}
