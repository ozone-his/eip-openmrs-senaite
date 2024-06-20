package com.ozonehis.eip.openmrs.senaite.routes;

import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.commons.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CreateClientRoute extends RouteBuilder {

    @Value("${senaite.username}")
    private String senaiteUsername;

    @Value("${senaite.password}")
    private String senaitePassword;

    @Value("${senaite.baseUrl}")
    private String senaiteBaseUrl;

    @Override
    public void configure() {
        String auth = senaiteUsername + ":" + senaitePassword;
        byte[] encodedAuth = Base64.encodeBase64(auth.getBytes());
        String authHeader = "Basic " + new String(encodedAuth);

        // spotless:off
        from("direct:senaite-create-client-route")
            .log(LoggingLevel.INFO, "Creating Client in SENAITE...")
            .routeId("senaite-create-client-route")
            .setHeader("CamelHttpMethod", constant("POST"))
            .setHeader("Content-Type", constant("application/json"))
            .setHeader("Authorization", constant(authHeader))
            .to(senaiteBaseUrl + "/@@API/senaite/v1/create")
            .log("Response: ${body}")
                .end();
        // spotless:on
    }
}
