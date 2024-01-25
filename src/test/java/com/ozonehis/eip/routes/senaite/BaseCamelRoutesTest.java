package com.ozonehis.eip.routes.senaite;

import org.openmrs.eip.mysql.watcher.route.BaseWatcherRouteTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@ActiveProfiles("test")
@Import({TestConfiguration.class})
@TestPropertySource(properties = {"camel.springboot.routes-collector-enabled=false"})
public class BaseCamelRoutesTest extends BaseWatcherRouteTest {}
