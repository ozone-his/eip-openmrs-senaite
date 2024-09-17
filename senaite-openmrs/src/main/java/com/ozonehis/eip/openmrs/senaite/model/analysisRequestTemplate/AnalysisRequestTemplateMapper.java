/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openmrs.senaite.model.analysisRequestTemplate;

import com.ozonehis.eip.openmrs.senaite.model.SenaiteResponseWrapper;
import com.ozonehis.eip.openmrs.senaite.model.analysisRequestTemplate.response.AnalysisRequestTemplateItem;

public class AnalysisRequestTemplateMapper {

    public static AnalysisRequestTemplateDTO map(
            SenaiteResponseWrapper<AnalysisRequestTemplateItem> analysisRequestTemplateResponse) {
        AnalysisRequestTemplateDTO analysisRequestTemplateDTO = new AnalysisRequestTemplateDTO();
        if (analysisRequestTemplateResponse != null
                && analysisRequestTemplateResponse.getItems() != null
                && !analysisRequestTemplateResponse.getItems().isEmpty()) {
            AnalysisRequestTemplateItem analysisRequestTemplateItem =
                    analysisRequestTemplateResponse.getItems().get(0);

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
