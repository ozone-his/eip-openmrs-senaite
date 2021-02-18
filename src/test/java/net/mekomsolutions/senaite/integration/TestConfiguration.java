package net.mekomsolutions.senaite.integration;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.DeadLetterChannelBuilder;
import org.apache.camel.spring.boot.CamelContextConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableAutoConfiguration
@ComponentScan({
    "org.openmrs.eip.component.utils"
})
public class TestConfiguration {
	
	@Bean
	CamelContextConfiguration contextConfiguration() {
	    return new CamelContextConfiguration() {

	        @Override
	        public void beforeApplicationStart(CamelContext context) {}

	        @Override
	        public void afterApplicationStart(CamelContext camelContext) {}
	    };
	}
	
    @Bean
    public DeadLetterChannelBuilder deadLetterChannelBuilder() {
        DeadLetterChannelBuilder builder = new DeadLetterChannelBuilder("direct:dlc");
        builder.setUseOriginalMessage(true);
        return builder;
    }

}