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

    public static AnalysisRequestTemplateDAO map(AnalysisRequestTemplateResponse analysisRequestTemplateResponse) {
        AnalysisRequestTemplateDAO analysisRequestTemplateDAO = new AnalysisRequestTemplateDAO();
        if (analysisRequestTemplateResponse != null
                && analysisRequestTemplateResponse.getAnalysisRequestTemplateItems() != null
                && !analysisRequestTemplateResponse
                        .getAnalysisRequestTemplateItems()
                        .isEmpty()) {
            AnalysisRequestTemplateItem analysisRequestTemplateItem = analysisRequestTemplateResponse
                    .getAnalysisRequestTemplateItems()
                    .get(0);

            analysisRequestTemplateDAO.setUid(analysisRequestTemplateItem.getUid());
            analysisRequestTemplateDAO.setPath(analysisRequestTemplateItem.getPath());
            analysisRequestTemplateDAO.setAnalysisProfile(analysisRequestTemplateItem.getAnalysisProfile());
            analysisRequestTemplateDAO.setAnalyses(analysisRequestTemplateItem.getAnalyses());
            analysisRequestTemplateDAO.setSampleType(analysisRequestTemplateItem.getSampleType());

            return analysisRequestTemplateDAO;
        }

        return null;
    }
}
