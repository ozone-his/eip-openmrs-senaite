/*
 * Copyright © 2024, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openmrs.senaite.routes;

import com.ozonehis.eip.openmrs.senaite.converters.AnalysisRequestConverter;
import com.ozonehis.eip.openmrs.senaite.converters.ClientConverter;
import com.ozonehis.eip.openmrs.senaite.converters.ContactConverter;
import com.ozonehis.eip.openmrs.senaite.converters.ServiceRequestConverter;
import com.ozonehis.eip.openmrs.senaite.converters.TaskConverter;
import com.ozonehis.eip.openmrs.senaite.processors.TaskProcessor;
import lombok.Setter;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Setter
@Component
public class TaskRouting extends RouteBuilder {

    @Autowired
    private TaskProcessor taskProcessor;

    @Autowired
    private ClientConverter clientConverter;

    @Autowired
    private AnalysisRequestConverter analysisRequestConverter;

    @Autowired
    private TaskConverter taskConverter;

    @Autowired
    private ContactConverter contactConverter;

    @Autowired
    private ServiceRequestConverter serviceRequestConverter;

    @Override
    public void configure() {
        getContext().getTypeConverterRegistry().addTypeConverters(clientConverter);
        getContext().getTypeConverterRegistry().addTypeConverters(analysisRequestConverter);
        getContext().getTypeConverterRegistry().addTypeConverters(taskConverter);
        getContext().getTypeConverterRegistry().addTypeConverters(contactConverter);
        getContext().getTypeConverterRegistry().addTypeConverters(serviceRequestConverter);
        // spotless:off
        from("scheduler:taskUpdate?initialDelay=60000&delay=60000")
                .routeId("poll-senaite")
                .log("Scheduled FHIR Task status updater")
                .to("direct:openmrs-get-task-by-status-route")
                .process(taskProcessor)
                .log(LoggingLevel.INFO, "Polling Tasks completed...")
                .end();
        // spotless:on
    }
}
