/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openmrs.senaite.routes;

import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

@Component
public class CreateOpenmrsFhirResourceRoute extends RouteBuilder {

    @Override
    public void configure() {
        // spotless:off
        from("direct:openmrs-create-resource-route")
                .log(LoggingLevel.INFO, "Creating Resource in OpenMRS...")
                .routeId("openmrs-create-resource-route")
                .marshal().fhirJson("R4")
                .convertBodyTo(String.class)
                .to("fhir://create/resource?inBody=resourceAsString")
                .marshal().fhirJson("R4")
                .convertBodyTo(String.class)
                .end();
        // spotless:on
    }
}
