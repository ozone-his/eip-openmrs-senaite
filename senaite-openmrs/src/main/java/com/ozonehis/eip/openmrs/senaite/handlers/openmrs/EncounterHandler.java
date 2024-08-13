/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openmrs.senaite.handlers.openmrs;

import com.ozonehis.eip.openmrs.senaite.Constants;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.ProducerTemplate;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Resource;
import org.springframework.stereotype.Component;

@Slf4j
@Setter
@Component
public class EncounterHandler {

    public Encounter getEncounterByTypeAndSubject(ProducerTemplate producerTemplate, String typeID, String subjectID) {
        String url = String.format("Encounter?type=%s&subject=%s", typeID, subjectID);
        Map<String, Object> headers = new HashMap<>();
        headers.put(Constants.CUSTOM_URL, url);
        Bundle bundle = producerTemplate.requestBodyAndHeaders(
                "direct:openmrs-get-encounter-route", null, headers, Bundle.class);
        List<Bundle.BundleEntryComponent> entries = bundle.getEntry();

        Encounter encounter = null;
        for (Bundle.BundleEntryComponent entry : entries) {
            Resource resource = entry.getResource();
            if (resource instanceof Encounter) {
                encounter = (Encounter) resource;
            }
        }
        return encounter;
    }

    public Encounter getEncounterByEncounterID(ProducerTemplate producerTemplate, String encounterID) {
        Map<String, Object> headers = new HashMap<>();
        headers.put(Constants.HEADER_ENCOUNTER_ID, encounterID);
        return producerTemplate.requestBodyAndHeaders(
                "direct:openmrs-get-encounter-by-id-route", null, headers, Encounter.class);
    }

    public Encounter sendEncounter(ProducerTemplate producerTemplate, Encounter encounter) {
        return producerTemplate.requestBody("direct:openmrs-create-resource-route", encounter, Encounter.class);
    }

    public Encounter buildLabResultEncounter(Encounter orderEncounter) {
        Encounter resultEncounter = new Encounter();
        resultEncounter.setLocation(orderEncounter.getLocation());
        Coding coding = new Coding();
        coding.setCode("3596fafb-6f6f-4396-8c87-6e63a0f1bd71"); // TODO: Fetch typeID from config
        coding.setSystem("http://fhir.openmrs.org/code-system/encounter-type");
        coding.setDisplay("Lab Results");
        resultEncounter.setType(
                (Collections.singletonList(new CodeableConcept().setCoding(Collections.singletonList(coding)))));
        resultEncounter.setPeriod(orderEncounter.getPeriod());
        resultEncounter.setSubject(orderEncounter.getSubject());
        resultEncounter.setPartOf(orderEncounter.getPartOf());
        resultEncounter.setParticipant(orderEncounter.getParticipant());
        return resultEncounter;
    }
}
