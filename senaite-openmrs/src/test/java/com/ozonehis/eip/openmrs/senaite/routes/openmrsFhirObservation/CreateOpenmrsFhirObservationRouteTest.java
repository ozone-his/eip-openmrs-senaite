/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openmrs.senaite.routes.openmrsFhirObservation;

import static org.apache.camel.builder.AdviceWith.adviceWith;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.camel.Endpoint;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.spring.junit5.CamelSpringTestSupport;
import org.apache.camel.test.spring.junit5.UseAdviceWith;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Reference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.StaticApplicationContext;

@UseAdviceWith
class CreateOpenmrsFhirObservationRouteTest extends CamelSpringTestSupport {
    private static final String CREATE_OBSERVATION_ROUTE = "direct:openmrs-create-observation-route";

    //    @Override
    //    protected RoutesBuilder createRouteBuilder() {
    //        OpenmrsFhirClient openmrsFhirClient = new OpenmrsFhirClient();
    //        openmrsFhirClient.setOpenmrsFhirBaseUrl("http://localhost:8080/openmrs/ws/fhir2/R4");
    //        return new CreateOpenmrsFhirObservationRoute(openmrsFhirClient);
    //    }

    @Override
    protected AbstractApplicationContext createApplicationContext() {
        return new StaticApplicationContext();
    }

    @BeforeEach
    public void setup() throws Exception {
        adviceWith("openmrs-create-observation-route", context, new AdviceWithRouteBuilder() {

            @Override
            public void configure() {
                weaveByToUri("http://localhost:8080/openmrs/ws/fhir2/R4/Observation")
                        .replace()
                        .to("mock:create-observation");
            }
        });

        Endpoint defaultEndpoint = context.getEndpoint(CREATE_OBSERVATION_ROUTE);
        template.setDefaultEndpoint(defaultEndpoint);
    }

    @Test
    public void shouldCreateObservation() throws Exception {
        Observation observation = new Observation();
        observation.setSubject(new Reference().setReference("patient_reference"));
        observation.setStatus(Observation.ObservationStatus.FINAL);

        // Expectations
        MockEndpoint mockCreatePartnerEndpoint = getMockEndpoint("mock:create-observation");
        mockCreatePartnerEndpoint.expectedMessageCount(1);
        mockCreatePartnerEndpoint.setResultWaitTime(100);

        // Act
        template.send(CREATE_OBSERVATION_ROUTE, exchange -> {
            exchange.getMessage().setBody(observation);
        });

        // Verify
        mockCreatePartnerEndpoint.assertIsSatisfied();
    }
}
