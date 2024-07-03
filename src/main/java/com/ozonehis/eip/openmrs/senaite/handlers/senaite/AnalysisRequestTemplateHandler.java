package com.ozonehis.eip.openmrs.senaite.handlers.senaite;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ozonehis.eip.openmrs.senaite.model.AnalysisRequestTemplate;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.ProducerTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Setter
@Component
public class AnalysisRequestTemplateHandler {

    public AnalysisRequestTemplate sendAnalysisRequestTemplate(
            ProducerTemplate producerTemplate, AnalysisRequestTemplate analysisRequest) throws JsonProcessingException {
        String response = producerTemplate.requestBody(
                "direct:senaite-create-analysis-request-template-route", analysisRequest, String.class);
        log.error("sendAnalysisRequestTemplate response {}", response);
        ObjectMapper objectMapper = new ObjectMapper();
        AnalysisRequestTemplate savedAnalysisRequestTemplate =
                objectMapper.readValue(response, AnalysisRequestTemplate.class);
        log.error("sendAnalysisRequestTemplate {}", response);
        return savedAnalysisRequestTemplate;
    }

    public AnalysisRequestTemplate getAnalysisRequestTemplate(ProducerTemplate producerTemplate, String queryParams)
            throws JsonProcessingException {
        String response =
                producerTemplate.requestBody("direct:senaite-get-analysis-request-template-route", null, String.class);
        log.error("getAnalysisRequestTemplate response {}", response);
        ObjectMapper objectMapper = new ObjectMapper();
        AnalysisRequestTemplate analysisRequest = objectMapper.readValue(response, AnalysisRequestTemplate.class);
        log.error("getAnalysisRequestTemplate {}", analysisRequest);
        return analysisRequest;
    }
}
