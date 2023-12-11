package com.ozonehis.eip.routes.senaite;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;

@Configuration
@ComponentScan({
    "org.openmrs.eip", "com.ozonehis.eip"
})
@ActiveProfiles("test")
public class TestConfiguration {}
