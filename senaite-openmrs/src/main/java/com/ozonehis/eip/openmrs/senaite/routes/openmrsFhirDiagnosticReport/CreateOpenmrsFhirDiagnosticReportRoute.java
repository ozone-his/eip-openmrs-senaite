/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openmrs.senaite.routes.openmrsFhirDiagnosticReport;

import com.ozonehis.eip.openmrs.senaite.Constants;
import com.ozonehis.eip.openmrs.senaite.client.OpenmrsFhirClient;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CreateOpenmrsFhirDiagnosticReportRoute extends RouteBuilder {

    @Autowired
    private OpenmrsFhirClient openmrsFhirClient;

    public static final String CREATE_ENDPOINT = "/DiagnosticReport";

    @Override
    public void configure() {
        // spotless:off
        from("direct:openmrs-create-diagnostic-report-route")
                .log(LoggingLevel.INFO, "Creating DiagnosticReport in OpenMRS...")
                .routeId("openmrs-create-diagnostic-report-route")
                .setHeader(Constants.CAMEL_HTTP_METHOD, constant(Constants.POST))
                .setHeader(Constants.CONTENT_TYPE, constant(Constants.APPLICATION_JSON))
                .setHeader(Constants.AUTHORIZATION, constant(openmrsFhirClient.authHeader()))
                .toD(openmrsFhirClient.getOpenmrsFhirBaseUrl() + CREATE_ENDPOINT)
                .end();
        // spotless:on
    }
}
