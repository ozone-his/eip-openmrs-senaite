package com.ozonehis.eip.openmrs.senaite.routes;

import com.ozonehis.eip.openmrs.senaite.Constants;
import com.ozonehis.eip.openmrs.senaite.client.SenaiteClient;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CreateContactRoute extends RouteBuilder {

    @Autowired
    private SenaiteClient senaiteClient;

    @Override
    public void configure() {
        // spotless:off
        from("direct:senaite-create-contact-route")
                .log(LoggingLevel.INFO, "Creating Contact in SENAITE...")
                .routeId("senaite-create-contact-route")
                .setHeader(Constants.CAMEL_HTTP_METHOD, constant(Constants.POST))
                .setHeader(Constants.CONTENT_TYPE, constant(Constants.APPLICATION_JSON))
                .setHeader(Constants.AUTHORIZATION, constant(senaiteClient.authHeader()))
                .to(senaiteClient.getSenaiteBaseUrl() + SenaiteClient.CREATE_ENDPOINT)
                .log("Response senaite-create-contact: ${body}")
                .end();
        // spotless:on
    }
}
