/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openmrs.senaite.handlers.senaite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ozonehis.eip.openmrs.senaite.Constants;
import com.ozonehis.eip.openmrs.senaite.model.SenaiteResponseWrapper;
import com.ozonehis.eip.openmrs.senaite.model.analysisRequestTemplate.AnalysisRequestTemplateDTO;
import com.ozonehis.eip.openmrs.senaite.model.analysisRequestTemplate.AnalysisRequestTemplateMapper;
import com.ozonehis.eip.openmrs.senaite.model.analysisRequestTemplate.response.Analyses;
import com.ozonehis.eip.openmrs.senaite.model.analysisRequestTemplate.response.AnalysisRequestTemplateItem;
import com.ozonehis.eip.openmrs.senaite.model.analysisRequestTemplate.response.SampleType;
import java.util.HashMap;
import java.util.Map;
import org.apache.camel.ProducerTemplate;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

class AnalysisRequestTemplateHandlerTest {

    @Mock
    private ProducerTemplate producerTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private AnalysisRequestTemplateHandler analysisRequestTemplateHandler;

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
    void shouldReturnAnalysisRequestTemplateGivenServiceRequestCode() throws JsonProcessingException {
        // Setup
        String responseBody = new Utils().readJSON("senaite/response/get-analysis-request-template.json");
        Map<String, Object> headers = new HashMap<>();
        headers.put(Constants.HEADER_DESCRIPTION, "serviceRequestCode");

        // Mock
        when(producerTemplate.requestBodyAndHeaders(
                        eq("direct:senaite-get-analysis-request-template-route"),
                        isNull(),
                        eq(headers),
                        eq(String.class)))
                .thenReturn(responseBody);

        TypeReference<SenaiteResponseWrapper<AnalysisRequestTemplateItem>> typeReference = new TypeReference<>() {};
        SenaiteResponseWrapper<AnalysisRequestTemplateItem> responseWrapper =
                objectMapper.readValue(responseBody, typeReference);

        AnalysisRequestTemplateDTO analysisRequestTemplateDTO = new AnalysisRequestTemplateDTO();
        analysisRequestTemplateDTO.setUid("7a09878065314fe29f8fc994fd2c8447");
        analysisRequestTemplateDTO.setPath("/senaite/bika_setup/bika_artemplates/artemplate-8");
        analysisRequestTemplateDTO.setAnalysisProfile(null);
        analysisRequestTemplateDTO.setAnalyses(
                new Analyses[] {new Analyses("8c057c866cf4439ca2c78e2bfc89f6a0", "part-1")});
        analysisRequestTemplateDTO.setSampleType(new SampleType(
                "http://senaite:8080/senaite/bika_setup/bika_sampletypes/sampletype-2",
                "db97756c89f143408a18e0d152d0d337",
                "http://senaite:8080/senaite/@@API/senaite/v1/sampletype/db97756c89f143408a18e0d152d0d337"));

        when(AnalysisRequestTemplateMapper.map(responseWrapper)).thenReturn(analysisRequestTemplateDTO);

        // Act
        AnalysisRequestTemplateDTO result =
                analysisRequestTemplateHandler.getAnalysisRequestTemplateByServiceRequestCode(
                        producerTemplate, "serviceRequestCode");

        // Verify
        assertEquals(analysisRequestTemplateDTO, result);
    }
}
