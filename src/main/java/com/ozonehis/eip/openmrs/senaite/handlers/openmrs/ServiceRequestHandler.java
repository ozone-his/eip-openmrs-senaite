package com.ozonehis.eip.openmrs.senaite.handlers.openmrs;

import ca.uhn.fhir.context.FhirContext;
import java.util.Map;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.ProducerTemplate;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.springframework.stereotype.Component;

@Slf4j
@Setter
@Component
public class ServiceRequestHandler {

    public ServiceRequest getServiceRequest(ProducerTemplate producerTemplate, Map<String, Object> headers) {
        String response = producerTemplate.requestBodyAndHeaders(
                "direct:openmrs-get-service-request-route", null, headers, String.class);
        log.info("getServiceRequest response {}", response);
        FhirContext ctx = FhirContext.forR4();
        ServiceRequest serviceRequestResponse = ctx.newJsonParser().parseResource(ServiceRequest.class, response);

        log.info("getServiceRequest {}", serviceRequestResponse);
        return serviceRequestResponse;
    }
}
