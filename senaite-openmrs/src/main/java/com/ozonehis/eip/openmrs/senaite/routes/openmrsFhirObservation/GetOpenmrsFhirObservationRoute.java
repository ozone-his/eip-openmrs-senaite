/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openmrs.senaite.routes.openmrsFhirObservation;

import com.ozonehis.eip.openmrs.senaite.Constants;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

@Component
public class GetOpenmrsFhirObservationRoute extends RouteBuilder {

    @Override
    public void configure() {
        // spotless:off
        from("direct:openmrs-get-observation-route")
                .log(LoggingLevel.INFO, "Fetching Observation in OpenMRS...")
                .routeId("openmrs-get-observation-route")
                .setHeader("CamelFhir.url", header(Constants.CUSTOM_URL))
                .toD("fhir://search/searchByUrl")
                .marshal()
                .fhirJson("R4")
                .convertBodyTo(String.class)
                .end();
        // spotless:on
    }
}
