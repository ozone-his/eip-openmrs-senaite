package com.ozonehis.eip.openmrs.senaite.converters;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ozonehis.eip.openmrs.senaite.model.analysisRequest.AnalysisRequest;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Converter;
import org.springframework.stereotype.Component;

@Slf4j
@Converter
@Component
public class AnalysisRequestConverter {

    @Converter
    public static InputStream convertAnalysisRequestToInputStream(AnalysisRequest analysisRequest) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        String json = objectMapper.writeValueAsString(analysisRequest);
        return new ByteArrayInputStream(json.getBytes());
    }
}
