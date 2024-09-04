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
import com.ozonehis.eip.openmrs.senaite.model.analysisRequest.AnalysisRequestDAO;
import com.ozonehis.eip.openmrs.senaite.model.analysisRequest.AnalysisRequestMapper;
import com.ozonehis.eip.openmrs.senaite.model.analysisRequest.request.AnalysisRequest;
import com.ozonehis.eip.openmrs.senaite.model.analysisRequest.request.CancelAnalysisRequest;
import com.ozonehis.eip.openmrs.senaite.model.analysisRequest.response.AnalysisRequestResponse;
import java.util.HashMap;
import java.util.Map;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.ProducerTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Setter
@Component
public class AnalysisRequestHandler {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public AnalysisRequestDAO sendAnalysisRequest(
            ProducerTemplate producerTemplate, AnalysisRequest analysisRequest, String clientUID)
            throws JsonProcessingException {
        Map<String, Object> headers = new HashMap<>();
        headers.put(Constants.HEADER_CLIENT_UID, clientUID);
        String response = producerTemplate.requestBodyAndHeaders(
                "direct:senaite-create-analysis-request-route", analysisRequest, headers, String.class);
        AnalysisRequestResponse savedAnalysisRequestResponse =
                objectMapper.readValue(response, AnalysisRequestResponse.class);
        return AnalysisRequestMapper.map(savedAnalysisRequestResponse);
    }

    public AnalysisRequestDAO getAnalysisRequestByClientIDAndClientSampleID(
            ProducerTemplate producerTemplate, String clientID, String clientSampleID) throws JsonProcessingException {
        Map<String, Object> headers = new HashMap<>();
        headers.put(Constants.HEADER_CLIENT_ID, clientID);
        headers.put(Constants.HEADER_CLIENT_SAMPLE_ID, clientSampleID);
        String response = producerTemplate.requestBodyAndHeaders(
                "direct:senaite-get-analysis-request-route", null, headers, String.class);
        AnalysisRequestResponse analysisRequestResponse =
                objectMapper.readValue(response, AnalysisRequestResponse.class);
        return AnalysisRequestMapper.map(analysisRequestResponse);
    }

    public AnalysisRequestDAO getAnalysisRequestByClientSampleID(
            ProducerTemplate producerTemplate, String clientSampleID) throws JsonProcessingException {
        Map<String, Object> headers = new HashMap<>();
        headers.put(Constants.HEADER_CLIENT_SAMPLE_ID, clientSampleID);
        String response = producerTemplate.requestBodyAndHeaders(
                "direct:senaite-get-analysis-request-by-client-sample-id-route", null, headers, String.class);
        AnalysisRequestResponse analysisRequestResponse =
                objectMapper.readValue(response, AnalysisRequestResponse.class);
        return AnalysisRequestMapper.map(analysisRequestResponse);
    }

    public AnalysisRequestDAO cancelAnalysisRequest(
            ProducerTemplate producerTemplate, CancelAnalysisRequest cancelAnalysisRequest, String analysisRequestUID)
            throws JsonProcessingException {
        Map<String, Object> headers = new HashMap<>();
        headers.put(Constants.HEADER_ANALYSIS_REQUEST_UID, analysisRequestUID);
        String response = producerTemplate.requestBodyAndHeaders(
                "direct:senaite-update-analysis-request-route", cancelAnalysisRequest, headers, String.class);
        AnalysisRequestResponse savedAnalysisRequestResponse =
                objectMapper.readValue(response, AnalysisRequestResponse.class);
        return AnalysisRequestMapper.map(savedAnalysisRequestResponse);
    }

    public boolean doesAnalysisRequestExists(AnalysisRequestDAO analysisRequestDAO) {
        return analysisRequestDAO != null
                && analysisRequestDAO.getContact() != null
                && !analysisRequestDAO.getContact().isEmpty();
    }
}
