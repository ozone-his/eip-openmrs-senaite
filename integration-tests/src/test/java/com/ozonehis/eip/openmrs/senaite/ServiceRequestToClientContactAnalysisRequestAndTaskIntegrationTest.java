/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openmrs.senaite;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.openmrs.eip.fhir.Constants.HEADER_FHIR_EVENT_TYPE;

import com.ozonehis.eip.openmrs.senaite.routes.analyses.GetAnalysesRoute;
import java.util.HashMap;
import java.util.Map;
import org.apache.camel.CamelContext;
import org.apache.camel.test.infra.core.annotations.RouteFixture;
import org.hl7.fhir.r4.model.Bundle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class ServiceRequestToClientContactAnalysisRequestAndTaskIntegrationTest extends BaseRouteIntegrationTest {

    private Bundle serviceRequestBundle;

    private static final String ENCOUNTER_PART_OF_UUID = "7c164b93-83fa-41a9-95fe-4630231a8ff1";

    private static final String PATIENT_UUID = "79355a93-3a4f-4490-98aa-278f922fa87c";

    @BeforeEach
    public void initializeData() {
        serviceRequestBundle = loadResource("fhir.bundle/service-request-bundle.json", new Bundle());
    }

    @RouteFixture
    public void createRouteBuilder(CamelContext context) throws Exception {
        context = getContextWithRouting(context);

        context.addRoutes(new GetAnalysesRoute(getSenaiteConfig()));
    }

    @Test
    @DisplayName("Should verify has sale order routes.")
    public void shouldVerifySaleOrderRoutes() {
        assertTrue(hasRoute(contextExtension.getContext(), "service-request-to-analysis-request-router"));
        assertTrue(hasRoute(contextExtension.getContext(), "senaite-get-analyses-route"));
    }

    @Test
    @DisplayName("Should create serviceRequest in Senaite give ServiceRequest Bundle.")
    public void shouldCreateAnalysisRequestInSenaiteGivenServiceRequest() {
        // Act
        Map<String, Object> headers = new HashMap<>();
        headers.put(HEADER_FHIR_EVENT_TYPE, "c");
        sendBodyAndHeaders("direct:service-request-to-analysis-request-processor", serviceRequestBundle, headers);

        // Verify

    }
}
