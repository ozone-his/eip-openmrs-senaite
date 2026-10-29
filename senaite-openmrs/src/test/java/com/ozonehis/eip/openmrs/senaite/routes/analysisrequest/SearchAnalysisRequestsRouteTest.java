/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openmrs.senaite.routes.analysisrequest;

import static org.apache.camel.builder.AdviceWith.adviceWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.ozonehis.eip.openmrs.senaite.Constants;
import com.ozonehis.eip.openmrs.senaite.config.SenaiteConfig;
import java.util.HashMap;
import java.util.Map;
import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.spring.junit5.CamelSpringTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.StaticApplicationContext;

class SearchAnalysisRequestsRouteTest extends CamelSpringTestSupport {

    private static final String SEARCH_ROUTE = "direct:senaite-search-analysis-requests-route";

    private static final String BASE_URL = "http://localhost:8080/senaite";

    @Override
    protected RoutesBuilder createRouteBuilder() {
        SenaiteConfig senaiteConfig = new SenaiteConfig();
        senaiteConfig.setSenaiteBaseUrl(BASE_URL);
        senaiteConfig.setSenaiteUsername("admin");
        senaiteConfig.setSenaitePassword("password");
        return new SearchAnalysisRequestsRoute(senaiteConfig);
    }

    @Override
    protected AbstractApplicationContext createApplicationContext() {
        return new StaticApplicationContext();
    }

    @BeforeEach
    public void setup() throws Exception {
        adviceWith("senaite-search-analysis-requests-route", context, new AdviceWithRouteBuilder() {

            @Override
            public void configure() {
                weaveByToUri(BASE_URL + "/@@API/senaite/v1/AnalysisRequest*")
                        .replace()
                        .to("mock:senaite");
            }
        });

        Endpoint defaultEndpoint = context.getEndpoint(SEARCH_ROUTE);
        template.setDefaultEndpoint(defaultEndpoint);
    }

    @Test
    public void shouldSendTheDateQueryAsAnHttpHeader() throws Exception {
        String query = AnalysisRequestCatalogQuery.publishedSince("2026-10-28T14:00:00", 0);
        Map<String, Object> headers = new HashMap<>();
        headers.put(Constants.HEADER_SENAITE_SEARCH_QUERY, query);

        MockEndpoint mockEndpoint = getMockEndpoint("mock:senaite");
        mockEndpoint.expectedMessageCount(1);
        mockEndpoint.setResultWaitTime(100);

        template.send(SEARCH_ROUTE, exchange -> exchange.getMessage().setHeaders(headers));

        mockEndpoint.assertIsSatisfied();
        String httpQuery = mockEndpoint.getExchanges().get(0).getMessage().getHeader(Exchange.HTTP_QUERY, String.class);
        assertEquals(query, httpQuery);
        assertFalse(SearchAnalysisRequestsRoute.searchEndpoint(BASE_URL).contains("query:record"));
    }
}
