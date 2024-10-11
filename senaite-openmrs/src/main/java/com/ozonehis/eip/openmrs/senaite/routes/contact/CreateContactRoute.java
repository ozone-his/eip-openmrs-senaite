/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openmrs.senaite.routes.contact;

import com.ozonehis.eip.openmrs.senaite.Constants;
import com.ozonehis.eip.openmrs.senaite.config.SenaiteConfig;
import lombok.AllArgsConstructor;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@AllArgsConstructor
@Component
public class CreateContactRoute extends RouteBuilder {

    @Autowired
    private SenaiteConfig senaiteConfig;

    public static final String CREATE_ENDPOINT = "/@@API/senaite/v1/create";

    @Override
    public void configure() {
        // spotless:off
        from("direct:senaite-create-contact-route")
                .log(LoggingLevel.INFO, "Creating Contact in SENAITE...")
                .routeId("senaite-create-contact-route")
                .setHeader(Constants.CAMEL_HTTP_METHOD, constant(Constants.POST))
                .setHeader(Constants.CONTENT_TYPE, constant(Constants.APPLICATION_JSON))
                .setHeader(Constants.AUTHORIZATION, constant(senaiteConfig.authHeader()))
                .to(senaiteConfig.getSenaiteBaseUrl() + CREATE_ENDPOINT)
                .end();
        // spotless:on
    }
}
