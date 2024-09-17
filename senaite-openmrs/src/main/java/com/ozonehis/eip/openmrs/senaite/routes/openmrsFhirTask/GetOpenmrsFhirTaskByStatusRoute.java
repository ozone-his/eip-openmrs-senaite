/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openmrs.senaite.routes.openmrsFhirTask;

import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.hl7.fhir.r4.model.Bundle;
import org.springframework.stereotype.Component;

@Component
public class GetOpenmrsFhirTaskByStatusRoute extends RouteBuilder {

    public static final String GET_BY_STATUS_ENDPOINT = "/Task?status=requested,accepted";

    @Override
    public void configure() {
        // spotless:off
        from("direct:openmrs-get-task-by-status-route")
                .log(LoggingLevel.INFO, "Fetching Task by Status in OpenMRS...")
                .routeId("openmrs-get-task-by-status-route")
                .toD("fhir://search/searchByUrl?url=" + GET_BY_STATUS_ENDPOINT)
                .unmarshal()
                .fhirJson("R4")
                .convertBodyTo(Bundle.class)
                .end();
        // spotless:on
    }
}
