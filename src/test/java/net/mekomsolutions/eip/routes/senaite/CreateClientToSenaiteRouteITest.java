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
public class CreateClientToSenaiteRouteITest extends BaseWatcherRouteTest {  
    
    @EndpointInject(value = "mock:authenticateToSenaiteRoute")
    private MockEndpoint authenticateToSenaiteRoute;
    
    @EndpointInject(value = "mock:searchClientSenaiteEndpoint")
    private MockEndpoint searchClientSenaiteEndpoint;
    
    @EndpointInject(value = "mock:createSenaiteEndpoint")
    private MockEndpoint createSenaiteEndpoint;
    
    @Before
    public void setup() throws Exception {
    	loadXmlRoutesInDirectory("senaite", "create-client-to-senaite-route.xml");
    	RouteDefinition routeDefinition = camelContext.adapt(ModelCamelContext.class).getRouteDefinitions().stream().filter(routeDef -> "create-client-to-senaite".equals(routeDef.getRouteId())).collect(Collectors.toList()).get(0);
    	RouteReifier.adviceWith(routeDefinition, camelContext, new AdviceWithRouteBuilder() {
    	    @Override
    	    public void configure() throws Exception {
    	    	weaveByToString("To[direct:authenticate-to-senaite]").replace().toD("mock:authenticateToSenaiteRoute");
    	    	weaveByToString("DynamicTo[{{senaite.baseUrl}}/@@API/senaite/v1/search?portal_type=Client&getClientID=${exchangeProperty.patient-id}]").replace().toD("mock:searchClientSenaiteEndpoint");
    	    	weaveByToString("DynamicTo[{{senaite.baseUrl}}/@@API/senaite/v1/create]").replace().toD("mock:createSenaiteEndpoint");
    	    }
    	});
    	
    	setupExpectations();
    	
    }
    
    @After
    public void reset() throws Exception {
    	authenticateToSenaiteRoute.reset();
    	searchClientSenaiteEndpoint.reset();
    	createSenaiteEndpoint.reset();
    }

    @Test
    public void shouldSearchAndUseExistingClientInSenaite() throws Exception {
    	// setup
    	Exchange exchange = new DefaultExchange(camelContext);
    	exchange.setProperty("patient-name-unique", "John Smitha (HCD-3000005)");
    	exchange.setProperty("patient-id", "86f0b43e-12a2-4e98-9937-6c85d8f05d65");
    	
    	// replay
    	producerTemplate.send("direct:create-client-to-senaite", exchange);
    	
    	// verify
    	authenticateToSenaiteRoute.assertExchangeReceived(0);
    	searchClientSenaiteEndpoint.assertIsSatisfied();
    	createSenaiteEndpoint.assertIsNotSatisfied();
    	
    	assertEquals("06a29e6454464466876b718466176457", exchange.getProperty("client-uid"));
    	assertEquals("/senaite/clients/client-1", exchange.getProperty("client-storage-path"));
    }
    
    @Test
    public void shouldCreateClientGivenItDoesNotExistsInSenaite() throws Exception {
    	// setup
    	Exchange exchange = new DefaultExchange(camelContext);
    	exchange.setProperty("patient-name-unique", "John Smitha 2 (HCD-3000006)");
    	exchange.setProperty("patient-id", "77a7901-0a73-4849-8c36-fc5a6ae28503");
    	
    	// replay
    	producerTemplate.send("direct:create-client-to-senaite", exchange);
    	
    	// verify
    	authenticateToSenaiteRoute.assertExchangeReceived(0);
    	searchClientSenaiteEndpoint.assertIsSatisfied();
    	createSenaiteEndpoint.assertIsSatisfied();
    	
    	assertEquals("dce9a10c32c54dabaa5da7a0a29f8ef8", exchange.getProperty("client-uid"));
    	assertEquals("/senaite/clients/client-2", exchange.getProperty("client-storage-path"));
    }
    
    private void setupExpectations() {
    	searchClientSenaiteEndpoint.whenAnyExchangeReceived(new Processor() {
			@Override
			public void process(Exchange exchange) throws Exception {
				
				if ("86f0b43e-12a2-4e98-9937-6c85d8f05d65".equals(exchange.getProperty("patient-id"))) {
					exchange.getIn().setBody(
							"{\"count\":1,\"items\":[{\"uid\":\"06a29e6454464466876b718466176457\",\"creation_date\":\"2021-11-11T13:43:36+00:00\",\"id\":\"client-1\",\"Name\":\"John Smitha 2 (HCD-3000006)\",\"parent_id\":\"clients\",\"api_url\":\"http://127.0.0.1:8088/senaite/@@API/senaite/v1/client/72499009f97847a98a1cb27456a36079\",\"author\":\"admin\",\"ClientID\":\"86e0b43e-12a2-4e98-9937-6c85d8f05d65\",\"portal_type\":\"Client\",\"language\":\"en\",\"path\":\"/senaite/clients/client-1\",\"parent_path\":\"/senaite/clients\",\"created\":\"2021-11-11T13:43:36+00:00\",\"url\":\"http://127.0.0.1:8088/clients/client-1\",\"title\":\"John Smitha (HCD-3000005)\",\"modified\":\"2021-11-11T13:43:36+00:00\",\"creators\":[\"admin\"]}],\"url\":\"http://127.0.0.1:8088/senaite/@@API/senaite/v1/create\",\"_runtime\":0.22557592391967773}");
				} else if ("77a7901-0a73-4849-8c36-fc5a6ae28503".equals(exchange.getProperty("patient-id"))){
					exchange.getIn().setBody("{\"count\":0,\"pagesize\":25,\"items\":[],\"page\":1,\"_runtime\":0.0023889541625976562,\"next\":null,\"pages\":1,\"previous\":null}");
				}
			}

		});
    	
    	createSenaiteEndpoint.expectedHeaderReceived(Exchange.HTTP_METHOD, "POST");
    	createSenaiteEndpoint.expectedBodiesReceived("{\"portal_type\":\"Client\",\"title\":\"John Smitha 2 (HCD-3000006)\",\"ClientID\":\"77a7901-0a73-4849-8c36-fc5a6ae28503\",\"parent_path\":\"/senaite/clients\"}");
    	createSenaiteEndpoint.whenAnyExchangeReceived(new Processor() {
			@Override
			public void process(Exchange exchange) throws Exception {
				exchange.getIn().setBody(
						"{\"count\":1,\"items\":[{\"uid\":\"dce9a10c32c54dabaa5da7a0a29f8ef8\",\"creation_date\":\"2021-11-11T13:43:36+00:00\",\"id\":\"client-1\",\"Name\":\"John Smitha 2 (HCD-3000006)\",\"parent_id\":\"clients\",\"api_url\":\"http://127.0.0.1:8088/senaite/@@API/senaite/v1/client/06a29e6454464466876b718466176457\",\"author\":\"admin\",\"ClientID\":\"77a7901-0a73-4849-8c36-fc5a6ae28503\",\"portal_type\":\"Client\",\"language\":\"en\",\"path\":\"/senaite/clients/client-2\",\"parent_path\":\"/senaite/clients\",\"created\":\"2021-11-11T13:43:36+00:00\",\"url\":\"http://127.0.0.1:8088/clients/client-2\",\"title\":\"John Smitha 2 (HCD-3000006)\",\"modified\":\"2021-11-11T13:43:36+00:00\",\"creators\":[\"admin\"]}],\"url\":\"http://127.0.0.1:8088/senaite/@@API/senaite/v1/create\",\"_runtime\":0.22557592391967773}");
			}

		});
        createSenaiteEndpoint.expectedPropertyReceived("patient-name-unique", "John Smitha 2 (HCD-3000006)");
    	createSenaiteEndpoint.expectedPropertyReceived("patient-id", "77a7901-0a73-4849-8c36-fc5a6ae28503");
    }

}