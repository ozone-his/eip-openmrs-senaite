/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openmrs.senaite.routes.analysisrequest;

import static org.apache.camel.builder.AdviceWith.adviceWith;

import com.ozonehis.eip.openmrs.senaite.Constants;
import com.ozonehis.eip.openmrs.senaite.model.analysisRequest.AnalysisRequestDTO;
import java.util.HashMap;
import java.util.Map;
import org.apache.camel.Endpoint;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.spring.junit5.CamelSpringTestSupport;
import org.apache.camel.test.spring.junit5.UseAdviceWith;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.StaticApplicationContext;

@UseAdviceWith
class CreateAnalysisRequestRouteTest extends CamelSpringTestSupport {
    private static final String CREATE_ANALYSIS_REQUEST_ROUTE = "direct:senaite-create-analysis-request-route";

    //    @Override
    //    protected RoutesBuilder createRouteBuilder() {
    //        SenaiteClient senaiteClient = new SenaiteClient();
    //        senaiteClient.setSenaiteBaseUrl("http://localhost:8080/senaite");
    //        return new CreateAnalysisRequestRoute(senaiteClient);
    //    }

    @Override
    protected AbstractApplicationContext createApplicationContext() {
        return new StaticApplicationContext();
    }

    @BeforeEach
    public void setup() throws Exception {
        adviceWith("senaite-create-analysis-request-route", context, new AdviceWithRouteBuilder() {

            @Override
            public void configure() {
                weaveByToUri("http://localhost:8080/senaite/@@API/senaite/v1/AnalysisRequest/create/*")
                        .replace()
                        .to("mock:create-analysis-request");
            }
        });

        Endpoint defaultEndpoint = context.getEndpoint(CREATE_ANALYSIS_REQUEST_ROUTE);
        template.setDefaultEndpoint(defaultEndpoint);
    }

    @Test
    public void shouldCreateAnalysisRequest() throws Exception {
        AnalysisRequestDTO analysisRequest = new AnalysisRequestDTO();
        analysisRequest.setClient("client_id");
        analysisRequest.setReviewState("sample_due");
        analysisRequest.setClientSampleID("client_sample_id");

        Map<String, Object> headers = new HashMap<>();
        headers.put(Constants.HEADER_CLIENT_UID, "client_uid");

        // Expectations
        MockEndpoint mockCreatePartnerEndpoint = getMockEndpoint("mock:create-analysis-request");
        mockCreatePartnerEndpoint.expectedMessageCount(1);
        mockCreatePartnerEndpoint.expectedHeaderReceived(Constants.HEADER_CLIENT_UID, "client_uid");
        mockCreatePartnerEndpoint.setResultWaitTime(100);

        // Act
        template.send(CREATE_ANALYSIS_REQUEST_ROUTE, exchange -> {
            exchange.getMessage().setHeaders(headers);
            exchange.getMessage().setBody(analysisRequest);
        });

        // Verify
        mockCreatePartnerEndpoint.assertIsSatisfied();
    }
}
