package net.mekomsolutions.eip.routes.senaite;

import static org.junit.Assert.assertEquals;

import java.util.stream.Collectors;

import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.model.ModelCamelContext;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.reifier.RouteReifier;
import org.apache.camel.support.DefaultExchange;
import org.apache.camel.test.spring.MockEndpoints;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.eip.mysql.watcher.route.BaseWatcherRouteTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.TestExecutionListeners.MergeMode;

@MockEndpoints
@Import({ TestConfiguration.class})
@TestExecutionListeners(listeners = {}, mergeMode = MergeMode.REPLACE_DEFAULTS)
public class RetrieveContactNamesRouteITest extends BaseWatcherRouteTest {  

	@EndpointInject(value = "mock:authenticateToOpenmrsRoute")
    private MockEndpoint authenticateToOpenmrs;
	
    @EndpointInject(value = "mock:requesterEndpoint")
    private MockEndpoint requesterEndpoint;
    
    @Before
    public void setup() throws Exception {
    	loadXmlRoutesInDirectory("senaite", "retrieve-orderer-names-from-openmrs-route.xml");
    	RouteDefinition routeDefinition = camelContext.adapt(ModelCamelContext.class).getRouteDefinitions().stream().filter(routeDef -> "retrieve-orderer-names-from-openmrs".equals(routeDef.getRouteId())).collect(Collectors.toList()).get(0);
    	RouteReifier.adviceWith(routeDefinition, camelContext, new AdviceWithRouteBuilder() {
    	    @Override
    	    public void configure() throws Exception {
    	    	weaveByToString("To[direct:authenticate-to-openmrs]").replace().toD("mock:authenticateToOpenmrsRoute");
    	    	weaveByToString("DynamicTo[{{fhirR4.baseUrl}}/${exchangeProperty.requester-reference}]").replace().toD("mock:requesterEndpoint");
    	    }
    	});
    	
    	setupExpectations();
    	
    }
    
    @Test
    public void shouldSetFamilyAndGivenRequesterNames() throws Exception {
    	// setup
    	Exchange exchange = new DefaultExchange(camelContext);
    	exchange.setProperty("requester-reference", "Practitioner/d042d719-1d09-11ec-9616-0242ac1a000a");
    	
    	// replay
    	producerTemplate.send("direct:retrieve-orderer-names-from-openmrs", exchange);
    	
    	// verify
    	authenticateToOpenmrs.assertExchangeReceived(0);
    	requesterEndpoint.assertIsSatisfied();
    	assertEquals("Man", exchange.getProperty("requester-family-name"));
    	assertEquals("Super", exchange.getProperty("requester-given-name"));
    	
    	
    }
    
    private void setupExpectations() {    	
    	requesterEndpoint.whenAnyExchangeReceived(new Processor () {
			@Override
			public void process(Exchange exchange) throws Exception {
				exchange.getIn().setBody("{\"resourceType\":\"Practitioner\",\"id\":\"d042d719-1d09-11ec-9616-0242ac1a000a\",\"text\":{\"status\":\"generated\",\"div\":\"<div/>\"},\"identifier\":[{\"system\":\"http://fhir.openmrs.org/ext/provider/identifier\",\"value\":\"superman\"}],\"active\":true,\"name\":[{\"id\":\"d041e155-1d09-11ec-9616-0242ac1a000a\",\"family\":\"Man\",\"given\":[\"Super\"]}],\"gender\":\"male\"}");
			}
    		
    	});
    	requesterEndpoint.expectedHeaderReceived(Exchange.HTTP_METHOD, "GET");
    	requesterEndpoint.expectedPropertyReceived("requester-reference", "Practitioner/d042d719-1d09-11ec-9616-0242ac1a000a");
    }

}