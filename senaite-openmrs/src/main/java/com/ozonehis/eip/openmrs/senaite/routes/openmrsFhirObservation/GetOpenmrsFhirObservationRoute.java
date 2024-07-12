/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openmrs.senaite.routes.openmrsFhirObservation;

import com.ozonehis.eip.openmrs.senaite.Constants;
import com.ozonehis.eip.openmrs.senaite.client.OpenmrsFhirClient;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class GetOpenmrsFhirObservationRoute extends RouteBuilder {

    @Autowired
    private OpenmrsFhirClient openmrsFhirClient;

    public static final String GET_ENDPOINT = "/Observation?code=%s&subject=%s&encounter=%s&date=%s";

    @Override
    public void configure() {
        // spotless:off
        from("direct:openmrs-get-observation-route")
                .log(LoggingLevel.INFO, "Fetching Observation in OpenMRS...")
                .routeId("openmrs-get-observation-route")
                .setHeader(Constants.CAMEL_HTTP_METHOD, constant(Constants.GET))
                .setHeader(Constants.CONTENT_TYPE, constant(Constants.APPLICATION_JSON))
                .setHeader(Constants.AUTHORIZATION, constant(openmrsFhirClient.authHeader()))
                .toD(openmrsFhirClient.getOpenmrsFhirBaseUrl()
                        + String.format(
                                GET_ENDPOINT,
                                "${header." + Constants.HEADER_OBSERVATION_CODE + "}",
                                "${header." + Constants.HEADER_OBSERVATION_SUBJECT + "}",
                                "${header." + Constants.HEADER_OBSERVATION_ENCOUNTER + "}",
                                "${header." + Constants.HEADER_OBSERVATION_DATE + "}"))
                .log(
                        LoggingLevel.INFO,
                        "Response get-observation-route: ${body} code, subject, encounter, date" + "${header."
                                + Constants.HEADER_OBSERVATION_CODE + "}" + "${header."
                                + Constants.HEADER_OBSERVATION_SUBJECT + "}" + "${header."
                                + Constants.HEADER_OBSERVATION_ENCOUNTER + "}" + "${header."
                                + Constants.HEADER_OBSERVATION_DATE + "}")
                .end();
        // spotless:on
    }
}
