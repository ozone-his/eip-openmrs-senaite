/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openmrs.senaite.routes.analyses;

import com.ozonehis.eip.openmrs.senaite.Constants;
import com.ozonehis.eip.openmrs.senaite.config.SenaiteConfig;
import lombok.AllArgsConstructor;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@AllArgsConstructor
@Component
public class GetAnalysesByAnalysisRequestRoute extends RouteBuilder {

    @Autowired
    private SenaiteConfig senaiteConfig;

    // SENAITE pages results at 25 by default, so ask for enough to get every analysis of a sample in one response.
    public static final int MAX_ANALYSES_PER_REQUEST = 1000;

    private static final String GET_ANALYSES_BY_ANALYSIS_REQUEST_ENDPOINT =
            "/@@API/senaite/v1/Analysis?getAncestorsUIDs=%s&catalog=senaite_catalog_analysis&complete=true&limit="
                    + MAX_ANALYSES_PER_REQUEST;

    @Override
    public void configure() {
        // spotless:off
        from("direct:senaite-get-analyses-by-analysis-request-route")
                .log(LoggingLevel.INFO, "Fetching Analyses of AnalysisRequest in SENAITE...")
                .routeId("senaite-get-analyses-by-analysis-request-route")
                .setHeader(Constants.CAMEL_HTTP_METHOD, constant(Constants.GET))
                .setHeader(Constants.CONTENT_TYPE, constant(Constants.APPLICATION_JSON))
                .setHeader(Constants.AUTHORIZATION, constant(senaiteConfig.authHeader()))
                .toD(senaiteConfig.getSenaiteBaseUrl()
                        + String.format(
                                GET_ANALYSES_BY_ANALYSIS_REQUEST_ENDPOINT,
                                "${header." + Constants.HEADER_ANALYSIS_REQUEST_UID + "}"))
                .end();
        // spotless:on
    }
}
