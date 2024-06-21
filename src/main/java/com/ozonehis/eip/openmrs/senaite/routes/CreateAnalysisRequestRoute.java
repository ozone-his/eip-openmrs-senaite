package com.ozonehis.eip.openmrs.senaite.routes;

import com.ozonehis.eip.openmrs.senaite.Constants;
import com.ozonehis.eip.openmrs.senaite.client.SenaiteClient;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CreateAnalysisRequestRoute extends RouteBuilder {

    @Autowired
    private SenaiteClient senaiteClient;

    @Override
    public void configure() {
        // spotless:off
        from("direct:senaite-create-analysis-request-route")
                .log(LoggingLevel.INFO, "Creating AnalysisRequest in SENAITE...")
                .routeId("senaite-create-analysis-request-route")
                .setHeader(Constants.CAMEL_HTTP_METHOD, constant(Constants.POST))
                .setHeader(Constants.CONTENT_TYPE, constant(Constants.APPLICATION_JSON))
                .setHeader(Constants.AUTHORIZATION, constant(senaiteClient.authHeader()))
                .to(senaiteClient.getSenaiteBaseUrl() + SenaiteClient.CREATE_ENDPOINT)
                .log("Response create-analysis-request: ${body}")
                .end();
        // spotless:on
    }
}
