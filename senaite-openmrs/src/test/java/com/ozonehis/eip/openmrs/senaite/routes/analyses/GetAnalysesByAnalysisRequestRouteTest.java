/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openmrs.senaite.routes.analyses;

import static org.apache.camel.builder.AdviceWith.adviceWith;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ozonehis.eip.openmrs.senaite.Constants;
import com.ozonehis.eip.openmrs.senaite.config.SenaiteConfig;
import java.util.HashMap;
import java.util.Map;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.spring.junit5.CamelSpringTestSupport;
import org.apache.camel.test.spring.junit5.UseAdviceWith;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.StaticApplicationContext;

@UseAdviceWith
class GetAnalysesByAnalysisRequestRouteTest extends CamelSpringTestSupport {
    private static final String ROUTE = "direct:senaite-get-analyses-by-analysis-request-route";

    @Override
    protected RoutesBuilder createRouteBuilder() {
        SenaiteConfig senaiteConfig = new SenaiteConfig();
        senaiteConfig.setSenaiteBaseUrl("http://localhost:8080/senaite");
        senaiteConfig.setSenaiteUsername("admin");
        senaiteConfig.setSenaitePassword("password");
        return new GetAnalysesByAnalysisRequestRoute(senaiteConfig);
    }

    @Override
    protected AbstractApplicationContext createApplicationContext() {
        return new StaticApplicationContext();
    }

    @BeforeEach
    public void setup() throws Exception {
        adviceWith("senaite-get-analyses-by-analysis-request-route", context, new AdviceWithRouteBuilder() {

            @Override
            public void configure() {
                interceptSendToEndpoint("http*").skipSendToOriginalEndpoint().to("mock:senaite");
            }
        });
        context.start();
    }

    @Test
    public void shouldQueryAllAnalysesOfAnalysisRequestInOneRequest() throws Exception {
        Map<String, Object> headers = new HashMap<>();
        headers.put(Constants.HEADER_ANALYSIS_REQUEST_UID, "99afa09663054c18b1ae7a1cd2cd7693");
        MockEndpoint mockEndpoint = getMockEndpoint("mock:senaite");
        mockEndpoint.expectedMessageCount(1);

        // Act
        template.send(ROUTE, exchange -> exchange.getMessage().setHeaders(headers));

        // Verify
        mockEndpoint.assertIsSatisfied();
        // toD to an http endpoint keeps the host as the endpoint and moves path and query into headers
        Message message = mockEndpoint.getExchanges().get(0).getMessage();
        String uri = message.getHeader(Exchange.HTTP_PATH, String.class) + "?"
                + message.getHeader(Exchange.HTTP_QUERY, String.class);
        assertTrue(uri.contains("senaite/@@API/senaite/v1/Analysis?"), uri);
        assertTrue(uri.contains("getAncestorsUIDs=99afa09663054c18b1ae7a1cd2cd7693"), uri);
        assertTrue(uri.contains("catalog=senaite_catalog_analysis"), uri);
        assertTrue(uri.contains("complete=true"), uri);
        assertTrue(uri.contains("limit=" + GetAnalysesByAnalysisRequestRoute.MAX_ANALYSES_PER_REQUEST), uri);
    }
}
