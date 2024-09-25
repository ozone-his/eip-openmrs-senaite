/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openmrs.senaite.routes.openmrsFhirEncounter;

import static org.apache.camel.builder.AdviceWith.adviceWith;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Date;
import org.apache.camel.Endpoint;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.spring.junit5.CamelSpringTestSupport;
import org.apache.camel.test.spring.junit5.UseAdviceWith;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.Reference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.StaticApplicationContext;

@UseAdviceWith
class CreateOpenmrsFhirEncounterRouteTest extends CamelSpringTestSupport {
    private static final String CREATE_ENCOUNTER_ROUTE = "direct:openmrs-create-encounter-route";

    //    @Override
    //    protected RoutesBuilder createRouteBuilder() {
    //        OpenmrsFhirClient openmrsFhirClient = new OpenmrsFhirClient();
    //        openmrsFhirClient.setOpenmrsFhirBaseUrl("http://localhost:8080/openmrs/ws/fhir2/R4");
    //        return new CreateOpenmrsFhirEncounterRoute(openmrsFhirClient);
    //    }

    @Override
    protected AbstractApplicationContext createApplicationContext() {
        return new StaticApplicationContext();
    }

    @BeforeEach
    public void setup() throws Exception {
        adviceWith("openmrs-create-encounter-route", context, new AdviceWithRouteBuilder() {

            @Override
            public void configure() {
                weaveByToUri("http://localhost:8080/openmrs/ws/fhir2/R4/Encounter")
                        .replace()
                        .to("mock:create-encounter");
            }
        });

        Endpoint defaultEndpoint = context.getEndpoint(CREATE_ENCOUNTER_ROUTE);
        template.setDefaultEndpoint(defaultEndpoint);
    }

    @Test
    public void shouldCreateEncounter() throws Exception {
        Encounter encounter = new Encounter();
        encounter.setSubject(new Reference().setReference("patient_reference"));
        encounter.setPeriod(new Period().setStart(new Date()));

        // Expectations
        MockEndpoint mockCreatePartnerEndpoint = getMockEndpoint("mock:create-encounter");
        mockCreatePartnerEndpoint.expectedMessageCount(1);
        mockCreatePartnerEndpoint.setResultWaitTime(100);

        // Act
        template.send(CREATE_ENCOUNTER_ROUTE, exchange -> {
            exchange.getMessage().setBody(encounter);
        });

        // Verify
        mockCreatePartnerEndpoint.assertIsSatisfied();
    }
}
