/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openmrs.senaite.routes.client;

import static org.apache.camel.builder.AdviceWith.adviceWith;
import static org.junit.jupiter.api.Assertions.*;

import com.ozonehis.eip.openmrs.senaite.config.SenaiteConfig;
import com.ozonehis.eip.openmrs.senaite.model.client.ClientDTO;
import com.ozonehis.eip.openmrs.senaite.routes.analysisrequest.CreateAnalysisRequestRoute;
import org.apache.camel.Endpoint;
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
class CreateClientRouteTest extends CamelSpringTestSupport {
    private static final String CREATE_CLIENT_ROUTE = "direct:senaite-create-client-route";

    @Override
    protected RoutesBuilder createRouteBuilder() {
        SenaiteConfig senaiteConfig = new SenaiteConfig();
        senaiteConfig.setSenaiteBaseUrl("http://localhost:8080/senaite");
        senaiteConfig.setSenaiteUsername("admin");
        senaiteConfig.setSenaitePassword("password");
        return new CreateClientRoute(senaiteConfig);
    }

    @Override
    protected AbstractApplicationContext createApplicationContext() {
        return new StaticApplicationContext();
    }

    @BeforeEach
    public void setup() throws Exception {
        adviceWith("senaite-create-client-route", context, new AdviceWithRouteBuilder() {

            @Override
            public void configure() {
                weaveByToUri("http://localhost:8080/senaite/@@API/senaite/v1/create")
                        .replace()
                        .to("mock:create-client");
            }
        });

        Endpoint defaultEndpoint = context.getEndpoint(CREATE_CLIENT_ROUTE);
        template.setDefaultEndpoint(defaultEndpoint);
    }

    @Test
    public void shouldCreateClient() throws Exception {
        ClientDTO client = new ClientDTO();
        client.setTitle("John Doe");
        client.setClientID("client_id");

        // Expectations
        MockEndpoint mockEndpoint = getMockEndpoint("mock:create-client");
        mockEndpoint.expectedMessageCount(1);
        mockEndpoint.setResultWaitTime(100);

        // Act
        template.send(CREATE_CLIENT_ROUTE, exchange -> {
            exchange.getMessage().setBody(client);
        });

        // Verify
        mockEndpoint.assertIsSatisfied();
    }
}
