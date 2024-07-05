package com.ozonehis.eip.openmrs.senaite.routes.contact;

import com.ozonehis.eip.openmrs.senaite.Constants;
import com.ozonehis.eip.openmrs.senaite.client.SenaiteClient;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class GetContactRoute extends RouteBuilder {

    @Autowired
    private SenaiteClient senaiteClient;

    private static final String GET_CONTACT_ENDPOINT = "/@@API/senaite/v1/Contact?depth=2&path=";

    @Override
    public void configure() {
        // spotless:off
        from("direct:senaite-get-contact-route")
                .log(LoggingLevel.INFO, "Fetching Contact in SENAITE...")
                .routeId("senaite-get-contact-route")
                .setHeader(Constants.CAMEL_HTTP_METHOD, constant(Constants.GET))
                .setHeader(Constants.CONTENT_TYPE, constant(Constants.APPLICATION_JSON))
                .setHeader(Constants.AUTHORIZATION, constant(senaiteClient.authHeader()))
                .to(senaiteClient.getSenaiteBaseUrl() + GET_CONTACT_ENDPOINT + header(Constants.HEADER_PATH))
                .log("Response get-contact: ${body}")
                .end();
        // spotless:on
    }
}
