/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openmrs.senaite.routes.openmrsFhirTask;

import static org.apache.camel.builder.AdviceWith.adviceWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import ca.uhn.fhir.context.FhirContext;
import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
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

    @Override
    public boolean isUseAdviceWith() {
        return true;
    }

    @BeforeEach
    public void setup() throws Exception {
        adviceWith("openmrs-get-task-by-status-route", context, new AdviceWithRouteBuilder() {

            @Override
            public void configure() {
                weaveByToUri("fhir:*").replace().to("mock:get-task-by-status-route");
            }
        });
        context.start();

        Endpoint defaultEndpoint = context.getEndpoint(GET_BY_STATUS_TASK_ROUTE);
        template.setDefaultEndpoint(defaultEndpoint);
    }

    @Test
    public void shouldGetAllPagesOfTasksWithStatusRequestedOrAccepted() throws Exception {
        String nextPageUrl = "https://openmrs.example/ws/fhir2/R4?_getpages=page-id&_getpagesoffset=10&_count=10";
        Bundle firstPage = bundleWithTask("first_task");
        firstPage.addLink().setRelation(Bundle.LINK_NEXT).setUrl(nextPageUrl);
        Bundle secondPage = bundleWithTask("second_task");

        // Expectations
        MockEndpoint mockEndpoint = getMockEndpoint("mock:get-task-by-status-route");
        mockEndpoint.expectedMessageCount(2);
        mockEndpoint.expectedHeaderValuesReceivedInAnyOrder(
                "CamelFhir.url", GetOpenmrsFhirTaskByStatusRoute.GET_BY_STATUS_ENDPOINT, nextPageUrl);
        mockEndpoint.setResultWaitTime(100);
        mockEndpoint.whenAnyExchangeReceived(exchange -> {
            String requestedUrl = exchange.getMessage().getHeader("CamelFhir.url", String.class);
            Bundle response = nextPageUrl.equals(requestedUrl) ? secondPage : firstPage;
            exchange.getMessage().setBody(FhirContext.forR4().newJsonParser().encodeResourceToString(response));
        });

        // Act
        Exchange result = template.request(
                GET_BY_STATUS_TASK_ROUTE, exchange -> exchange.getMessage().setBody(null));

        // Verify
        mockEndpoint.assertIsSatisfied();
        Bundle resultBundle = result.getMessage().getBody(Bundle.class);
        assertEquals(2, resultBundle.getEntry().size());
        assertEquals(
                "first_task",
                resultBundle.getEntryFirstRep().getResource().getIdElement().getIdPart());
        assertEquals(
                "second_task",
                resultBundle.getEntry().get(1).getResource().getIdElement().getIdPart());
        assertFalse(result.getMessage().getHeaders().containsKey("CamelFhir.url"));
    }

    private Bundle bundleWithTask(String taskId) {
        Task task = new Task();
        task.setId(taskId);
        task.setIntent(Task.TaskIntent.ORDER);
        task.addBasedOn().setReference("service_request_id").setType("ServiceRequest");
        Bundle bundle = new Bundle();
        bundle.addEntry().setResource(task);
        return bundle;
    }
}
