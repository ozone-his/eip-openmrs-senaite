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
public class CreateServiceRequestTaskToOpenmrsRouteITest extends BaseWatcherRouteTest {  

	@EndpointInject(value = "mock:authenticateToOpenmrsRoute")
    private MockEndpoint authenticateToOpenmrs;
    
    @EndpointInject(value = "mock:createTaskOpenmrsEndpoint")
    private MockEndpoint createTaskOpenmrsEndpoint; 
    
    @Before
    public void setup() throws Exception {
    	loadXmlRoutesInDirectory("senaite", "create-serviceRequestTask-to-openmrs-route.xml");
    	RouteDefinition routeDefinition = camelContext.adapt(ModelCamelContext.class).getRouteDefinitions().stream().filter(routeDef -> "create-serviceRequestTask-to-openmrs".equals(routeDef.getRouteId())).collect(Collectors.toList()).get(0);
    	RouteReifier.adviceWith(routeDefinition, camelContext, new AdviceWithRouteBuilder() {
    	    @Override
    	    public void configure() throws Exception {
    	    	weaveByToString("To[direct:authenticate-to-openmrs]").replace().toD("mock:authenticateToOpenmrsRoute");
    	    	weaveByToString("DynamicTo[{{openmrs.baseUrl}}/ws/fhir2/R4/Task]").replace().toD("mock:createTaskOpenmrsEndpoint");
    	    }
    	});
    	
    	setupExpectations();
    	
    }
    
    @After
    public void reset() throws Exception {
    	createTaskOpenmrsEndpoint.reset();
    }

    @Test
    public void shouldCreateServiceRequestTaskOnOpenmrs() throws Exception {
    	// setup
    	Exchange exchange = new DefaultExchange(camelContext);
    	exchange.setProperty("lab-order-uuid", "8ee73df9-b80e-49d9-9fd1-8a5b6864178f");
    	
    	// replay
    	producerTemplate.send("direct:create-serviceRequestTask-to-openmrs", exchange);
    	
    	// verify
    	authenticateToOpenmrs.assertExchangeReceived(0);
    	authenticateToOpenmrs.assertIsSatisfied();
    	createTaskOpenmrsEndpoint.assertIsSatisfied();
    }
    
    private void setupExpectations() {
    	createTaskOpenmrsEndpoint.expectedHeaderReceived(Exchange.HTTP_METHOD, "POST");
    	createTaskOpenmrsEndpoint.expectedBodiesReceived("{\"resourceType\": \"Task\", \"status\": \"requested\", \"intent\": \"order\", \"basedOn\": [{\"reference\":\"8ee73df9-b80e-49d9-9fd1-8a5b6864178f\", \"type\": \"ServiceRequest\"}]}");
    	createTaskOpenmrsEndpoint.expectedPropertyReceived("lab-order-uuid", "8ee73df9-b80e-49d9-9fd1-8a5b6864178f");
    }
}