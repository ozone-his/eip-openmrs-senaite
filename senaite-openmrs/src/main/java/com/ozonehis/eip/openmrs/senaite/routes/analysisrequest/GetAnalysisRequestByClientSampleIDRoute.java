/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openmrs.senaite.routes.analysisrequest;

import com.ozonehis.eip.openmrs.senaite.Constants;
import com.ozonehis.eip.openmrs.senaite.config.SenaiteConfig;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class GetAnalysisRequestByClientSampleIDRoute extends RouteBuilder {

    @Autowired
    private SenaiteConfig senaiteConfig;

    private static final String GET_ANALYSIS_REQUEST_ENDPOINT =
            "/@@API/senaite/v1/AnalysisRequest?getClientSampleID=%s&catalog=senaite_catalog_sample&complete=true";

    @Override
    public void configure() {
        // spotless:off
        from("direct:senaite-get-analysis-request-by-client-sample-id-route")
                .log(LoggingLevel.INFO, "Fetching AnalysisRequest in SENAITE...")
                .routeId("senaite-get-analysis-request-by-client-sample-id-route")
                .setHeader(Constants.CAMEL_HTTP_METHOD, constant(Constants.GET))
                .setHeader(Constants.CONTENT_TYPE, constant(Constants.APPLICATION_JSON))
                .setHeader(Constants.AUTHORIZATION, constant(senaiteConfig.authHeader()))
                .toD(senaiteConfig.getSenaiteBaseUrl()
                        + String.format(
                                GET_ANALYSIS_REQUEST_ENDPOINT,
                                "${header." + Constants.HEADER_CLIENT_SAMPLE_ID + "}"))
                .end();
        // spotless:on
    }
}
