/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openmrs.senaite.handlers.openmrs;

import ca.uhn.fhir.context.FhirContext;
import java.util.List;
import java.util.Map;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.ProducerTemplate;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Resource;
import org.springframework.stereotype.Component;

@Slf4j
@Setter
@Component
public class ObservationHandler {

    public Observation getObservation(ProducerTemplate producerTemplate, Map<String, Object> headers) {
        String response = producerTemplate.requestBodyAndHeaders(
                "direct:openmrs-get-observation-route", null, headers, String.class);
        log.info("getObservation response {}", response);
        FhirContext ctx = FhirContext.forR4();
        Bundle bundle = ctx.newJsonParser().parseResource(Bundle.class, response);
        List<Bundle.BundleEntryComponent> entries = bundle.getEntry();

        Observation observation = null;
        for (Bundle.BundleEntryComponent entry : entries) {
            Resource resource = entry.getResource();
            if (resource instanceof Observation) {
                observation = (Observation) resource;
            }
        }
        log.info("getObservation {}", observation);
        return observation;
    }

    public Observation sendObservation(ProducerTemplate producerTemplate, Observation observation) {
        String response =
                producerTemplate.requestBody("direct:openmrs-create-observation-route", observation, String.class);
        log.info("sendObservation response {}", response);
        FhirContext ctx = FhirContext.forR4();
        Observation savedObservation = ctx.newJsonParser().parseResource(Observation.class, response);
        log.info("sendObservation {}", savedObservation);
        return savedObservation;
    }
}
