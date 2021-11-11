package net.mekomsolutions.eip.routes.senaite;

import java.util.stream.Collectors;

import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.model.ModelCamelContext;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.reifier.RouteReifier;
import org.apache.camel.support.DefaultExchange;
import org.apache.camel.test.spring.MockEndpoints;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.eip.mysql.watcher.route.BaseWatcherRouteTest;
import org.springframework.context.annotation.Import;

@MockEndpoints
@Import({ TestConfiguration.class})
public class UpdateServiceRequestTaskToOpenmrsRouteITest extends BaseWatcherRouteTest {  

	@EndpointInject(value = "mock:authenticateToOpenmrsRoute")
    private MockEndpoint authenticateToOpenmrs;
    
    @EndpointInject(value = "mock:updateTaskEndpoint")
    private MockEndpoint updateTaskEndpoint; 
    
    @Before
    public void setup() throws Exception {
    	loadXmlRoutesInDirectory("senaite", "update-serviceRequest-task-toOpenmrs-route.xml");
    	RouteDefinition routeDefinition = camelContext.adapt(ModelCamelContext.class).getRouteDefinitions().stream().filter(routeDef -> "update-serviceRequest-task".equals(routeDef.getRouteId())).collect(Collectors.toList()).get(0);
    	RouteReifier.adviceWith(routeDefinition, camelContext, new AdviceWithRouteBuilder() {
    	    @Override
    	    public void configure() throws Exception {
    	    	weaveByToString("To[direct:authenticate-toOpenmrs]").replace().toD("mock:authenticateToOpenmrsRoute");
    	    	weaveByToString("DynamicTo[{{openmrs.baseUrl}}/ws/fhir2/R4/Task/${exchangeProperty.task-id}]").replace().toD("mock:updateTaskEndpoint");
    	    }
    	});
    	
    	setupExpectations();
    	
    }
    
    @After
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
    	producerTemplate.send("direct:update-serviceRequest-task", exchange);
    	
    	// verify
    	authenticateToOpenmrs.assertExchangeReceived(0);
    	updateTaskEndpoint.assertIsSatisfied();
    }
    
    private void setupExpectations() {
    	updateTaskEndpoint.expectedHeaderReceived(Exchange.HTTP_METHOD, "PUT");
    	updateTaskEndpoint.expectedBodiesReceived("{\"resourceType\": \"Task\", \"id\": \"77ee5f8b-c9ec-4d1f-b3a3-fdae006f1032\", \"status\": \"accepted\", \"intent\": \"order\"}");
    }

}