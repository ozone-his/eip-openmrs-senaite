package com.ozonehis.eip.openmrs.senaite.handlers.openmrs;

import ca.uhn.fhir.context.FhirContext;
import java.util.List;
import java.util.Map;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.ProducerTemplate;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Resource;
import org.springframework.stereotype.Component;

@Slf4j
@Setter
@Component
public class EncounterHandler {

    public Encounter getEncounter(ProducerTemplate producerTemplate, Map<String, Object> headers) {
        String response = producerTemplate.requestBodyAndHeaders(
                "direct:openmrs-get-encounter-route", null, headers, String.class);
        log.info("getEncounter response {}", response);
        FhirContext ctx = FhirContext.forR4();
        Bundle bundle = ctx.newJsonParser().parseResource(Bundle.class, response);
        List<Bundle.BundleEntryComponent> entries = bundle.getEntry();

        Encounter encounter = null;
        for (Bundle.BundleEntryComponent entry : entries) {
            Resource resource = entry.getResource();
            if (resource instanceof Encounter) {
                encounter = (Encounter) resource;
            }
        }
        log.info("getEncounter {}", encounter);
        return encounter;
    }

    public Encounter sendEncounter(ProducerTemplate producerTemplate, Encounter encounter) {
        log.info(
                "sendEncounter response {}", FhirContext.forR4().newJsonParser().encodeResourceToString(encounter));
        String response =
                producerTemplate.requestBody("direct:openmrs-create-encounter-route", encounter, String.class);
        log.info("sendEncounter response {}", response);
        FhirContext ctx = FhirContext.forR4();
        Encounter savedEncounter = ctx.newJsonParser().parseResource(Encounter.class, response);
        log.info("sendEncounter {}", savedEncounter);
        return savedEncounter;
    }
}
