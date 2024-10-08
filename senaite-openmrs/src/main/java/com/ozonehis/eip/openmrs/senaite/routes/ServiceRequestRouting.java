/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openmrs.senaite.routes;

import com.ozonehis.eip.openmrs.senaite.Constants;
import com.ozonehis.eip.openmrs.senaite.converters.ResourceConverter;
import com.ozonehis.eip.openmrs.senaite.processors.ServiceRequestProcessor;
import lombok.Setter;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Setter
@Component
public class ServiceRequestRouting extends RouteBuilder {

    private static final String SERVICE_REQUEST_ID = "service.request.id";

    private static final String SERVICE_REQUEST_INCLUDE_PARAMS = "ServiceRequest:encounter,ServiceRequest:patient";

    private static final String SEARCH_PARAMS =
            "id=${exchangeProperty." + SERVICE_REQUEST_ID + "}&resource=${exchangeProperty."
                    + Constants.FHIR_RESOURCE_TYPE + "}&include=" + SERVICE_REQUEST_INCLUDE_PARAMS;

    @Autowired
    private ServiceRequestProcessor serviceRequestProcessor;

    @Autowired
    private ResourceConverter resourceConverter;

    @Override
    public void configure() {
        getContext().getTypeConverterRegistry().addTypeConverters(resourceConverter);
        // spotless:off
        from("direct:fhir-servicerequest")
                .routeId("service-request-to-analysis-request-router")
                .filter(body().isNotNull())
                .filter(exchange -> exchange.getMessage().getBody() instanceof ServiceRequest)
                .process(exchange -> {
                    ServiceRequest serviceRequest = exchange.getMessage().getBody(ServiceRequest.class);
                    exchange.setProperty(Constants.FHIR_RESOURCE_TYPE, serviceRequest.fhirType());
                    exchange.setProperty(
                            SERVICE_REQUEST_ID, serviceRequest.getIdElement().getIdPart());
                    exchange.getMessage().setBody(serviceRequest);
                })
                .toD("openmrs-fhir://?" + SEARCH_PARAMS)
                .to("direct:service-request-to-analysis-request-processor")
                .end();

        from("direct:service-request-to-analysis-request-processor")
                .routeId("service-request-to-analysis-request-processor")
                .process(serviceRequestProcessor)
                .log(LoggingLevel.INFO, "Processed ServiceRequest")
                .end();
        // spotless:on
    }
}
