package com.ozonehis.eip.openmrs.senaite.routes;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

/**
 * This route is used to initialize the OpenMRS watcher.
 * It is only used once when the application starts up.
 */
@Component
public class OpenmrsWatcherInitRoute extends RouteBuilder {

    @Override
    public void configure() throws Exception {
        from("scheduler:openmrs-watcher?initialDelay=500&repeatCount=1")
                .routeId("openmrs-watcher-init-route")
                .to("openmrs-watcher:init");
    }
}
