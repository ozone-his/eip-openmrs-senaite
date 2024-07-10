package com.ozonehis.eip.openmrs.senaite.routes.openmrsFhirTask;

import com.ozonehis.eip.openmrs.senaite.Constants;
import com.ozonehis.eip.openmrs.senaite.client.OpenmrsFhirClient;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class UpdateOpenmrsFhirTaskRoute extends RouteBuilder {

    @Autowired
    private OpenmrsFhirClient openmrsFhirClient;

    public static final String UPDATE_ENDPOINT = "/Task/";

    @Override
    public void configure() {
        // spotless:off
        from("direct:openmrs-update-task-route")
                .log(LoggingLevel.INFO, "Updating Task in OpenMRS...")
                .routeId("openmrs-update-task-route")
                .setHeader(Constants.CAMEL_HTTP_METHOD, constant(Constants.PUT))
                .setHeader(Constants.CONTENT_TYPE, constant(Constants.APPLICATION_JSON))
                .setHeader(Constants.AUTHORIZATION, constant(openmrsFhirClient.authHeader()))
                .toD(openmrsFhirClient.getOpenmrsFhirBaseUrl() + UPDATE_ENDPOINT + "${header."
                        + Constants.HEADER_TASK_ID + "}")
                .log(
                        LoggingLevel.INFO,
                        "Response update-task-route: ${body} task_id ${header." + Constants.HEADER_TASK_ID + "}")
                .end();
        // spotless:on
    }
}
