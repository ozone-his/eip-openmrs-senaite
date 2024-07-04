package com.ozonehis.eip.openmrs.senaite.converters;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ozonehis.eip.openmrs.senaite.model.AnalysisRequest;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Converter;

@Slf4j
@Converter
public class AnalysisRequestConverter {

    @Converter
    public static InputStream convertAnalysisRequestToInputStream(AnalysisRequest analysisRequest) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        String json = objectMapper.writeValueAsString(analysisRequest);
        return new ByteArrayInputStream(json.getBytes());
    }
}
