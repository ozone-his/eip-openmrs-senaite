/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openmrs.senaite.routes.openmrsFhirTask;

import static org.apache.camel.builder.AdviceWith.adviceWith;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.camel.Endpoint;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.spring.junit5.CamelSpringTestSupport;
import org.apache.camel.test.spring.junit5.UseAdviceWith;
import org.hl7.fhir.r4.model.Task;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.StaticApplicationContext;

@UseAdviceWith
class CreateOpenmrsFhirTaskRouteTest extends CamelSpringTestSupport {
    private static final String CREATE_TASK_ROUTE = "direct:openmrs-create-task-route";

    //    @Override
    //    protected RoutesBuilder createRouteBuilder() {
    //        OpenmrsFhirClient openmrsFhirClient = new OpenmrsFhirClient();
    //        openmrsFhirClient.setOpenmrsFhirBaseUrl("http://localhost:8080/openmrs/ws/fhir2/R4");
    //        return new CreateOpenmrsFhirTaskRoute(openmrsFhirClient);
    //    }

    @Override
    protected AbstractApplicationContext createApplicationContext() {
        return new StaticApplicationContext();
    }

    @BeforeEach
    public void setup() throws Exception {
        adviceWith("openmrs-create-task-route", context, new AdviceWithRouteBuilder() {

            @Override
            public void configure() {
                weaveByToUri("http://localhost:8080/openmrs/ws/fhir2/R4/Task")
                        .replace()
                        .to("mock:create-task");
            }
        });

        Endpoint defaultEndpoint = context.getEndpoint(CREATE_TASK_ROUTE);
        template.setDefaultEndpoint(defaultEndpoint);
    }

    @Test
    public void shouldCreateTask() throws Exception {
        Task task = new Task();
        task.setIntent(Task.TaskIntent.ORDER);
        task.addBasedOn().setReference("service_request_id").setType("ServiceRequest");

        // Expectations
        MockEndpoint mockCreatePartnerEndpoint = getMockEndpoint("mock:create-task");
        mockCreatePartnerEndpoint.expectedMessageCount(1);
        mockCreatePartnerEndpoint.setResultWaitTime(100);

        // Act
        template.send(CREATE_TASK_ROUTE, exchange -> {
            exchange.getMessage().setBody(task);
        });

        // Verify
        mockCreatePartnerEndpoint.assertIsSatisfied();
    }
}
