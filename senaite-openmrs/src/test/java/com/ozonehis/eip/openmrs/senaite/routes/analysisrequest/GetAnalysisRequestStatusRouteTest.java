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
class GetAnalysisRequestStatusRouteTest extends CamelSpringTestSupport {
    private static final String ROUTE = "direct:senaite-get-analysis-request-status-route";

    @Override
    protected RoutesBuilder createRouteBuilder() {
        SenaiteConfig senaiteConfig = new SenaiteConfig();
        senaiteConfig.setSenaiteBaseUrl("http://localhost:8080/senaite");
        senaiteConfig.setSenaiteUsername("admin");
        senaiteConfig.setSenaitePassword("password");
        return new GetAnalysisRequestStatusRoute(senaiteConfig);
    }

    @Override
    protected AbstractApplicationContext createApplicationContext() {
        return new StaticApplicationContext();
    }

    @BeforeEach
    public void setup() throws Exception {
        adviceWith("senaite-get-analysis-request-status-route", context, new AdviceWithRouteBuilder() {

            @Override
            public void configure() {
                interceptSendToEndpoint("http*").skipSendToOriginalEndpoint().to("mock:senaite");
            }
        });
        context.start();
    }

    @Test
    public void shouldQuerySampleCatalogWithoutLoadingTheSample() throws Exception {
        Map<String, Object> headers = new HashMap<>();
        headers.put(Constants.HEADER_CLIENT_ID, "client-1");
        headers.put(Constants.HEADER_CLIENT_SAMPLE_ID, "sample-1");
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
        assertTrue(uri.contains("senaite/@@API/senaite/v1/AnalysisRequest?"), uri);
        assertTrue(uri.contains("getClientSampleID=sample-1"), uri);
        assertTrue(uri.contains("getClientID=client-1"), uri);
        assertTrue(uri.contains("catalog=senaite_catalog_sample"), uri);
        assertFalse(uri.contains("complete"), uri);
    }
}
