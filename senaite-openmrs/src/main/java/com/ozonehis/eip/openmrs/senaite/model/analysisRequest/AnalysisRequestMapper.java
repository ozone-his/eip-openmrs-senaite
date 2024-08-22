/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openmrs.senaite.model.analysisRequest;

import com.ozonehis.eip.openmrs.senaite.model.analysisRequest.response.Analyses;
import com.ozonehis.eip.openmrs.senaite.model.analysisRequest.response.AnalysisRequestItem;
import com.ozonehis.eip.openmrs.senaite.model.analysisRequest.response.AnalysisRequestResponse;
import java.util.Arrays;

public class AnalysisRequestMapper {

    public static AnalysisRequestDAO map(AnalysisRequestResponse analysisRequestResponse) {
        AnalysisRequestDAO analysisRequestDAO = new AnalysisRequestDAO();
        if (analysisRequestResponse != null
                && analysisRequestResponse.getAnalysisRequestItems() != null
                && !analysisRequestResponse.getAnalysisRequestItems().isEmpty()) {
            AnalysisRequestItem analysisRequestItem =
                    analysisRequestResponse.getAnalysisRequestItems().get(0);

            analysisRequestDAO.setContact(analysisRequestItem.getContactUid());
            analysisRequestDAO.setSampleType(analysisRequestItem.getSampleTypeUid());
            analysisRequestDAO.setDateSampled(analysisRequestItem.getDateSampled());
            analysisRequestDAO.setTemplate(analysisRequestItem.getTemplateUid());
            analysisRequestDAO.setClient(analysisRequestItem.getClientUID());
            analysisRequestDAO.setUid(analysisRequestItem.getUid());
            if (analysisRequestItem.getProfilesUid() != null && analysisRequestItem.getProfilesUid().length > 0) {
                analysisRequestDAO.setProfiles(analysisRequestItem.getProfilesUid()[0]);
            }
            if (analysisRequestItem.getAnalyses() != null && analysisRequestItem.getAnalyses().length > 0) {
                String[] uids = Arrays.stream(analysisRequestItem.getAnalyses())
                        .map(Analyses::getAnalysesUid)
                        .toArray(String[]::new);

                analysisRequestDAO.setAnalysesUids(uids);
            }
            analysisRequestDAO.setAnalyses(analysisRequestItem.getAnalyses());
            analysisRequestDAO.setClientSampleID(analysisRequestItem.getClientSampleID());
            analysisRequestDAO.setReviewState(analysisRequestItem.getReviewState());
            return analysisRequestDAO;
        }

        return null;
    }
}
