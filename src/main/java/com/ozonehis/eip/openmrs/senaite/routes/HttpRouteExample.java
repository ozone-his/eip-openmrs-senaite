package com.ozonehis.eip.openmrs.senaite.routes;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ozonehis.eip.openmrs.senaite.model.Client;
import org.apache.camel.CamelContext;
import org.apache.camel.LoggingLevel;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.commons.codec.binary.Base64;

public class HttpRouteExample {
    public static void main(String[] args) throws Exception {
        CamelContext context = new DefaultCamelContext();

        context.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                String auth = "admin" + ":" + "password";
                byte[] encodedAuth = Base64.encodeBase64(auth.getBytes());
                String authHeader = "Basic " + new String(encodedAuth);
                Client client = new Client();
                client.setPortalType("Client");
                client.setTitle("SID SID");
                client.setClientID("123234");
                client.setParentPath("/senaite/clients");

                // spotless:off
                from("direct:senaite-create-client-route")
                        .log(LoggingLevel.INFO, "Creating Client in SENAITE...")
                        .routeId("senaite-create-client-route")
                        .setHeader("CamelHttpMethod", constant("POST"))
                        .setHeader("Content-Type", constant("application/json"))
                        .setHeader("Authorization", constant(authHeader))
                        .to("http://localhost:8081/senaite/@@API/senaite/v1/create")
                        .log("Response from save: ${body}")
                        .end();
                // spotless:on
            }
        });

        context.start();

        // Create an instance of Client
        Client client = new Client();
        client.setPortalType("Client");
        client.setTitle("SID SID");
        client.setClientID("123234");
        client.setParentPath("/senaite/clients");
        ObjectMapper objectMapper = new ObjectMapper();
        String jsonString = objectMapper.writeValueAsString(client);

        ProducerTemplate template = context.createProducerTemplate();
        System.out.println(jsonString);
        template.sendBody("direct:senaite-create-client-route", jsonString);

        Thread.sleep(300000); // Keep the context running for 5 minutes
        context.stop();
    }
}
