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
import java.util.Collections;
import java.util.List;

public class AnalysesMapper {

    public static AnalysesDTO map(SenaiteResponseWrapper<AnalysesItem> analysesResponse) {
        if (analysesResponse != null && !analysesResponse.getItems().isEmpty()) {
            return toDTO(analysesResponse.getItems().get(0));
        }

        return null;
    }

    public static List<AnalysesDTO> mapAll(SenaiteResponseWrapper<AnalysesItem> analysesResponse) {
        if (analysesResponse == null || analysesResponse.getItems() == null) {
            return Collections.emptyList();
        }
        return analysesResponse.getItems().stream().map(AnalysesMapper::toDTO).toList();
    }

    private static AnalysesDTO toDTO(AnalysesItem analysesItem) {
        AnalysesDTO analysesDTO = new AnalysesDTO();
        analysesDTO.setUid(analysesItem.getUid());
        analysesDTO.setResult(analysesItem.getResult());
        analysesDTO.setResultCaptureDate(analysesItem.getResultCaptureDate());
        analysesDTO.setDescription(analysesItem.getDescription());
        analysesDTO.setRemarks(analysesItem.getRemarks());
        return analysesDTO;
    }
}
