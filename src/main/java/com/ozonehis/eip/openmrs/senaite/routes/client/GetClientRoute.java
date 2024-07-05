package com.ozonehis.eip.openmrs.senaite.routes.client;

import com.ozonehis.eip.openmrs.senaite.Constants;
import com.ozonehis.eip.openmrs.senaite.client.SenaiteClient;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class GetClientRoute extends RouteBuilder {

    @Autowired
    private SenaiteClient senaiteClient;

    private static final String GET_CLIENT_ENDPOINT = "/@@API/senaite/v1/client?getClientID=";

    @Override
    public void configure() {
        // spotless:off
        from("direct:senaite-get-client-route")
                .log(LoggingLevel.INFO, "Fetching Client in SENAITE...")
                .routeId("senaite-get-client-route")
                .setHeader(Constants.CAMEL_HTTP_METHOD, constant(Constants.GET))
                .setHeader(Constants.CONTENT_TYPE, constant(Constants.APPLICATION_JSON))
                .setHeader(Constants.AUTHORIZATION, constant(senaiteClient.authHeader()))
                .toD(senaiteClient.getSenaiteBaseUrl() + GET_CLIENT_ENDPOINT + "${header." + Constants.HEADER_CLIENT_ID
                        + "}")
                .log(
                        LoggingLevel.INFO,
                        "Response get-client: ${body} clientID ${header." + Constants.HEADER_CLIENT_ID + "}")
                .end();
        // spotless:on
    }
}
