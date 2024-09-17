/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openmrs.senaite.model.analyses;

import com.ozonehis.eip.openmrs.senaite.model.SenaiteResponseWrapper;
import com.ozonehis.eip.openmrs.senaite.model.analyses.response.AnalysesItem;

public class AnalysesMapper {

    public static AnalysesDTO map(SenaiteResponseWrapper<AnalysesItem> analysesResponse) {
        AnalysesDTO analysesDTO = new AnalysesDTO();
        if (analysesResponse != null && !analysesResponse.getItems().isEmpty()) {
            analysesDTO.setResult(analysesResponse.getItems().get(0).getResult());
            analysesDTO.setResultCaptureDate(analysesResponse.getItems().get(0).getResultCaptureDate());
            analysesDTO.setDescription(analysesResponse.getItems().get(0).getDescription());
            return analysesDTO;
        }

        return null;
    }
}
