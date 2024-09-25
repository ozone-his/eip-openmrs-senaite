/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openmrs.senaite.routes.analyses;

import static org.apache.camel.builder.AdviceWith.adviceWith;
import static org.junit.jupiter.api.Assertions.*;

import com.ozonehis.eip.openmrs.senaite.Constants;
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
class GetAnalysesRouteTest extends CamelSpringTestSupport {
    private static final String GET_ANALYSES_ROUTE = "direct:senaite-get-analyses-route";

    //    @Override
    //    protected RoutesBuilder createRouteBuilder() {
    //        SenaiteClient senaiteClient = new SenaiteClient();
    //        senaiteClient.setSenaiteBaseUrl("http://localhost:8080/senaite");
    //        return new GetAnalysesRoute(senaiteClient);
    //    }

    @Override
    protected AbstractApplicationContext createApplicationContext() {
        return new StaticApplicationContext();
    }

    @BeforeEach
    public void setup() throws Exception {
        adviceWith("senaite-get-analyses-route", context, new AdviceWithRouteBuilder() {

            @Override
            public void configure() {
                weaveByToUri("*").replace().to("mock:get-analyses-route");
            }
        });

        Endpoint defaultEndpoint = context.getEndpoint(GET_ANALYSES_ROUTE);
        template.setDefaultEndpoint(defaultEndpoint);
    }

    @Test
    public void shouldGetAnalyses() throws Exception {
        Map<String, Object> headers = new HashMap<>();
        headers.put(
                Constants.HEADER_ANALYSES_GET_ENDPOINT,
                "http://localhost:8081/senaite/@@API/senaite/v1/analysis/50276c33096c472aa2c2958851c24899");

        // Expectations
        MockEndpoint mockCreatePartnerEndpoint = getMockEndpoint("mock:get-analyses-route");
        mockCreatePartnerEndpoint.expectedMessageCount(1);
        mockCreatePartnerEndpoint.expectedHeaderReceived(
                Constants.HEADER_ANALYSES_GET_ENDPOINT,
                "http://localhost:8081/senaite/@@API/senaite/v1/analysis/50276c33096c472aa2c2958851c24899");
        mockCreatePartnerEndpoint.setResultWaitTime(100);

        // Act
        template.send(GET_ANALYSES_ROUTE, exchange -> {
            exchange.getMessage().setHeaders(headers);
        });

        // Verify
        mockCreatePartnerEndpoint.assertIsSatisfied();
    }
}
