package com.ozonehis.eip.openmrs.senaite.handlers.senaite;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ozonehis.eip.openmrs.senaite.model.AnalysisRequest;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.ProducerTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Setter
@Component
public class AnalysisRequestHandler {

    public AnalysisRequest sendAnalysisRequest(ProducerTemplate producerTemplate, AnalysisRequest analysisRequest)
            throws JsonProcessingException {
        String response = producerTemplate.requestBody(
                "direct:senaite-create-analysis-request-route", analysisRequest, String.class);
        log.error("sendAnalysisRequest response {}", response);
        ObjectMapper objectMapper = new ObjectMapper();
        AnalysisRequest savedAnalysisRequest = objectMapper.readValue(response, AnalysisRequest.class);
        log.error("sendAnalysisRequest {}", response);
        return savedAnalysisRequest;
    }

    public AnalysisRequest getAnalysisRequest(ProducerTemplate producerTemplate, String queryParams)
            throws JsonProcessingException {
        String response = producerTemplate.requestBody("direct:senaite-get-analysis-request-route", null, String.class);
        log.error("getAnalysisRequest response {}", response);
        ObjectMapper objectMapper = new ObjectMapper();
        AnalysisRequest analysisRequest = objectMapper.readValue(response, AnalysisRequest.class);
        log.error("getAnalysisRequest {}", analysisRequest);
        return analysisRequest;
    }
}
