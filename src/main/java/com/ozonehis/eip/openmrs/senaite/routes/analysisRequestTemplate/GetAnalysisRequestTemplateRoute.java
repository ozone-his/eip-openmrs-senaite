package com.ozonehis.eip.openmrs.senaite.routes.analysisRequestTemplate;

import com.ozonehis.eip.openmrs.senaite.Constants;
import com.ozonehis.eip.openmrs.senaite.client.SenaiteClient;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class GetAnalysisRequestTemplateRoute extends RouteBuilder {

    @Autowired
    private SenaiteClient senaiteClient;

    private static final String GET_ANALYSIS_REQUEST_TEMPLATE_ENDPOINT =
            "/@@API/senaite/v1/ARTemplate?complete=true&catalog=senaite_catalog_setup&Description=";

    @Override
    public void configure() {
        // spotless:off
        from("direct:senaite-get-analysis-request-template-route")
                .log(LoggingLevel.INFO, "Fetching AnalysisRequestTemplate in SENAITE...")
                .routeId("senaite-get-analysis-request-template-route")
                .setHeader(Constants.CAMEL_HTTP_METHOD, constant(Constants.GET))
                .setHeader(Constants.CONTENT_TYPE, constant(Constants.APPLICATION_JSON))
                .setHeader(Constants.AUTHORIZATION, constant(senaiteClient.authHeader()))
                .toD(senaiteClient.getSenaiteBaseUrl() + GET_ANALYSIS_REQUEST_TEMPLATE_ENDPOINT + "${header."
                        + Constants.HEADER_DESCRIPTION + "}")
                .log("Response get-analysis-request-template: ${body} description ${header."
                        + Constants.HEADER_DESCRIPTION + "}")
                .end();
        // spotless:on
    }
}
