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
import com.ozonehis.eip.openmrs.senaite.model.analysisRequestTemplate.AnalysisRequestTemplate;
import java.util.Map;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.ProducerTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Setter
@Component
public class AnalysisRequestTemplateHandler {

    public AnalysisRequestTemplate sendAnalysisRequestTemplate(
            ProducerTemplate producerTemplate, AnalysisRequestTemplate analysisRequest) throws JsonProcessingException {
        String response = producerTemplate.requestBody(
                "direct:senaite-create-analysis-request-template-route", analysisRequest, String.class);
        log.error("sendAnalysisRequestTemplate response {}", response);
        ObjectMapper objectMapper = new ObjectMapper();
        AnalysisRequestTemplate savedAnalysisRequestTemplate =
                objectMapper.readValue(response, AnalysisRequestTemplate.class);
        log.error("sendAnalysisRequestTemplate {}", response);
        return savedAnalysisRequestTemplate;
    }

    public AnalysisRequestTemplate getAnalysisRequestTemplate(
            ProducerTemplate producerTemplate, Map<String, Object> headers) throws JsonProcessingException {
        String response = producerTemplate.requestBodyAndHeaders(
                "direct:senaite-get-analysis-request-template-route", null, headers, String.class);
        log.error("getAnalysisRequestTemplate response {}", response);
        ObjectMapper objectMapper = new ObjectMapper();
        AnalysisRequestTemplate analysisRequest = objectMapper.readValue(response, AnalysisRequestTemplate.class);
        log.error("getAnalysisRequestTemplate {}", analysisRequest);
        return analysisRequest;
    }
}
