package com.ozonehis.eip.openmrs.senaite.converters;

import ca.uhn.fhir.context.FhirContext;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Converter;
import org.hl7.fhir.r4.model.Encounter;
import org.springframework.stereotype.Component;

@Slf4j
@Converter
@Component
public class EncounterConverter {

    @Converter
    public static InputStream convertEncounterToInputStream(Encounter encounter) {
        FhirContext ctx = FhirContext.forR4();
        String json = ctx.newJsonParser().encodeResourceToString(encounter);
        return new ByteArrayInputStream(json.getBytes());
    }
}
