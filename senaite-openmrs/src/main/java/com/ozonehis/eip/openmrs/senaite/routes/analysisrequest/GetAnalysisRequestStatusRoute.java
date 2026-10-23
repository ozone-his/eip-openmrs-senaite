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
import lombok.AllArgsConstructor;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@AllArgsConstructor
@Component
public class GetAnalysisRequestStatusRoute extends RouteBuilder {

    @Autowired
    private SenaiteConfig senaiteConfig;

    // Catalog metadata only (no complete=true), so SENAITE answers from the catalog without loading the sample object.
    // Task polling runs this for every open Task on every cycle.
    private static final String GET_ANALYSIS_REQUEST_STATUS_ENDPOINT =
            "/@@API/senaite/v1/AnalysisRequest?getClientSampleID=%s&getClientID=%s&catalog=senaite_catalog_sample";

    @Override
    public void configure() {
        // spotless:off
        from("direct:senaite-get-analysis-request-status-route")
                .log(LoggingLevel.INFO, "Fetching AnalysisRequest status in SENAITE...")
                .routeId("senaite-get-analysis-request-status-route")
                .setHeader(Constants.CAMEL_HTTP_METHOD, constant(Constants.GET))
                .setHeader(Constants.CONTENT_TYPE, constant(Constants.APPLICATION_JSON))
                .setHeader(Constants.AUTHORIZATION, constant(senaiteConfig.authHeader()))
                .toD(senaiteConfig.getSenaiteBaseUrl()
                        + String.format(
                                GET_ANALYSIS_REQUEST_STATUS_ENDPOINT,
                                "${header." + Constants.HEADER_CLIENT_SAMPLE_ID + "}",
                                "${header." + Constants.HEADER_CLIENT_ID + "}"))
                .end();
        // spotless:on
    }
}
