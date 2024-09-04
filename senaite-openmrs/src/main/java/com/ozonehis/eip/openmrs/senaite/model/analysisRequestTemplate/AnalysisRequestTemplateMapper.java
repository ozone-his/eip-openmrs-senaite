/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openmrs.senaite.model.analysisRequestTemplate;

import com.ozonehis.eip.openmrs.senaite.model.analysisRequestTemplate.response.AnalysisRequestTemplateItem;
import com.ozonehis.eip.openmrs.senaite.model.analysisRequestTemplate.response.AnalysisRequestTemplateResponse;

public class AnalysisRequestTemplateMapper {

    public static AnalysisRequestTemplateDTO map(AnalysisRequestTemplateResponse analysisRequestTemplateResponse) {
        AnalysisRequestTemplateDTO analysisRequestTemplateDTO = new AnalysisRequestTemplateDTO();
        if (analysisRequestTemplateResponse != null
                && analysisRequestTemplateResponse.getAnalysisRequestTemplateItems() != null
                && !analysisRequestTemplateResponse
                        .getAnalysisRequestTemplateItems()
                        .isEmpty()) {
            AnalysisRequestTemplateItem analysisRequestTemplateItem = analysisRequestTemplateResponse
                    .getAnalysisRequestTemplateItems()
                    .get(0);

            analysisRequestTemplateDTO.setUid(analysisRequestTemplateItem.getUid());
            analysisRequestTemplateDTO.setPath(analysisRequestTemplateItem.getPath());
            analysisRequestTemplateDTO.setAnalysisProfile(analysisRequestTemplateItem.getAnalysisProfile());
            analysisRequestTemplateDTO.setAnalyses(analysisRequestTemplateItem.getAnalyses());
            analysisRequestTemplateDTO.setSampleType(analysisRequestTemplateItem.getSampleType());

            return analysisRequestTemplateDTO;
        }

        return null;
    }
}
