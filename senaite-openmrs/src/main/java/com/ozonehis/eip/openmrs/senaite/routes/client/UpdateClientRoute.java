/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openmrs.senaite.routes.client;

import com.ozonehis.eip.openmrs.senaite.Constants;
import com.ozonehis.eip.openmrs.senaite.client.SenaiteClient;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class UpdateClientRoute extends RouteBuilder {

    @Autowired
    private SenaiteClient senaiteClient;

    public static final String UPDATE_ENDPOINT = "/@@API/senaite/v1/update";

    @Override
    public void configure() {
        // spotless:off
        from("direct:senaite-update-client-route")
                .log(LoggingLevel.INFO, "Updating Client in SENAITE...")
                .routeId("senaite-update-client-route")
                .setHeader(Constants.CAMEL_HTTP_METHOD, constant(Constants.POST))
                .setHeader(Constants.CONTENT_TYPE, constant(Constants.APPLICATION_JSON))
                .setHeader(Constants.AUTHORIZATION, constant(senaiteClient.authHeader()))
                .to(senaiteClient.getSenaiteBaseUrl() + UPDATE_ENDPOINT)
                .log("Response update-client-route: ${body}")
                .end();
        // spotless:on
    }
}
