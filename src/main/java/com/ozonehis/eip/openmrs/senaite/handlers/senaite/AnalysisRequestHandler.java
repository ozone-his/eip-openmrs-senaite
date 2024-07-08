package com.ozonehis.eip.openmrs.senaite.handlers.senaite;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ozonehis.eip.openmrs.senaite.model.analysisRequest.AnalysisRequest;
import com.ozonehis.eip.openmrs.senaite.model.analysisRequest.AnalysisRequestResponse;
import java.util.Map;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.ProducerTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Setter
@Component
public class AnalysisRequestHandler {

    public AnalysisRequest sendAnalysisRequest(
            ProducerTemplate producerTemplate, AnalysisRequest analysisRequest, Map<String, Object> headers)
            throws JsonProcessingException {
        String response = producerTemplate.requestBodyAndHeaders(
                "direct:senaite-create-analysis-request-route", analysisRequest, headers, String.class);
        log.error("sendAnalysisRequest response {}", response);
        ObjectMapper objectMapper = new ObjectMapper();
        AnalysisRequestResponse savedAnalysisRequestResponse =
                objectMapper.readValue(response, AnalysisRequestResponse.class);
        log.error("sendAnalysisRequest {}", savedAnalysisRequestResponse);
        return savedAnalysisRequestResponse.analysisRequestResponseToAnalysisRequest(savedAnalysisRequestResponse);
    }

    public AnalysisRequest getAnalysisRequest(ProducerTemplate producerTemplate, Map<String, Object> headers)
            throws JsonProcessingException {
        String response = producerTemplate.requestBodyAndHeaders(
                "direct:senaite-get-analysis-request-route", null, headers, String.class);
        log.error("getAnalysisRequest response {}", response);
        ObjectMapper objectMapper = new ObjectMapper();
        AnalysisRequestResponse analysisRequestResponse =
                objectMapper.readValue(response, AnalysisRequestResponse.class);
        log.error("getAnalysisRequest {}", analysisRequestResponse);
        return analysisRequestResponse.analysisRequestResponseToAnalysisRequest(analysisRequestResponse);
    }
}
