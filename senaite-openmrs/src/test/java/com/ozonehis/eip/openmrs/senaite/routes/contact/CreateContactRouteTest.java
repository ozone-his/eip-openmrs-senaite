/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openmrs.senaite.routes.contact;

import static org.apache.camel.builder.AdviceWith.adviceWith;
import static org.junit.jupiter.api.Assertions.*;

import com.ozonehis.eip.openmrs.senaite.model.contact.ContactDTO;
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
class CreateContactRouteTest extends CamelSpringTestSupport {
    private static final String CREATE_CONTACT_ROUTE = "direct:senaite-create-contact-route";

    //    @Override
    //    protected RoutesBuilder createRouteBuilder() {
    //        SenaiteClient senaiteClient = new SenaiteClient();
    //        senaiteClient.setSenaiteBaseUrl("http://localhost:8080/senaite");
    //        return new CreateContactRoute(senaiteClient);
    //    }

    @Override
    protected AbstractApplicationContext createApplicationContext() {
        return new StaticApplicationContext();
    }

    @BeforeEach
    public void setup() throws Exception {
        adviceWith("senaite-create-contact-route", context, new AdviceWithRouteBuilder() {

            @Override
            public void configure() {
                weaveByToUri("http://localhost:8080/senaite/@@API/senaite/v1/create")
                        .replace()
                        .to("mock:create-contact");
            }
        });

        Endpoint defaultEndpoint = context.getEndpoint(CREATE_CONTACT_ROUTE);
        template.setDefaultEndpoint(defaultEndpoint);
    }

    @Test
    public void shouldCreateContact() throws Exception {
        ContactDTO contact = new ContactDTO();
        contact.setFirstName("John");
        contact.setSurname("Doe");

        // Expectations
        MockEndpoint mockCreatePartnerEndpoint = getMockEndpoint("mock:create-contact");
        mockCreatePartnerEndpoint.expectedMessageCount(1);
        mockCreatePartnerEndpoint.setResultWaitTime(100);

        // Act
        template.send(CREATE_CONTACT_ROUTE, exchange -> {
            exchange.getMessage().setBody(contact);
        });

        // Verify
        mockCreatePartnerEndpoint.assertIsSatisfied();
    }
}
