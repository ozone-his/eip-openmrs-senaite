package net.mekomsolutions.senaite.integration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {"org.openmrs.eip"})

public class SenaiteIntegrationApplication {
	
	private static final Logger logger = LoggerFactory.getLogger(SenaiteIntegrationApplication.class);
	
	public static void main(final String[] args) {
		logger.info("Starting Senaite Integration application...");
		
		SpringApplication.run(SenaiteIntegrationApplication.class, args);
	}
	
}
