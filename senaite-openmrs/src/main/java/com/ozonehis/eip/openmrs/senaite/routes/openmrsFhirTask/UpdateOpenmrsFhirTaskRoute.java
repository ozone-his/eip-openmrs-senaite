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
import org.hl7.fhir.r4.model.Task;
import org.springframework.stereotype.Component;

@Component
public class UpdateOpenmrsFhirTaskRoute extends RouteBuilder {

    @Override
    public void configure() {
        // spotless:off
        from("direct:openmrs-update-task-route")
            .log(LoggingLevel.INFO, "Updating Task in OpenMRS...")
            .routeId("openmrs-update-task-route")
            .marshal()
            .fhirJson("R4")
            .convertBodyTo(String.class)
            .toD("fhir://update/resource?inBody=resourceAsString&stringId=" + "${header."
                        + Constants.HEADER_TASK_ID + "}")
            .marshal()
            .fhirJson("R4")
            .convertBodyTo(Task.class)
                .end();
        // spotless:on
    }
}
