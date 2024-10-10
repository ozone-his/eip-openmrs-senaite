/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openmrs.senaite.routes.openmrsFhirTask;

import static org.apache.camel.builder.AdviceWith.adviceWith;

import ca.uhn.fhir.context.FhirContext;
import org.apache.camel.Endpoint;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.spring.junit5.CamelSpringTestSupport;
import org.apache.camel.test.spring.junit5.UseAdviceWith;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Task;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.StaticApplicationContext;

@UseAdviceWith
class GetOpenmrsFhirTaskByStatusRouteTest extends CamelSpringTestSupport {
    private static final String GET_BY_STATUS_TASK_ROUTE = "direct:openmrs-get-task-by-status-route";

    @Override
    protected RoutesBuilder createRouteBuilder() {
        return new GetOpenmrsFhirTaskByStatusRoute();
    }

    @Override
    protected AbstractApplicationContext createApplicationContext() {
        return new StaticApplicationContext();
    }

    @BeforeEach
    public void setup() throws Exception {
        adviceWith("openmrs-get-task-by-status-route", context, new AdviceWithRouteBuilder() {

            @Override
            public void configure() {
                weaveByToUri("fhir://search/searchByUrl?url=/Task?status=requested,accepted")
                        .replace()
                        .to("mock:get-task-by-status-route");
            }
        });

        Endpoint defaultEndpoint = context.getEndpoint(GET_BY_STATUS_TASK_ROUTE);
        template.setDefaultEndpoint(defaultEndpoint);
    }

    @Test
    public void shouldGetTaskWithStatusRequestedOrAccepted() throws Exception {
        Task task = new Task();
        task.setIntent(Task.TaskIntent.ORDER);
        task.addBasedOn().setReference("service_request_id").setType("ServiceRequest");
        Bundle bundle = new Bundle();
        bundle.addEntry().setResource(task);
        String bundleJson = FhirContext.forR4().newJsonParser().encodeResourceToString(bundle);

        // Expectations
        MockEndpoint mockEndpoint = getMockEndpoint("mock:get-task-by-status-route");
        mockEndpoint.expectedMessageCount(1);
        mockEndpoint.setResultWaitTime(100);
        mockEndpoint.whenAnyExchangeReceived(exchange -> exchange.getIn().setBody(bundleJson));

        // Act
        template.send(GET_BY_STATUS_TASK_ROUTE, exchange -> {
            exchange.getMessage().setBody(null);
        });

        // Verify
        mockEndpoint.assertIsSatisfied();
    }
}
