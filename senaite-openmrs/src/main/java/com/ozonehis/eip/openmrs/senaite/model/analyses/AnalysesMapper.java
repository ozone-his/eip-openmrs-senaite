/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openmrs.senaite.model.analyses;

import com.ozonehis.eip.openmrs.senaite.model.analyses.response.AnalysesResponse;

public class AnalysesMapper {

    public static AnalysesDAO map(AnalysesResponse analysesResponse) {
        AnalysesDAO analysesDAO = new AnalysesDAO();
        if (analysesResponse != null && !analysesResponse.getAnalysesItems().isEmpty()) {
            analysesDAO.setResult(analysesResponse.getAnalysesItems().get(0).getResult());
            analysesDAO.setResultCaptureDate(
                    analysesResponse.getAnalysesItems().get(0).getResultCaptureDate());
            analysesDAO.setDescription(
                    analysesResponse.getAnalysesItems().get(0).getDescription());
            return analysesDAO;
        }

        return null;
    }
}
