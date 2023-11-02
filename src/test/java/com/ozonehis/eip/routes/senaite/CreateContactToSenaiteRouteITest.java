package com.ozonehis.eip.routes.senaite;

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

@MockEndpoints
@Import({ TestConfiguration.class})
public class CreateContactToSenaiteRouteITest extends BaseWatcherRouteTest {  

	@EndpointInject(value = "mock:authenticateToSenaiteRoute")
    private MockEndpoint authenticateToSenaiteRoute;
    
    @EndpointInject(value = "mock:createSenaiteEndpoint")
    private MockEndpoint createSenaiteEndpoint;
    
    @EndpointInject(value = "mock:searchClientContactSenaiteEndpoint")
    private MockEndpoint searchClientContactSenaiteEndpoint;
    
    @Before
    public void setup() throws Exception {
    	loadXmlRoutesInDirectory("senaite", "create-contact-to-senaite-route.xml");
    	RouteDefinition routeDefinition = camelContext.adapt(ModelCamelContext.class).getRouteDefinitions().stream().filter(routeDef -> "create-contact-to-senaite".equals(routeDef.getRouteId())).collect(Collectors.toList()).get(0);
    	RouteReifier.adviceWith(routeDefinition, camelContext, new AdviceWithRouteBuilder() {
    	    @Override
    	    public void configure() throws Exception {
    	    	weaveByToString("To[direct:authenticate-to-senaite]").replace().toD("mock:authenticateToSenaiteRoute");
    	    	weaveByToString("DynamicTo[{{senaite.baseUrl}}/@@API/senaite/v1/create]").replace().toD("mock:createSenaiteEndpoint");
    	    	weaveByToString("DynamicTo[{{senaite.baseUrl}}/@@API/senaite/v1/search?limit=10000&depth=2&path=${exchangeProperty.client-storage-path}]").replace().toD("mock:searchClientContactSenaiteEndpoint");
    	    }
    	});
    	
    	setupExpectations();
    	
    }
    
    @Test
    public void shouldCreateContactInSenaite() throws Exception {
    	// setup
    	Exchange exchange = new DefaultExchange(camelContext);
    	exchange.setProperty("client-storage-path", "/senaite/clients/client-1");
    	exchange.setProperty("requester-given-name", "Super");
    	exchange.setProperty("requester-family-name", "Man");
    	searchClientContactSenaiteEndpoint.whenAnyExchangeReceived(new Processor() {
			@Override
			public void process(Exchange exchange) throws Exception {
				exchange.getIn().setBody(
						"{\"count\":0,\"pagesize\":10000,\"items\":[],\"page\":1,\"_runtime\":0.0013840190016845703,\"next\":null,\"pages\":1,\"previous\":null}");
			}

		});
    	
    	// replay
    	producerTemplate.send("direct:create-contact-to-senaite", exchange);
    	
    	// verify
    	authenticateToSenaiteRoute.assertExchangeReceived(0);
    	createSenaiteEndpoint.assertIsSatisfied();
    	assertEquals("Man", exchange.getProperty("requester-family-name"));
    	assertEquals("Super", exchange.getProperty("requester-given-name"));
    	assertEquals("14a20a6851754ccb882deb89b835b5a1", exchange.getProperty("client-contact-uid"));
    	
    	
    }
    
    @Test
    public void shouldUseExistingContactInSenaite() throws Exception {
    	// setup
    	Exchange exchange = new DefaultExchange(camelContext);
    	exchange.setProperty("client-storage-path", "/senaite/clients/client-1");
    	exchange.setProperty("requester-given-name", "Super");
    	exchange.setProperty("requester-family-name", "Man");
    	searchClientContactSenaiteEndpoint.whenAnyExchangeReceived(new Processor() {
			@Override
			public void process(Exchange exchange) throws Exception {
				exchange.getIn().setBody(
						"{\"count\":0,\"pagesize\":10000,\"items\":[{\"uid\":\"14a20a6851754ccb882deb89b835b5a1\",\"creation_date\":\"2021-11-11T13:11:24+00:00\",\"id\":\"contact-1\",\"parent_id\":\"client-1\",\"api_url\":\"http://127.0.0.1:8088/senaite/@@API/senaite/v1/contact/194a8608cdd6404a8b81365cee7fd877\",\"author\":\"admin\",\"portal_type\":\"Contact\",\"expires\":\"2499-12-31T00:00:00+00:00\",\"language\":\"en\",\"path\":\"/senaite/clients/client-1/contact-1\",\"title\":\"Super Man\",\"modification_date\":\"2021-11-11T13:11:25+00:00\",\"parent_path\":\"/senaite/clients/client-1\",\"effective\":\"1000-01-01T00:00:00+00:00\",\"created\":\"2021-11-11T13:11:24+00:00\",\"url\":\"http://127.0.0.1:8088/clients/client-1/contact-1\"}],\"page\":1,\"_runtime\":0.0013840190016845703,\"next\":null,\"pages\":1,\"previous\":null}");
			}

		});
    	
    	// replay
    	producerTemplate.send("direct:create-contact-to-senaite", exchange);
    	
    	// verify
    	authenticateToSenaiteRoute.assertExchangeReceived(0);
    	searchClientContactSenaiteEndpoint.assertIsSatisfied();
    	createSenaiteEndpoint.assertIsNotSatisfied();
    	assertEquals("Man", exchange.getProperty("requester-family-name"));
    	assertEquals("Super", exchange.getProperty("requester-given-name"));
    	assertEquals("14a20a6851754ccb882deb89b835b5a1", exchange.getProperty("client-contact-uid"));
    	
    	
    }
    
    private void setupExpectations() {
    	createSenaiteEndpoint.expectedHeaderReceived(Exchange.HTTP_METHOD, "POST");
    	createSenaiteEndpoint.expectedBodiesReceived("{\"portal_type\": \"Contact\",\"parent_path\": \"/senaite/clients/client-1\",\"Firstname\": \"Super\",\"Surname\": \"Man\"}");
    	createSenaiteEndpoint.whenAnyExchangeReceived(new Processor() {
			@Override
			public void process(Exchange exchange) throws Exception {
				exchange.getIn().setBody(
						"{\"count\":1,\"items\":[{\"uid\":\"14a20a6851754ccb882deb89b835b5a1\",\"creation_date\":\"2021-11-11T13:11:24+00:00\",\"id\":\"contact-1\",\"parent_id\":\"client-1\",\"api_url\":\"http://127.0.0.1:8088/senaite/@@API/senaite/v1/contact/194a8608cdd6404a8b81365cee7fd877\",\"author\":\"admin\",\"Firstname\":\"Super\",\"portal_type\":\"Contact\",\"expires\":\"2499-12-31T00:00:00+00:00\",\"language\":\"en\",\"path\":\"/senaite/clients/client-1/contact-1\",\"Fullname\":\"Super Man\",\"modification_date\":\"2021-11-11T13:11:25+00:00\",\"Surname\":\"Man\",\"parent_path\":\"/senaite/clients/client-1\",\"effective\":\"1000-01-01T00:00:00+00:00\",\"created\":\"2021-11-11T13:11:24+00:00\",\"url\":\"http://127.0.0.1:8088/clients/client-1/contact-1\",\"title\":\"Super Man\",\"EmailAddress\":null,\"creators\":[\"admin\"],\"HomePhone\":null}],\"url\":\"http://127.0.0.1:8088/senaite/@@API/senaite/v1/create\",\"_runtime\":0.2933061122894287}");
			}

		});
    	
    	createSenaiteEndpoint.expectedPropertyReceived("client-storage-path", "/senaite/clients/client-1");
    	createSenaiteEndpoint.expectedPropertyReceived("requester-given-name", "Super");
    	createSenaiteEndpoint.expectedPropertyReceived("requester-family-name", "Man");
    }

}
