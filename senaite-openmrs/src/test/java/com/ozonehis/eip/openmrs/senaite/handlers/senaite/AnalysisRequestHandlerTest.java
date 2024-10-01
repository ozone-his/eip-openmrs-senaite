/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openmrs.senaite.handlers.senaite;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.MockitoAnnotations.openMocks;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ozonehis.eip.openmrs.senaite.Constants;
import com.ozonehis.eip.openmrs.senaite.model.analysisRequest.request.AnalysisRequest;
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
    void sendAnalysisRequest() {
        // Setup
        AnalysisRequest analysisRequest = new AnalysisRequest();
        analysisRequest.setContact("9af23e0590644669ab1811ede930c894");
        analysisRequest.setSampleType("0c5a92ea879a4dafa13f60762d93e52e");
        analysisRequest.setDateSampled("Tue Oct 24 01:51:17 UTC 2023");
        analysisRequest.setAnalyses(new String[] {"ca98e3ab2b4445df94b11d207a3b19e9"});
        analysisRequest.setClientSampleID("aed1e72f-5687-4dc5-9f81-d81fec568c07");
        analysisRequest.setReviewState("sample_due");

        Map<String, Object> headers = new HashMap<>();
        headers.put(Constants.HEADER_CLIENT_UID, "client-id");
    }

    @Test
    void getAnalysisRequestByClientIDAndClientSampleID() {}

    @Test
    void getAnalysisRequestByClientSampleID() {}

    @Test
    void cancelAnalysisRequest() {}

    @Test
    void doesAnalysisRequestExists() {}
}
