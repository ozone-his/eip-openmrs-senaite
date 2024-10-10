/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openmrs.senaite.routes.client;

import com.ozonehis.eip.openmrs.senaite.Constants;
import com.ozonehis.eip.openmrs.senaite.config.SenaiteConfig;
import lombok.AllArgsConstructor;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@AllArgsConstructor
@Component
public class GetClientRoute extends RouteBuilder {

    @Autowired
    private SenaiteConfig senaiteConfig;

    private static final String GET_CLIENT_ENDPOINT = "/@@API/senaite/v1/client?getClientID=";

    @Override
    public void configure() {
        // spotless:off
        from("direct:senaite-get-client-route")
                .log(LoggingLevel.INFO, "Fetching Client in SENAITE...")
                .routeId("senaite-get-client-route")
                .setHeader(Constants.CAMEL_HTTP_METHOD, constant(Constants.GET))
                .setHeader(Constants.CONTENT_TYPE, constant(Constants.APPLICATION_JSON))
                .setHeader(Constants.AUTHORIZATION, constant(senaiteConfig.authHeader()))
                .toD(senaiteConfig.getSenaiteBaseUrl() + GET_CLIENT_ENDPOINT + "${header." + Constants.HEADER_CLIENT_ID
                        + "}")
                .end();
        // spotless:on
    }
}
