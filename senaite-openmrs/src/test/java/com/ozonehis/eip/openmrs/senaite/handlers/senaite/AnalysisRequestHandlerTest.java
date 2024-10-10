/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openmrs.senaite.handlers.senaite;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ozonehis.eip.openmrs.senaite.Constants;
import com.ozonehis.eip.openmrs.senaite.model.SenaiteResponseWrapper;
import com.ozonehis.eip.openmrs.senaite.model.analysisRequest.AnalysisRequestDTO;
import com.ozonehis.eip.openmrs.senaite.model.analysisRequest.AnalysisRequestMapper;
import com.ozonehis.eip.openmrs.senaite.model.analysisRequest.request.AnalysisRequest;
import com.ozonehis.eip.openmrs.senaite.model.analysisRequest.request.CancelAnalysisRequest;
import com.ozonehis.eip.openmrs.senaite.model.analysisRequest.response.Analyses;
import com.ozonehis.eip.openmrs.senaite.model.analysisRequest.response.AnalysisRequestItem;
import java.util.HashMap;
import java.util.Map;
import org.apache.camel.ProducerTemplate;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

class AnalysisRequestHandlerTest {

    private static final String CLIENT_ID = "7f4aebaf3d4a4a2f8e8ebb5881d4ce73";

    private static final String CONTACT_ID = "1baac45668fc49cbbd5c4fd35d804b72";

    private static final String SAMPLE_TYPE = "db97756c89f143408a18e0d152d0d337";

    private static final String TEMPLATE_ID = "7a09878065314fe29f8fc994fd2c8447";

    private static final String ANALYSES_UID_1 = "06d1e902317047d1b90b27df5122ab78";

    private static final String DATE_SAMPLED = "2024-10-07T08:20:31+00:00";

    private static final String CLIENT_SAMPLE_ID = "bbaea498-e046-43c6-bf9c-dbbc7d39f38c";

    private static final String UID = "99afa09663054c18b1ae7a1cd2cd7693";

    @Mock
    private ProducerTemplate producerTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private AnalysisRequestHandler analysisRequestHandler;

    private static AutoCloseable mocksCloser;

    @BeforeEach
    void setup() {
        mocksCloser = openMocks(this);
    }

    @AfterAll
    static void close() throws Exception {
        mocksCloser.close();
    }

    @Test
    void shouldSaveAndReturnAnalysisRequest() throws JsonProcessingException {
        // Setup
        String responseBody = new Utils().readJSON("senaite/response/create-analysis-request.json");
        Map<String, Object> headers = new HashMap<>();
        headers.put(Constants.HEADER_CLIENT_UID, CLIENT_ID);

        AnalysisRequest analysisRequest = new AnalysisRequest();
        analysisRequest.setContact(CONTACT_ID);
        analysisRequest.setSampleType(SAMPLE_TYPE);
        analysisRequest.setDateSampled(DATE_SAMPLED);
        analysisRequest.setTemplate(TEMPLATE_ID);
        analysisRequest.setAnalyses(new String[] {ANALYSES_UID_1});
        analysisRequest.setClientSampleID(CLIENT_SAMPLE_ID);
        analysisRequest.setReviewState("sample_due");

        // Mock
        when(producerTemplate.requestBodyAndHeaders(
                        eq("direct:senaite-create-analysis-request-route"),
                        eq(analysisRequest),
                        eq(headers),
                        eq(String.class)))
                .thenReturn(responseBody);

        TypeReference<SenaiteResponseWrapper<AnalysisRequestItem>> typeReference = new TypeReference<>() {};
        SenaiteResponseWrapper<AnalysisRequestItem> responseWrapper =
                objectMapper.readValue(responseBody, typeReference);

        AnalysisRequestDTO analysisRequestDTO = new AnalysisRequestDTO();
        analysisRequestDTO.setContact(CONTACT_ID);
        analysisRequestDTO.setSampleType(SAMPLE_TYPE);
        analysisRequestDTO.setDateSampled(DATE_SAMPLED);
        analysisRequestDTO.setTemplate(TEMPLATE_ID);
        analysisRequestDTO.setProfiles(null);
        analysisRequestDTO.setAnalysesUids(new String[] {ANALYSES_UID_1});
        analysisRequestDTO.setClientSampleID(CLIENT_SAMPLE_ID);
        analysisRequestDTO.setAnalyses(null);
        analysisRequestDTO.setAnalyses(new Analyses[] {
            new Analyses(
                    "http://senaite:8080/senaite/clients/client-1/URI-0001/LAB-030",
                    ANALYSES_UID_1,
                    "http://senaite:8080/senaite/@@API/senaite/v1/analysis/" + ANALYSES_UID_1)
        });
        analysisRequestDTO.setUid(UID);

        when(AnalysisRequestMapper.map(responseWrapper)).thenReturn(analysisRequestDTO);

        // Act
        AnalysisRequestDTO result =
                analysisRequestHandler.sendAnalysisRequest(producerTemplate, analysisRequest, CLIENT_ID);

        // Verify
        assertEquals(analysisRequestDTO, result);
    }

    @Test
    void shouldReturnAnalysisRequestGivenClientIDAndClientSampleID() throws JsonProcessingException {
        // Setup
        String responseBody = new Utils().readJSON("senaite/response/get-analysis-request.json");
        Map<String, Object> headers = new HashMap<>();
        headers.put(Constants.HEADER_CLIENT_ID, CLIENT_ID);
        headers.put(Constants.HEADER_CLIENT_SAMPLE_ID, CLIENT_SAMPLE_ID);

        // Mock
        when(producerTemplate.requestBodyAndHeaders(
                        eq("direct:senaite-get-analysis-request-route"), isNull(), eq(headers), eq(String.class)))
                .thenReturn(responseBody);

        TypeReference<SenaiteResponseWrapper<AnalysisRequestItem>> typeReference = new TypeReference<>() {};
        SenaiteResponseWrapper<AnalysisRequestItem> responseWrapper =
                objectMapper.readValue(responseBody, typeReference);

        AnalysisRequestDTO analysisRequestDTO = new AnalysisRequestDTO();
        analysisRequestDTO.setContact(CONTACT_ID);
        analysisRequestDTO.setSampleType(SAMPLE_TYPE);
        analysisRequestDTO.setDateSampled(DATE_SAMPLED);
        analysisRequestDTO.setTemplate(TEMPLATE_ID);
        analysisRequestDTO.setProfiles(null);
        analysisRequestDTO.setAnalysesUids(new String[] {ANALYSES_UID_1});
        analysisRequestDTO.setClientSampleID(CLIENT_SAMPLE_ID);
        analysisRequestDTO.setAnalyses(null);
        analysisRequestDTO.setAnalyses(new Analyses[] {
            new Analyses(
                    "http://senaite:8080/senaite/clients/client-1/URI-0001/LAB-030",
                    ANALYSES_UID_1,
                    "http://senaite:8080/senaite/@@API/senaite/v1/analysis/" + ANALYSES_UID_1)
        });
        analysisRequestDTO.setUid(UID);
        analysisRequestDTO.setClient(CLIENT_ID);
        analysisRequestDTO.setReviewState("sample_due");

        when(AnalysisRequestMapper.map(responseWrapper)).thenReturn(analysisRequestDTO);

        // Act
        AnalysisRequestDTO result = analysisRequestHandler.getAnalysisRequestByClientIDAndClientSampleID(
                producerTemplate, CLIENT_ID, CLIENT_SAMPLE_ID);

        // Verify
        assertEquals(analysisRequestDTO, result);
    }

    @Test
    void shouldReturnAnalysisRequestGivenClientSampleID() throws JsonProcessingException {
        // Setup
        String responseBody = new Utils().readJSON("senaite/response/get-analysis-request.json");
        Map<String, Object> headers = new HashMap<>();
        headers.put(Constants.HEADER_CLIENT_SAMPLE_ID, CLIENT_SAMPLE_ID);

        // Mock
        when(producerTemplate.requestBodyAndHeaders(
                        eq("direct:senaite-get-analysis-request-by-client-sample-id-route"),
                        isNull(),
                        eq(headers),
                        eq(String.class)))
                .thenReturn(responseBody);

        TypeReference<SenaiteResponseWrapper<AnalysisRequestItem>> typeReference = new TypeReference<>() {};
        SenaiteResponseWrapper<AnalysisRequestItem> responseWrapper =
                objectMapper.readValue(responseBody, typeReference);

        AnalysisRequestDTO analysisRequestDTO = new AnalysisRequestDTO();
        analysisRequestDTO.setContact(CONTACT_ID);
        analysisRequestDTO.setSampleType(SAMPLE_TYPE);
        analysisRequestDTO.setDateSampled(DATE_SAMPLED);
        analysisRequestDTO.setTemplate(TEMPLATE_ID);
        analysisRequestDTO.setProfiles(null);
        analysisRequestDTO.setAnalysesUids(new String[] {ANALYSES_UID_1});
        analysisRequestDTO.setClientSampleID(CLIENT_SAMPLE_ID);
        analysisRequestDTO.setAnalyses(null);
        analysisRequestDTO.setAnalyses(new Analyses[] {
            new Analyses(
                    "http://senaite:8080/senaite/clients/client-1/URI-0001/LAB-030",
                    ANALYSES_UID_1,
                    "http://senaite:8080/senaite/@@API/senaite/v1/analysis/" + ANALYSES_UID_1)
        });
        analysisRequestDTO.setUid(UID);
        analysisRequestDTO.setClient(CLIENT_ID);
        analysisRequestDTO.setReviewState("sample_due");

        when(AnalysisRequestMapper.map(responseWrapper)).thenReturn(analysisRequestDTO);

        // Act
        AnalysisRequestDTO result =
                analysisRequestHandler.getAnalysisRequestByClientSampleID(producerTemplate, CLIENT_SAMPLE_ID);

        // Verify
        assertEquals(analysisRequestDTO, result);
    }

    @Test
    void shouldCancelAndReturnAnalysisRequest() throws JsonProcessingException {
        // Setup
        String responseBody = new Utils().readJSON("senaite/response/cancel-analysis-request.json");
        Map<String, Object> headers = new HashMap<>();
        headers.put(Constants.HEADER_ANALYSIS_REQUEST_UID, "8182d02098ce487086ea207c8f9df5b0");

        CancelAnalysisRequest cancelAnalysisRequest = new CancelAnalysisRequest();
        cancelAnalysisRequest.setClient("639727d93df24cb0a429484da4fbc71a");
        cancelAnalysisRequest.setTransition("cancel");

        // Mock
        when(producerTemplate.requestBodyAndHeaders(
                        eq("direct:senaite-update-analysis-request-route"),
                        eq(cancelAnalysisRequest),
                        eq(headers),
                        eq(String.class)))
                .thenReturn(responseBody);

        TypeReference<SenaiteResponseWrapper<AnalysisRequestItem>> typeReference = new TypeReference<>() {};
        SenaiteResponseWrapper<AnalysisRequestItem> responseWrapper =
                objectMapper.readValue(responseBody, typeReference);

        AnalysisRequestDTO analysisRequestDTO = new AnalysisRequestDTO();
        analysisRequestDTO.setContact("b8c08ae975d549f0ac0aa9dba9b4d929");
        analysisRequestDTO.setSampleType(SAMPLE_TYPE);
        analysisRequestDTO.setDateSampled("2024-10-07T11:21:13+00:00");
        analysisRequestDTO.setTemplate(TEMPLATE_ID);
        analysisRequestDTO.setProfiles(null);
        analysisRequestDTO.setAnalysesUids(new String[] {"0de6a738adb740bd990d48a8817b93f6"});
        analysisRequestDTO.setClientSampleID("9bd3a7de-fc0d-4601-8899-dab36bba399a");
        analysisRequestDTO.setAnalyses(null);
        analysisRequestDTO.setAnalyses(new Analyses[] {
            new Analyses(
                    "http://senaite:8080/senaite/clients/client-1/URI-0001/LAB-030",
                    "0de6a738adb740bd990d48a8817b93f6",
                    "http://senaite:8080/senaite/@@API/senaite/v1/analysis/0de6a738adb740bd990d48a8817b93f6")
        });
        analysisRequestDTO.setUid("8182d02098ce487086ea207c8f9df5b0");
        analysisRequestDTO.setClient(null);
        analysisRequestDTO.setReviewState(null);

        when(AnalysisRequestMapper.map(responseWrapper)).thenReturn(analysisRequestDTO);

        // Act
        AnalysisRequestDTO result = analysisRequestHandler.cancelAnalysisRequest(
                producerTemplate, cancelAnalysisRequest, "8182d02098ce487086ea207c8f9df5b0");

        // Verify
        assertEquals(analysisRequestDTO, result);
    }
}
