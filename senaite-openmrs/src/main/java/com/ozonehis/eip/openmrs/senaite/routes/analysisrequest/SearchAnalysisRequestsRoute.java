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
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@AllArgsConstructor
@Component
public class SearchAnalysisRequestsRoute extends RouteBuilder {

    @Autowired
    private SenaiteConfig senaiteConfig;

    static String searchEndpoint(String baseUrl) {
        return baseUrl + "/@@API/senaite/v1/AnalysisRequest";
    }

    @Override
    public void configure() {
        // spotless:off
        from("direct:senaite-search-analysis-requests-route")
                .log(LoggingLevel.INFO, "Searching AnalysisRequests in SENAITE...")
                .routeId("senaite-search-analysis-requests-route")
                .setHeader(Constants.CAMEL_HTTP_METHOD, constant(Constants.GET))
                .setHeader(Constants.CONTENT_TYPE, constant(Constants.APPLICATION_JSON))
                .setHeader(Constants.AUTHORIZATION, constant(senaiteConfig.authHeader()))
                .setHeader(Exchange.HTTP_QUERY, header(Constants.HEADER_SENAITE_SEARCH_QUERY))
                .toD(searchEndpoint(senaiteConfig.getSenaiteBaseUrl()))
                .end();
        // spotless:on
    }
}
