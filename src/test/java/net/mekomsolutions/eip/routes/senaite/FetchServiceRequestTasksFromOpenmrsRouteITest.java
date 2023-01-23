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
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.eip.mysql.watcher.route.BaseWatcherRouteTest;
import org.springframework.context.annotation.Import;

@MockEndpoints
@Import({ TestConfiguration.class})
public class FetchServiceRequestTasksFromOpenmrsRouteITest extends BaseWatcherRouteTest {
    
    @EndpointInject(value = "mock:authenticateToOpenmrsRoute")
    private MockEndpoint authenticateToOpenmrs;
    
    @EndpointInject(value = "mock:fetchTasksEndpoint")
    private MockEndpoint fetchTasksEndpoint; 
    
    @Before
    public void setup() throws Exception {
    	loadXmlRoutesInDirectory("senaite", "fetch-servicerequest-tasks-from-openmrs-route.xml");
    	RouteDefinition routeDefinition = camelContext.adapt(ModelCamelContext.class).getRouteDefinitions().stream().filter(routeDef -> "fetch-servicerequest-tasks-from-openmrs".equals(routeDef.getRouteId())).collect(Collectors.toList()).get(0);
    	RouteReifier.adviceWith(routeDefinition, camelContext, new AdviceWithRouteBuilder() {
    	    @Override
    	    public void configure() throws Exception {
    	    	weaveByToString("To[direct:authenticate-to-openmrs]").replace().toD("mock:authenticateToOpenmrsRoute");
    	    	weaveByToString("DynamicTo[{{fhirR4.baseUrl}}/Task?status=requested,accepted]").replace().toD("mock:fetchTasksEndpoint");
    	    }
    	});
    	
    	setupExpectations();
    	
    }
    
    @After
    public void reset() throws Exception {
    	fetchTasksEndpoint.reset();
    }

    @Test
    public void shouldFethRequestedAndOrAcceptedServiceRequestTasks() throws Exception {
    	// setup
    	String expectedBody = "[{\"fullUrl\":\"http:\\/\\/openmrs:8080\\/openmrs\\/ws\\/fhir2\\/R4\\/Task\\/77ee5f8b-c9ec-4d1f-b3a3-fdae006f1032\",\"resource\":{\"resourceType\":\"Task\",\"id\":\"77ee5f8b-c9ec-4d1f-b3a3-fdae006f1032\",\"text\":{\"status\":\"generated\",\"div\":\"<div \\/>\"},\"identifier\":[{\"system\":\"http:\\/\\/fhir.openmrs.org\\/ext\\/task\\/identifier\",\"value\":\"77ee5f8b-c9ec-4d1f-b3a3-fdae006f1032\"}],\"basedOn\":[{\"reference\":\"8ee73df9-b80e-49d9-9fd1-8a5b6864178f\",\"type\":\"ServiceRequest\"}],\"status\":\"requested\",\"intent\":\"order\",\"authoredOn\":\"2021-11-11T08:27:55+00:00\",\"lastModified\":\"2021-11-11T08:27:55+00:00\"}}]";
    	Exchange exchange = new DefaultExchange(camelContext);
    	
    	// replay
    	producerTemplate.send("direct:fetch-servicerequest-tasks-from-openmrs", exchange);
    	
    	// verify
    	authenticateToOpenmrs.assertExchangeReceived(0);
    	fetchTasksEndpoint.assertIsSatisfied();
    	assertEquals(expectedBody, exchange.getIn().getBody().toString());
    }
    
    private void setupExpectations() {
    	fetchTasksEndpoint.whenAnyExchangeReceived(new Processor () {
			@Override
			public void process(Exchange exchange) throws Exception {
				exchange.getIn().setBody("{\"resourceType\":\"Bundle\",\"id\":\"481814d7-436d-43ee-aa1e-5ebc4b10441a\",\"meta\":{\"lastUpdated\":\"2021-11-11T17:21:33.783+00:00\"},\"type\":\"searchset\",\"total\":1,\"link\":[{\"relation\":\"self\",\"url\":\"http://openmrs:8080/openmrs/ws/fhir2/R4/Task?status=requested%2Caccepted\"}],\"entry\":[{\"fullUrl\":\"http://openmrs:8080/openmrs/ws/fhir2/R4/Task/77ee5f8b-c9ec-4d1f-b3a3-fdae006f1032\",\"resource\":{\"resourceType\":\"Task\",\"id\":\"77ee5f8b-c9ec-4d1f-b3a3-fdae006f1032\",\"text\":{\"status\":\"generated\",\"div\":\"<div />\"},\"identifier\":[{\"system\":\"http://fhir.openmrs.org/ext/task/identifier\",\"value\":\"77ee5f8b-c9ec-4d1f-b3a3-fdae006f1032\"}],\"basedOn\":[{\"reference\":\"8ee73df9-b80e-49d9-9fd1-8a5b6864178f\",\"type\":\"ServiceRequest\"}],\"status\":\"requested\",\"intent\":\"order\",\"authoredOn\":\"2021-11-11T08:27:55+00:00\",\"lastModified\":\"2021-11-11T08:27:55+00:00\"}}]}");
			}
    		
    	});
    	fetchTasksEndpoint.expectedHeaderReceived(Exchange.HTTP_METHOD, "GET");
    }

}