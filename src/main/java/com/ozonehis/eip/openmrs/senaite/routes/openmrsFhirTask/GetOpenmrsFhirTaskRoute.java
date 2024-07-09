package com.ozonehis.eip.openmrs.senaite.routes.openmrsFhirTask;

import com.ozonehis.eip.openmrs.senaite.Constants;
import com.ozonehis.eip.openmrs.senaite.client.OpenmrsFhirClient;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class GetOpenmrsFhirTaskRoute extends RouteBuilder {

    @Autowired
    private OpenmrsFhirClient openmrsFhirClient;

    public static final String GET_ENDPOINT = "/Task?based-on:ServiceRequest=";

    @Override
    public void configure() {
        // spotless:off
        from("direct:openmrs-get-task-route")
                .log(LoggingLevel.INFO, "Fetching Task in OpenMRS...")
                .routeId("openmrs-get-task-route")
                .setHeader(Constants.CAMEL_HTTP_METHOD, constant(Constants.GET))
                .setHeader(Constants.CONTENT_TYPE, constant(Constants.APPLICATION_JSON))
                .setHeader(Constants.AUTHORIZATION, constant(openmrsFhirClient.authHeader()))
                .toD(openmrsFhirClient.getOpenmrsFhirBaseUrl() + GET_ENDPOINT + "${header."
                        + Constants.HEADER_SERVICE_REQUEST_ID + "}")
                .log(
                        LoggingLevel.INFO,
                        "Response get-task-route: ${body} service_request_id ${header."
                                + Constants.HEADER_SERVICE_REQUEST_ID + "}")
                .end();
        // spotless:on
    }
}
