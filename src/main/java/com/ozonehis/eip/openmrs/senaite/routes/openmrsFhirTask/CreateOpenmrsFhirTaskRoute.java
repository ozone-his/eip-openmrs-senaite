package com.ozonehis.eip.openmrs.senaite.routes.openmrsFhirTask;

import com.ozonehis.eip.openmrs.senaite.Constants;
import com.ozonehis.eip.openmrs.senaite.client.OpenmrsFhirClient;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CreateOpenmrsFhirTaskRoute extends RouteBuilder {

    @Autowired
    private OpenmrsFhirClient openmrsFhirClient;

    public static final String CREATE_ENDPOINT = "/Task";

    @Override
    public void configure() {
        // spotless:off
        from("direct:openmrs-create-task-route")
                .log(LoggingLevel.INFO, "Creating Task in OpenMRS...")
                .routeId("openmrs-create-task-route")
                .setHeader(Constants.CAMEL_HTTP_METHOD, constant(Constants.POST))
                .setHeader(Constants.CONTENT_TYPE, constant(Constants.APPLICATION_JSON))
                .setHeader(Constants.AUTHORIZATION, constant(openmrsFhirClient.authHeader()))
                .to(openmrsFhirClient.getOpenmrsFhirBaseUrl() + CREATE_ENDPOINT)
                .log("Response create-task-route: ${body}")
                .end();
        // spotless:on
    }
}
