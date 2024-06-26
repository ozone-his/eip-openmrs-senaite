package com.ozonehis.eip.openmrssenaite.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource(
        value = {"eip-openmrs-senaite.properties", "classpath:eip-openmrs-senaite.properties"},
        ignoreResourceNotFound = true)
public class OpenmrsSenaiteConfig {}
