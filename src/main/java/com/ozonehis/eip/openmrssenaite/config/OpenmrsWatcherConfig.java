package com.ozonehis.eip.openmrssenaite.config;

import org.openmrs.eip.app.config.AppConfig;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Import the {@link AppConfig} class to ensure that the required beans are created.
 */
@Configuration
@Import(AppConfig.class)
public class OpenmrsWatcherConfig {}
