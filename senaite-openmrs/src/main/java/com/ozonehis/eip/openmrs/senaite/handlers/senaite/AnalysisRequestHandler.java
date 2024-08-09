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
import com.ozonehis.eip.openmrs.senaite.model.analysisRequest.AnalysisRequest;
import com.ozonehis.eip.openmrs.senaite.model.analysisRequest.AnalysisRequestResponse;
import com.ozonehis.eip.openmrs.senaite.model.analysisRequest.CancelAnalysisRequestPayload;
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

    public AnalysisRequest sendAnalysisRequest(
            ProducerTemplate producerTemplate, AnalysisRequest analysisRequest, String clientUID)
            throws JsonProcessingException {
        Map<String, Object> headers = new HashMap<>();
        headers.put(Constants.HEADER_CLIENT_UID, clientUID);
        String response = producerTemplate.requestBodyAndHeaders(
                "direct:senaite-create-analysis-request-route", analysisRequest, headers, String.class);
        ObjectMapper objectMapper = new ObjectMapper();
        AnalysisRequestResponse savedAnalysisRequestResponse =
                objectMapper.readValue(response, AnalysisRequestResponse.class);
        return savedAnalysisRequestResponse.analysisRequestResponseToAnalysisRequest(savedAnalysisRequestResponse);
    }

    public AnalysisRequest getAnalysisRequestByClientIDAndClientSampleID(
            ProducerTemplate producerTemplate, String clientID, String clientSampleID) throws JsonProcessingException {
        Map<String, Object> headers = new HashMap<>();
        headers.put(Constants.HEADER_CLIENT_ID, clientID);
        headers.put(Constants.HEADER_CLIENT_SAMPLE_ID, clientSampleID);
        String response = producerTemplate.requestBodyAndHeaders(
                "direct:senaite-get-analysis-request-route", null, headers, String.class);
        ObjectMapper objectMapper = new ObjectMapper();
        AnalysisRequestResponse analysisRequestResponse =
                objectMapper.readValue(response, AnalysisRequestResponse.class);
        return analysisRequestResponse.analysisRequestResponseToAnalysisRequest(analysisRequestResponse);
    }

    public AnalysisRequest getAnalysisRequestByClientSampleID(ProducerTemplate producerTemplate, String clientSampleID)
            throws JsonProcessingException {
        Map<String, Object> headers = new HashMap<>();
        headers.put(Constants.HEADER_CLIENT_SAMPLE_ID, clientSampleID);
        String response = producerTemplate.requestBodyAndHeaders(
                "direct:senaite-get-analysis-request-by-client-sample-id-route", null, headers, String.class);
        ObjectMapper objectMapper = new ObjectMapper();
        AnalysisRequestResponse analysisRequestResponse =
                objectMapper.readValue(response, AnalysisRequestResponse.class);
        return analysisRequestResponse.analysisRequestResponseToAnalysisRequest(analysisRequestResponse);
    }

    public AnalysisRequestResponse getAnalysisRequestResponseByClientIDAndClientSampleID(
            ProducerTemplate producerTemplate, String clientID, String clientSampleID) throws JsonProcessingException {
        Map<String, Object> headers = new HashMap<>();
        headers.put(Constants.HEADER_CLIENT_ID, clientID);
        headers.put(Constants.HEADER_CLIENT_SAMPLE_ID, clientSampleID);
        String response = producerTemplate.requestBodyAndHeaders(
                "direct:senaite-get-analysis-request-route", null, headers, String.class);
        ObjectMapper objectMapper = new ObjectMapper();
        AnalysisRequestResponse analysisRequestResponse =
                objectMapper.readValue(response, AnalysisRequestResponse.class);
        return analysisRequestResponse;
    }

    public AnalysisRequest cancelAnalysisRequest(
            ProducerTemplate producerTemplate,
            CancelAnalysisRequestPayload cancelAnalysisRequestPayload,
            String analysisRequestUID)
            throws JsonProcessingException {
        Map<String, Object> headers = new HashMap<>();
        headers.put(Constants.HEADER_ANALYSIS_REQUEST_UID, analysisRequestUID);
        String response = producerTemplate.requestBodyAndHeaders(
                "direct:senaite-update-analysis-request-route", cancelAnalysisRequestPayload, headers, String.class);
        ObjectMapper objectMapper = new ObjectMapper();
        AnalysisRequestResponse savedAnalysisRequestResponse =
                objectMapper.readValue(response, AnalysisRequestResponse.class);
        return savedAnalysisRequestResponse.analysisRequestResponseToAnalysisRequest(savedAnalysisRequestResponse);
    }

    public boolean doesAnalysisRequestExists(AnalysisRequest analysisRequest) {
        return analysisRequest != null
                && analysisRequest.getContact() != null
                && !analysisRequest.getContact().isEmpty();
    }

    public boolean doesAnalysisRequestResponseExists(AnalysisRequestResponse analysisRequestResponse) {
        return analysisRequestResponse != null
                && analysisRequestResponse.getAnalysisRequestItems() != null
                && !analysisRequestResponse.getAnalysisRequestItems().isEmpty();
    }
}
