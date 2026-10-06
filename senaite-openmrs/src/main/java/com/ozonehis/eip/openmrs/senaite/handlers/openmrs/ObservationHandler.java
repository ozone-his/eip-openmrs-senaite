/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openmrs.senaite.handlers.openmrs;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.api.SummaryEnum;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Setter
@AllArgsConstructor
@Component
public class ObservationHandler {

    @Autowired
    private IGenericClient openmrsFhirClient;

    public Observation getObservationByCodeSubjectEncounterAndDate(
            String codeID, String subjectID, String encounterID, String observationDate) {
        Bundle bundle = openmrsFhirClient
                .search()
                .forResource(Observation.class)
                .where(Observation.CODE.exactly().code(codeID))
                .and(Observation.SUBJECT.hasId(subjectID))
                .and(Observation.ENCOUNTER.hasId(encounterID))
                .summaryMode(SummaryEnum.DATA)
                .returnBundle(Bundle.class)
                .execute();

        log.debug("ObservationHandler: Observation getObservationByCodeSubjectEncounterAndDate {}", bundle.getId());

        return bundle.getEntry().stream()
                .map(Bundle.BundleEntryComponent::getResource)
                .filter(Observation.class::isInstance)
                .map(Observation.class::cast)
                .filter(observation -> matchesObservationDate(observation, observationDate))
                .findFirst()
                .orElse(null);
    }

    public Observation getObservationByCodeSubjectEncounterDateAndValue(
            String codeID, String subjectID, String encounterID, String observationDate, String observationValue) {
        List<Observation> observations = openmrsFhirClient
                .search()
                .forResource(Observation.class)
                .where(Observation.CODE.exactly().code(codeID))
                .and(Observation.SUBJECT.hasId(subjectID))
                .and(Observation.ENCOUNTER.hasId(encounterID))
                .summaryMode(SummaryEnum.DATA)
                .returnBundle(Bundle.class)
                .execute()
                .getEntry()
                .stream()
                .map(Bundle.BundleEntryComponent::getResource)
                .filter(Observation.class::isInstance)
                .map(Observation.class::cast)
                .toList();

        return observations.stream()
                .filter(observation -> matchesObservationDate(observation, observationDate))
                .filter(observation -> matchesObservationValue(observation, observationValue))
                .findFirst()
                .orElse(null);
    }

    public Observation sendObservation(Observation observation) {
        MethodOutcome methodOutcome =
                openmrsFhirClient.create().resource(observation).encodedJson().execute();

        log.debug("ObservationHandler: Observation created {}", methodOutcome.getCreated());
        return (Observation) methodOutcome.getResource();
    }

    public Observation buildResultObservation(
            Encounter savedResultEncounter,
            String conceptUuid,
            String analysesResult,
            String analysesResultCaptureDate,
            String remarks) {
        Observation observation = new Observation();
        observation.setStatus(Observation.ObservationStatus.FINAL);
        observation.setCode(new CodeableConcept(new Coding().setCode(conceptUuid)));
        observation.setSubject(savedResultEncounter.getSubject());
        observation.setEffective(new DateTimeType().setValue(Date.from(Instant.parse(analysesResultCaptureDate))));
        observation.setValue(getObservationValueBySenaiteResult(analysesResult));
        observation.setEncounter(new Reference("Encounter/" + savedResultEncounter.getIdPart()));
        if (remarks != null && !remarks.isBlank()) {
            observation.addNote().setText(remarks); // FHIR2 module needs to add support for Observation.note
        }
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
        return observation != null && observation.hasId();
    }

    private boolean matchesObservationDate(Observation observation, String observationDate) {
        if (observationDate == null || !observation.hasEffective()) {
            return true;
        }
        if (observation.getEffective() instanceof DateTimeType) {
            DateTimeType effectiveDate = (DateTimeType) observation.getEffective();
            try {
                return Instant.parse(observationDate)
                        .equals(effectiveDate.getValue().toInstant());
            } catch (Exception e) {
                return observationDate.equalsIgnoreCase(effectiveDate.asStringValue());
            }
        }
        return false;
    }

    private boolean matchesObservationValue(Observation observation, String observationValue) {
        if (observationValue == null || !observation.hasValue()) {
            return true;
        }
        String normalizedObservationValue = normalizeValueToComparableString(observation.getValue());
        return normalizeValueToComparableString(observationValue).equals(normalizedObservationValue);
    }

    private String normalizeValueToComparableString(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof Quantity) {
            Quantity quantity = (Quantity) value;
            if (quantity.getValue() == null) {
                return "";
            }
            return quantity.getValue().stripTrailingZeros().toPlainString();
        }
        if (value instanceof Type) {
            Type type = (Type) value;
            String primitiveValue = type.primitiveValue();
            if (primitiveValue != null && !primitiveValue.isBlank()) {
                try {
                    return new java.math.BigDecimal(primitiveValue)
                            .stripTrailingZeros()
                            .toPlainString();
                } catch (NumberFormatException ignored) {
                    return primitiveValue.trim();
                }
            }
        }
        return String.valueOf(value).trim();
    }
}
