package com.ozonehis.eip.routes.senaite;

import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.support.DefaultExchange;
import org.apache.camel.test.spring.junit5.MockEndpoints;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openmrs.eip.mysql.watcher.route.BaseWatcherRouteTest;
import org.springframework.context.annotation.Import;

@MockEndpoints
@Import({ TestConfiguration.class})
public class UpdateServiceRequestTaskToOpenmrsRouteITest extends BaseWatcherRouteTest {  

	@EndpointInject(value = "mock:authenticateToOpenmrsRoute")
    private MockEndpoint authenticateToOpenmrs;
    
    @EndpointInject(value = "mock:updateTaskEndpoint")
    private MockEndpoint updateTaskEndpoint; 
    
    @BeforeEach
    public void setup() throws Exception {
    	loadXmlRoutesInDirectory("camel", "update-servicerequest-task-to-openmrs-route.xml");
		
    	advise("update-servicerequest-task-to-openmrs", new AdviceWithRouteBuilder() {
    	    @Override
    	    public void configure() {
    	    	weaveByToString("To[direct:authenticate-to-openmrs]").replace().toD("mock:authenticateToOpenmrsRoute");
    	    	weaveByToString(".*/Task/\\$\\{exchangeProperty.task-id\\}]").replace().toD("mock:updateTaskEndpoint");
    	    }
    	});
    	
    	setupExpectations();
    	
    }
    
    @AfterEach
    public void reset() throws Exception {
    	updateTaskEndpoint.reset();
    }

    @Test
    public void shouldUpdateTaskState() throws Exception {
    	// setup
    	Exchange exchange = new DefaultExchange(camelContext);
    	exchange.setProperty("task-id", "77ee5f8b-c9ec-4d1f-b3a3-fdae006f1032");
    	exchange.setProperty("service-request-transitioned-status", "accepted");
    	
    	// replay
    	producerTemplate.send("direct:update-servicerequest-task-to-openmrs", exchange);
    	
    	// verify
    	authenticateToOpenmrs.assertExchangeReceived(0);
    	updateTaskEndpoint.assertIsSatisfied();
    }
    
    private void setupExpectations() {
    	updateTaskEndpoint.expectedHeaderReceived(Exchange.HTTP_METHOD, "PUT");
    	updateTaskEndpoint.expectedBodiesReceived("{\"resourceType\": \"Task\", \"id\": \"77ee5f8b-c9ec-4d1f-b3a3-fdae006f1032\", \"status\": \"accepted\", \"intent\": \"order\"}");
    }

}
