/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openmrs.senaite.routes.openmrsFhirTask;

import com.ozonehis.eip.openmrs.senaite.Constants;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.hl7.fhir.r4.model.Bundle;
import org.springframework.stereotype.Component;

@Component
public class GetOpenmrsFhirTaskRoute extends RouteBuilder {

    public static final String GET_ENDPOINT = "/Task?based-on:ServiceRequest=";

    @Override
    public void configure() {
        // spotless:off
        from("direct:openmrs-get-task-route")
            .log(LoggingLevel.INFO, "Fetching Task in OpenMRS...")
            .routeId("openmrs-get-task-route")
            .toD("fhir://search/searchByUrl?url=" + GET_ENDPOINT + "${header."
                        + Constants.HEADER_SERVICE_REQUEST_ID + "}")
            .marshal()
            .fhirJson("R4")
            .convertBodyTo(Bundle.class)
                .end();
        // spotless:on
    }
}
