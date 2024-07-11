package com.ozonehis.eip.openmrs.senaite.routes.openmrsFhirObservation;

import com.ozonehis.eip.openmrs.senaite.Constants;
import com.ozonehis.eip.openmrs.senaite.client.OpenmrsFhirClient;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CreateOpenmrsFhirObservationRoute extends RouteBuilder {

    @Autowired
    private OpenmrsFhirClient openmrsFhirClient;

    public static final String CREATE_ENDPOINT = "/Observation";

    @Override
    public void configure() {
        // spotless:off
        from("direct:openmrs-create-observation-route")
                .log(LoggingLevel.INFO, "Creating Observation in OpenMRS...")
                .routeId("openmrs-create-observation-route")
                .setHeader(Constants.CAMEL_HTTP_METHOD, constant(Constants.POST))
                .setHeader(Constants.CONTENT_TYPE, constant(Constants.APPLICATION_JSON))
                .setHeader(Constants.AUTHORIZATION, constant(openmrsFhirClient.authHeader()))
                .toD(openmrsFhirClient.getOpenmrsFhirBaseUrl() + CREATE_ENDPOINT)
                .log(LoggingLevel.INFO, "Response create-observation-route: ${body}")
                .end();
        // spotless:on
    }
}
