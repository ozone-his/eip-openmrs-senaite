/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openmrs.senaite.handlers.senaite;

import static org.junit.jupiter.api.Assertions.*;
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
    void sendAnalysisRequest() throws JsonProcessingException {
        // Setup
        String responseBody = new Utils().readJSON("senaite/response/create-analysis-request.json");
        Map<String, Object> headers = new HashMap<>();
        headers.put(Constants.HEADER_CLIENT_UID, "7f4aebaf3d4a4a2f8e8ebb5881d4ce73");

        AnalysisRequest analysisRequest = new AnalysisRequest();
        analysisRequest.setContact("1baac45668fc49cbbd5c4fd35d804b72");
        analysisRequest.setSampleType("db97756c89f143408a18e0d152d0d337");
        analysisRequest.setDateSampled("2024-10-07T08:20:31Z");
        analysisRequest.setTemplate("7a09878065314fe29f8fc994fd2c8447");
        analysisRequest.setAnalyses(new String[] {"8c057c866cf4439ca2c78e2bfc89f6a0"});
        analysisRequest.setClientSampleID("bbaea498-e046-43c6-bf9c-dbbc7d39f38c");
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
        analysisRequestDTO.setContact("1baac45668fc49cbbd5c4fd35d804b72");
        analysisRequestDTO.setSampleType("db97756c89f143408a18e0d152d0d337");
        analysisRequestDTO.setDateSampled("2024-10-07T08:20:31+00:00");
        analysisRequestDTO.setTemplate("7a09878065314fe29f8fc994fd2c8447");
        analysisRequestDTO.setProfiles(null);
        analysisRequestDTO.setAnalysesUids(new String[] {"06d1e902317047d1b90b27df5122ab78"});
        analysisRequestDTO.setClientSampleID("bbaea498-e046-43c6-bf9c-dbbc7d39f38c");
        analysisRequestDTO.setAnalyses(null);
        analysisRequestDTO.setAnalyses(new Analyses[] {
            new Analyses(
                    "http://senaite:8080/senaite/clients/client-1/URI-0001/LAB-030",
                    "06d1e902317047d1b90b27df5122ab78",
                    "http://senaite:8080/senaite/@@API/senaite/v1/analysis/06d1e902317047d1b90b27df5122ab78")
        });
        analysisRequestDTO.setUid("99afa09663054c18b1ae7a1cd2cd7693");

        when(AnalysisRequestMapper.map(responseWrapper)).thenReturn(analysisRequestDTO);

        // Act
        AnalysisRequestDTO result = analysisRequestHandler.sendAnalysisRequest(
                producerTemplate, analysisRequest, "7f4aebaf3d4a4a2f8e8ebb5881d4ce73");

        // Verify
        assertEquals(analysisRequestDTO, result);
    }

    @Test
    void getAnalysisRequestByClientIDAndClientSampleID() throws JsonProcessingException {
        // Setup
        String responseBody = new Utils().readJSON("senaite/response/get-analysis-request.json");
        Map<String, Object> headers = new HashMap<>();
        headers.put(Constants.HEADER_CLIENT_ID, "7f4aebaf3d4a4a2f8e8ebb5881d4ce73");
        headers.put(Constants.HEADER_CLIENT_SAMPLE_ID, "bbaea498-e046-43c6-bf9c-dbbc7d39f38c");

        // Mock
        when(producerTemplate.requestBodyAndHeaders(
                        eq("direct:senaite-get-analysis-request-route"), isNull(), eq(headers), eq(String.class)))
                .thenReturn(responseBody);

        TypeReference<SenaiteResponseWrapper<AnalysisRequestItem>> typeReference = new TypeReference<>() {};
        SenaiteResponseWrapper<AnalysisRequestItem> responseWrapper =
                objectMapper.readValue(responseBody, typeReference);

        AnalysisRequestDTO analysisRequestDTO = new AnalysisRequestDTO();
        analysisRequestDTO.setContact("1baac45668fc49cbbd5c4fd35d804b72");
        analysisRequestDTO.setSampleType("db97756c89f143408a18e0d152d0d337");
        analysisRequestDTO.setDateSampled("2024-10-07T08:20:31+00:00");
        analysisRequestDTO.setTemplate("7a09878065314fe29f8fc994fd2c8447");
        analysisRequestDTO.setProfiles(null);
        analysisRequestDTO.setAnalysesUids(new String[] {"06d1e902317047d1b90b27df5122ab78"});
        analysisRequestDTO.setClientSampleID("bbaea498-e046-43c6-bf9c-dbbc7d39f38c");
        analysisRequestDTO.setAnalyses(null);
        analysisRequestDTO.setAnalyses(new Analyses[] {
            new Analyses(
                    "http://senaite:8080/senaite/clients/client-1/URI-0001/LAB-030",
                    "06d1e902317047d1b90b27df5122ab78",
                    "http://senaite:8080/senaite/@@API/senaite/v1/analysis/06d1e902317047d1b90b27df5122ab78")
        });
        analysisRequestDTO.setUid("99afa09663054c18b1ae7a1cd2cd7693");
        analysisRequestDTO.setClient("7f4aebaf3d4a4a2f8e8ebb5881d4ce73");
        analysisRequestDTO.setReviewState("sample_due");

        when(AnalysisRequestMapper.map(responseWrapper)).thenReturn(analysisRequestDTO);

        // Act
        AnalysisRequestDTO result = analysisRequestHandler.getAnalysisRequestByClientIDAndClientSampleID(
                producerTemplate, "7f4aebaf3d4a4a2f8e8ebb5881d4ce73", "bbaea498-e046-43c6-bf9c-dbbc7d39f38c");

        // Verify
        assertEquals(analysisRequestDTO, result);
    }

    @Test
    void getAnalysisRequestByClientSampleID() throws JsonProcessingException {
        // Setup
        String responseBody = new Utils().readJSON("senaite/response/get-analysis-request.json");
        Map<String, Object> headers = new HashMap<>();
        headers.put(Constants.HEADER_CLIENT_SAMPLE_ID, "bbaea498-e046-43c6-bf9c-dbbc7d39f38c");

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
        analysisRequestDTO.setContact("1baac45668fc49cbbd5c4fd35d804b72");
        analysisRequestDTO.setSampleType("db97756c89f143408a18e0d152d0d337");
        analysisRequestDTO.setDateSampled("2024-10-07T08:20:31+00:00");
        analysisRequestDTO.setTemplate("7a09878065314fe29f8fc994fd2c8447");
        analysisRequestDTO.setProfiles(null);
        analysisRequestDTO.setAnalysesUids(new String[] {"06d1e902317047d1b90b27df5122ab78"});
        analysisRequestDTO.setClientSampleID("bbaea498-e046-43c6-bf9c-dbbc7d39f38c");
        analysisRequestDTO.setAnalyses(null);
        analysisRequestDTO.setAnalyses(new Analyses[] {
            new Analyses(
                    "http://senaite:8080/senaite/clients/client-1/URI-0001/LAB-030",
                    "06d1e902317047d1b90b27df5122ab78",
                    "http://senaite:8080/senaite/@@API/senaite/v1/analysis/06d1e902317047d1b90b27df5122ab78")
        });
        analysisRequestDTO.setUid("99afa09663054c18b1ae7a1cd2cd7693");
        analysisRequestDTO.setClient("7f4aebaf3d4a4a2f8e8ebb5881d4ce73");
        analysisRequestDTO.setReviewState("sample_due");

        when(AnalysisRequestMapper.map(responseWrapper)).thenReturn(analysisRequestDTO);

        // Act
        AnalysisRequestDTO result = analysisRequestHandler.getAnalysisRequestByClientSampleID(
                producerTemplate, "bbaea498-e046-43c6-bf9c-dbbc7d39f38c");

        // Verify
        assertEquals(analysisRequestDTO, result);
    }

    @Test
    void cancelAnalysisRequest() {}
}
