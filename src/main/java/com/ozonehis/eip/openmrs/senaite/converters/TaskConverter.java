package com.ozonehis.eip.openmrs.senaite.converters;

import ca.uhn.fhir.context.FhirContext;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Converter;
import org.hl7.fhir.r4.model.Task;
import org.springframework.stereotype.Component;

@Slf4j
@Converter
@Component
public class TaskConverter {

    @Converter
    public static InputStream convertTaskToInputStream(Task task) {
        FhirContext ctx = FhirContext.forR4();
        String json = ctx.newJsonParser().encodeResourceToString(task);
        log.info("Task to JSON {}", json);
        return new ByteArrayInputStream(json.getBytes());
    }
}
