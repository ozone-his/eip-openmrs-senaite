package com.ozonehis.eip.openmrs.senaite.converters;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ozonehis.eip.openmrs.senaite.model.analyses.Analyses;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Converter;
import org.springframework.stereotype.Component;

@Slf4j
@Converter
@Component
public class AnalysesConverter {

    @Converter
    public static InputStream convertClientToInputStream(Analyses analyses) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        String json = objectMapper.writeValueAsString(analyses);
        return new ByteArrayInputStream(json.getBytes());
    }
}
