package com.ozonehis.eip.openmrs.senaite.converters;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.IOException;
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
    public static InputStream convertTaskToInputStream(Task task) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        String json = objectMapper.writeValueAsString(task);
        return new ByteArrayInputStream(json.getBytes());
    }
}
