/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openmrs.senaite.routes.openmrsFhirEncounter;

import com.ozonehis.eip.openmrs.senaite.Constants;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.hl7.fhir.r4.model.Encounter;
import org.springframework.stereotype.Component;

@Component
public class GetOpenmrsFhirEncounterByIdRoute extends RouteBuilder {

    @Override
    public void configure() {
        // spotless:off
        from("direct:openmrs-get-encounter-by-id-route")
            .log(LoggingLevel.INFO, "Fetching Encounter by id in OpenMRS...")
            .routeId("openmrs-get-encounter-by-id-route")
            .toD("fhir:read/resourceById?resourceClass=Encounter&stringId=" + "${header." + Constants.HEADER_ENCOUNTER_ID + "}")
            .marshal()
            .fhirJson("R4")
            .convertBodyTo(Encounter.class)
                .end();
        // spotless:on
    }
}
