/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openmrs.senaite.handlers.openmrs;

import com.ozonehis.eip.openmrs.senaite.Constants;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.ProducerTemplate;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Quantity;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.Type;
import org.springframework.stereotype.Component;

@Slf4j
@Setter
@Component
public class ObservationHandler {

    public Observation getObservationByCodeSubjectEncounterAndDate(
            ProducerTemplate producerTemplate,
            String codeID,
            String subjectID,
            String encounterID,
            String observationDate) {
        String url = String.format(
                "Observation?code=%s&subject=%s&encounter=%s&date=%s", codeID, subjectID, encounterID, observationDate);
        Map<String, Object> headers = new HashMap<>();
        headers.put(Constants.CUSTOM_URL, url);
        Bundle bundle = producerTemplate.requestBodyAndHeaders(
                "direct:openmrs-get-observation-route", null, headers, Bundle.class);
        return bundle.getEntry().stream()
                .map(Bundle.BundleEntryComponent::getResource)
                .filter(Observation.class::isInstance)
                .map(Observation.class::cast)
                .findFirst()
                .orElse(null);
    }

    public Observation sendObservation(ProducerTemplate producerTemplate, Observation observation) {
        return producerTemplate.requestBody("direct:openmrs-create-resource-route", observation, Observation.class);
    }

    public Observation buildResultObservation(
            Encounter savedResultEncounter,
            String conceptUuid,
            String analysesResult,
            String analysesResultCaptureDate) {
        Observation observation = new Observation();
        observation.setStatus(Observation.ObservationStatus.FINAL);
        observation.setCode(new CodeableConcept(new Coding().setCode(conceptUuid)));
        observation.setSubject(savedResultEncounter.getSubject());
        observation.setEffective(new DateTimeType().setValue(Date.from(Instant.parse(analysesResultCaptureDate))));
        observation.setValue(getObservationValueBySenaiteResult(analysesResult));
        observation.setEncounter(new Reference("Encounter/" + savedResultEncounter.getIdPart()));
        return observation;
    }

    private Type getObservationValueBySenaiteResult(String senaiteResult) {
        if (senaiteResult.matches("-?\\d+(\\.\\d+)?")) {
            // If result is a number
            return new Quantity().setValue(Double.parseDouble(senaiteResult));
        } else if (senaiteResult.matches(
                        "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")
                || senaiteResult.matches("^[A-F0-9]{36,38}$")) {
            // If result is a UUID
            return new CodeableConcept().setCoding(Collections.singletonList(new Coding().setCode(senaiteResult)));
        } else {
            // Handle ordinary string values
            return new StringType(senaiteResult);
        }
    }

    public boolean doesObservationExists(Observation observation) {
        return observation != null
                && observation.getId() != null
                && !observation.getId().isEmpty();
    }
}
