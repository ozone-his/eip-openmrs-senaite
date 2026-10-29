/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openmrs.senaite.routes.analysisrequest;

import static org.apache.camel.builder.AdviceWith.adviceWith;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ozonehis.eip.openmrs.senaite.Constants;
import com.ozonehis.eip.openmrs.senaite.config.SenaiteConfig;
import java.util.HashMap;
import java.util.Map;
import org.apache.camel.Endpoint;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.spring.junit5.CamelSpringTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.StaticApplicationContext;

class GetAnalysisRequestRouteTest extends CamelSpringTestSupport {

    private static final String GET_ANALYSIS_REQUEST_ROUTE = "direct:senaite-get-analysis-request-route";

    private static final String BASE_URL = "http://localhost:8080/senaite";

    @Override
    protected RoutesBuilder createRouteBuilder() {
        SenaiteConfig senaiteConfig = new SenaiteConfig();
        senaiteConfig.setSenaiteBaseUrl(BASE_URL);
        senaiteConfig.setSenaiteUsername("admin");
        senaiteConfig.setSenaitePassword("password");
        return new GetAnalysisRequestRoute(senaiteConfig);
    }

    @Override
    protected AbstractApplicationContext createApplicationContext() {
        return new StaticApplicationContext();
    }

    @BeforeEach
    public void setup() throws Exception {
        adviceWith("senaite-get-analysis-request-route", context, new AdviceWithRouteBuilder() {

            @Override
            public void configure() {
                weaveByToUri(BASE_URL + "/@@API/senaite/v1/AnalysisRequest*")
                        .replace()
                        .to("mock:senaite");
            }
        });

        Endpoint defaultEndpoint = context.getEndpoint(GET_ANALYSIS_REQUEST_ROUTE);
        template.setDefaultEndpoint(defaultEndpoint);
    }

    @Test
    public void shouldOmitCompleteUnlessTheFullObjectIsRequested() {
        String catalogEndpoint = GetAnalysisRequestRoute.catalogEndpoint(BASE_URL);
        String completeEndpoint = GetAnalysisRequestRoute.completeEndpoint(BASE_URL);

        assertFalse(catalogEndpoint.contains("complete=true"));
        assertTrue(catalogEndpoint.contains("catalog=senaite_catalog_sample"));
        assertTrue(completeEndpoint.contains("complete=true"));
        assertTrue(completeEndpoint.startsWith(catalogEndpoint));
    }

    @Test
    public void shouldGetAnalysisRequestFromCatalogWithoutLoadingTheObject() throws Exception {
        assertRouted(sampleHeaders());
    }

    @Test
    public void shouldLoadTheFullAnalysisRequestWhenCompleteIsRequested() throws Exception {
        Map<String, Object> headers = sampleHeaders();
        headers.put(Constants.HEADER_SENAITE_COMPLETE, Boolean.TRUE);
        assertRouted(headers);
    }

    private void assertRouted(Map<String, Object> headers) throws InterruptedException {
        MockEndpoint mockEndpoint = getMockEndpoint("mock:senaite");
        mockEndpoint.reset();
        mockEndpoint.expectedMessageCount(1);
        mockEndpoint.setResultWaitTime(100);

        template.send(
                GET_ANALYSIS_REQUEST_ROUTE, exchange -> exchange.getMessage().setHeaders(headers));

        mockEndpoint.assertIsSatisfied();
    }

    private Map<String, Object> sampleHeaders() {
        Map<String, Object> headers = new HashMap<>();
        headers.put(Constants.HEADER_CLIENT_SAMPLE_ID, "client_sample_id");
        headers.put(Constants.HEADER_CLIENT_ID, "client_id");
        return headers;
    }
}
