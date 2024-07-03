package com.ozonehis.eip.openmrs.senaite.routes.analysisrequest;

import com.ozonehis.eip.openmrs.senaite.Constants;
import com.ozonehis.eip.openmrs.senaite.client.SenaiteClient;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class GetAnalysisRequestRoute extends RouteBuilder {

    @Autowired
    private SenaiteClient senaiteClient;

    private static final String GET_ANALYSIS_REQUEST_ENDPOINT = "/@@API/senaite/v1/AnalysisRequest?getClientID=";

    @Override
    public void configure() {
        // spotless:off
        from("direct:senaite-get-analysis-request-route")
                .log(LoggingLevel.INFO, "Fetching AnalysisRequest in SENAITE...")
                .routeId("senaite-get-analysis-request-route")
                .setHeader(Constants.CAMEL_HTTP_METHOD, constant(Constants.GET))
                .setHeader(Constants.CONTENT_TYPE, constant(Constants.APPLICATION_JSON))
                .setHeader(Constants.AUTHORIZATION, constant(senaiteClient.authHeader()))
                .to(senaiteClient.getSenaiteBaseUrl()
                        + GET_ANALYSIS_REQUEST_ENDPOINT
                        + exchangeProperty("clientId")) // TODO: Check if correct url
                .log("Response get-analysis-request: ${body}")
                .end();
        // spotless:on
    }
}
