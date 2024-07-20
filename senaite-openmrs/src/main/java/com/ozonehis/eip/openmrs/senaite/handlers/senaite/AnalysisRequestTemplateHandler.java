/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openmrs.senaite.handlers.senaite;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ozonehis.eip.openmrs.senaite.Constants;
import com.ozonehis.eip.openmrs.senaite.model.analysisRequestTemplate.AnalysisRequestTemplate;
import java.util.HashMap;
import java.util.Map;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.ProducerTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Setter
@Component
public class AnalysisRequestTemplateHandler {

    public AnalysisRequestTemplate getAnalysisRequestTemplateByServiceRequestCode(
            ProducerTemplate producerTemplate, String serviceRequestCode) throws JsonProcessingException {
        Map<String, Object> headers = new HashMap<>();
        headers.put(Constants.HEADER_DESCRIPTION, serviceRequestCode);
        String response = producerTemplate.requestBodyAndHeaders(
                "direct:senaite-get-analysis-request-template-route", null, headers, String.class);
        ObjectMapper objectMapper = new ObjectMapper();
        AnalysisRequestTemplate analysisRequest = objectMapper.readValue(response, AnalysisRequestTemplate.class);
        return analysisRequest;
    }

    public boolean doesAnalysisRequestTemplateExists(AnalysisRequestTemplate analysisRequestTemplate) {
        return analysisRequestTemplate != null
                && analysisRequestTemplate.getAnalysisRequestTemplateItems() != null
                && !analysisRequestTemplate.getAnalysisRequestTemplateItems().isEmpty();
    }
}
