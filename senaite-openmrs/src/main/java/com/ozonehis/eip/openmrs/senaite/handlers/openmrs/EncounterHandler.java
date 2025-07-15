/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openmrs.senaite.handlers.openmrs;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import java.util.Collections;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Encounter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Component
public class EncounterHandler {

    @Value("${results.encounterType.uuid}")
    private String resultEncounterTypeUUID;

    private IGenericClient openmrsFhirClient;

    @Autowired
    public EncounterHandler(IGenericClient openmrsFhirClient) {
        this.openmrsFhirClient = openmrsFhirClient;
    }

    public Encounter getEncounterByTypeAndSubject(String typeID, String subjectID) {
        Bundle bundle = openmrsFhirClient
                .search()
                .forResource(Encounter.class)
                .where(Encounter.TYPE.exactly().code(typeID))
                .and(Encounter.SUBJECT.hasId(subjectID))
                .returnBundle(Bundle.class)
                .count(1) // Limit to 1 result
                .execute();

        log.debug("EncounterHandler: Encounter getEncounterByTypeAndSubject {}", bundle.getId());

        return bundle.getEntry().stream()
                .map(Bundle.BundleEntryComponent::getResource)
                .filter(Encounter.class::isInstance)
                .map(Encounter.class::cast)
                .findFirst()
                .orElse(null);
    }

    public Encounter getEncounterByEncounterID(String encounterID) {
        Encounter encounter = openmrsFhirClient
                .read()
                .resource(Encounter.class)
                .withId(encounterID)
                .execute();

        log.debug("EncounterHandler: Encounter getEncounterByEncounterID {}", encounter.getId());
        return encounter;
    }

    public Encounter sendEncounter(Encounter encounter) {
        MethodOutcome methodOutcome =
                openmrsFhirClient.create().resource(encounter).encodedJson().execute();

        log.debug("EncounterHandler: Encounter created {}", methodOutcome.getCreated());
        return (Encounter) methodOutcome.getResource();
    }

    public Encounter buildLabResultEncounter(Encounter orderEncounter) {
        Encounter resultEncounter = new Encounter();
        resultEncounter.setLocation(orderEncounter.getLocation());
        Coding coding = new Coding();
        coding.setCode(resultEncounterTypeUUID);
        coding.setSystem("http://fhir.openmrs.org/code-system/encounter-type");
        coding.setDisplay("Lab Results");
        resultEncounter.setType(
                (Collections.singletonList(new CodeableConcept().setCoding(Collections.singletonList(coding)))));
        if (orderEncounter.hasPeriod()) {
            resultEncounter.setPeriod(orderEncounter.getPeriod());
        }
        resultEncounter.setSubject(orderEncounter.getSubject());
        resultEncounter.setPartOf(orderEncounter.getPartOf());
        resultEncounter.setParticipant(orderEncounter.getParticipant());
        return resultEncounter;
    }
}
