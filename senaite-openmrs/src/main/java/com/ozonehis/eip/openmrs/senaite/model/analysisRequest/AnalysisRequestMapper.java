/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openmrs.senaite.model.analysisRequest;

import com.ozonehis.eip.openmrs.senaite.model.SenaiteResponseWrapper;
import com.ozonehis.eip.openmrs.senaite.model.analysisRequest.response.Analyses;
import com.ozonehis.eip.openmrs.senaite.model.analysisRequest.response.AnalysisRequestItem;
import java.util.Arrays;

public class AnalysisRequestMapper {

    public static AnalysisRequestDTO map(SenaiteResponseWrapper<AnalysisRequestItem> analysisRequestResponse) {
        AnalysisRequestDTO analysisRequestDTO = new AnalysisRequestDTO();
        if (analysisRequestResponse != null
                && analysisRequestResponse.getItems() != null
                && !analysisRequestResponse.getItems().isEmpty()) {
            AnalysisRequestItem analysisRequestItem =
                    analysisRequestResponse.getItems().get(0);

            analysisRequestDTO.setContact(analysisRequestItem.getContactUid());
            analysisRequestDTO.setSampleType(analysisRequestItem.getSampleTypeUid());
            analysisRequestDTO.setDateSampled(analysisRequestItem.getDateSampled());
            analysisRequestDTO.setTemplate(analysisRequestItem.getTemplateUid());
            analysisRequestDTO.setClient(analysisRequestItem.getClientUID());
            analysisRequestDTO.setUid(analysisRequestItem.getUid());
            if (analysisRequestItem.getProfilesUid() != null && analysisRequestItem.getProfilesUid().length > 0) {
                analysisRequestDTO.setProfiles(analysisRequestItem.getProfilesUid()[0]);
            }
            if (analysisRequestItem.getAnalyses() != null && analysisRequestItem.getAnalyses().length > 0) {
                String[] uids = Arrays.stream(analysisRequestItem.getAnalyses())
                        .map(Analyses::getAnalysesUid)
                        .toArray(String[]::new);

                analysisRequestDTO.setAnalysesUids(uids);
            }
            analysisRequestDTO.setAnalyses(analysisRequestItem.getAnalyses());
            analysisRequestDTO.setClientSampleID(analysisRequestItem.getClientSampleID());
            analysisRequestDTO.setReviewState(analysisRequestItem.getReviewState());
            analysisRequestDTO.setDatePublished(analysisRequestItem.getDatePublished());
            return analysisRequestDTO;
        }

        return null;
    }
}
