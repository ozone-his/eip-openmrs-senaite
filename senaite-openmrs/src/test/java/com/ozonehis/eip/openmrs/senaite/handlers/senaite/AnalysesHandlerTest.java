/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openmrs.senaite.handlers.senaite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.openMocks;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ozonehis.eip.openmrs.senaite.Constants;
import com.ozonehis.eip.openmrs.senaite.model.SenaiteResponseWrapper;
import com.ozonehis.eip.openmrs.senaite.model.analyses.AnalysesDTO;
import com.ozonehis.eip.openmrs.senaite.model.analyses.AnalysesMapper;
import com.ozonehis.eip.openmrs.senaite.model.analyses.response.AnalysesItem;
import java.util.HashMap;
import java.util.Map;
import org.apache.camel.ProducerTemplate;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

class AnalysesHandlerTest {

    @Mock
    private ProducerTemplate producerTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private AnalysesHandler analysesHandler;

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
    void shouldReturnAnalysesGivenAnalysesApiUrl() throws JsonProcessingException {
        // Setup
        String analysesApiUrl = "http://test.com/api/analyses";
        String responseBody = new Utils().readJSON("senaite/response/get-analyses.json");
        Map<String, Object> headers = new HashMap<>();
        headers.put(Constants.HEADER_ANALYSES_GET_ENDPOINT, analysesApiUrl);

        // Mock
        when(producerTemplate.requestBodyAndHeaders(
                        eq("direct:senaite-get-analyses-route"), isNull(), eq(headers), eq(String.class)))
                .thenReturn(responseBody);

        TypeReference<SenaiteResponseWrapper<AnalysesItem>> typeReference = new TypeReference<>() {};
        SenaiteResponseWrapper<AnalysesItem> responseWrapper = objectMapper.readValue(responseBody, typeReference);

        AnalysesDTO analysesDTO = new AnalysesDTO();
        analysesDTO.setResult("4.5");
        analysesDTO.setResultCaptureDate("2024-09-30T11:21:36+00:00");
        analysesDTO.setDescription(
                "Blood test to measure the number of red blood cells.(679AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA)");
        when(AnalysesMapper.map(responseWrapper)).thenReturn(analysesDTO);

        // Act
        AnalysesDTO result = analysesHandler.getAnalysesByAnalysesApiUrl(producerTemplate, analysesApiUrl);

        // Verify
        assertEquals(analysesDTO, result);
    }
}
