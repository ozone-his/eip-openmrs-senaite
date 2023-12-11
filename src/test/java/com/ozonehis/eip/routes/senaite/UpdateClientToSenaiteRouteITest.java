package com.ozonehis.eip.routes.senaite;

import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.support.DefaultExchange;
import org.apache.camel.test.spring.junit5.MockEndpoints;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openmrs.eip.mysql.watcher.route.BaseWatcherRouteTest;
import org.springframework.context.annotation.Import;

import ch.qos.logback.classic.Level;

import static org.junit.jupiter.api.Assertions.assertEquals;

@MockEndpoints
@Import({ TestConfiguration.class})
public class UpdateClientToSenaiteRouteITest extends BaseWatcherRouteTest {  
    
    @EndpointInject(value = "mock:authenticateToSenaiteRoute")
    private MockEndpoint authenticateToSenaiteRoute;
    
    @EndpointInject(value = "mock:searchClientSenaiteEndpoint")
    private MockEndpoint searchClientSenaiteEndpoint;
    
    @EndpointInject(value = "mock:updateSenaiteEndpoint")
    private MockEndpoint updateSenaiteEndpoint;
    
    @BeforeEach
    public void setup() throws Exception {
    	loadXmlRoutesInDirectory("camel", "update-client-to-senaite-route.xml");
		
    	advise("update-client-to-senaite", new AdviceWithRouteBuilder() {
    	    @Override
    	    public void configure() {
    	    	weaveByToString("To[direct:authenticate-to-senaite]").replace().toD("mock:authenticateToSenaiteRoute");
    	    	weaveByToString(".*/@@API/senaite/v1/search\\?portal_type=Client\\&getClientID=\\$\\{exchangeProperty.patient-id\\}]").replace().toD("mock:searchClientSenaiteEndpoint");
    	    	weaveByToString(".*/@@API/senaite/v1/update]").replace().toD("mock:updateSenaiteEndpoint");
    	    }
    	});
    	
    	setupExpectations();
    	
    }
    
    @AfterEach
    public void reset() throws Exception {
    	authenticateToSenaiteRoute.reset();
    	searchClientSenaiteEndpoint.reset();
    	updateSenaiteEndpoint.reset();
    }

    @Test
    public void shouldSearchAndUpdateExistingClientInSenaite() throws Exception {
    	// setup
    	Exchange exchange = new DefaultExchange(camelContext);
    	exchange.setProperty("patient-name-unique", "John Smitha Edited (HCD-3000006)");
    	exchange.setProperty("patient-id", "86f0b43e-12a2-4e98-9937-6c85d8f05d65");
    	
    	// replay
    	producerTemplate.send("direct:update-client-to-senaite", exchange);
    	
    	// verify
    	authenticateToSenaiteRoute.assertExchangeReceived(0);
    	searchClientSenaiteEndpoint.assertIsSatisfied();
    	updateSenaiteEndpoint.assertIsSatisfied();
    	
    	assertEquals("06a29e6454464466876b718466176457", exchange.getProperty("client-uid"));
    	assertEquals("John Smitha Edited (HCD-3000006)", exchange.getProperty("patient-name-unique"));
    }
    
    @Test
    public void shouldLogFailureToUpdatePatientDueToNoExistingPatientRecordInSenaite() throws Exception {
    	// setup
    	Exchange exchange = new DefaultExchange(camelContext);
    	exchange.setProperty("patient-name-unique", "Unexistent Patient (null)");
    	exchange.setProperty("patient-id", "77a7901-0a73-4849-8c36-fc5a6ae28503");
    	
    	// replay
    	producerTemplate.send("direct:update-client-to-senaite", exchange);
    	
    	// verify
    	authenticateToSenaiteRoute.assertExchangeReceived(0);
    	searchClientSenaiteEndpoint.assertIsSatisfied();
    	updateSenaiteEndpoint.assertIsNotSatisfied();
    	assertMessageLogged(Level.INFO, "Could not update patient record identified by uuid = '77a7901-0a73-4849-8c36-fc5a6ae28503' due to no existing record with same identifier in SENAITE");
    }
    
    @Test
    public void shouldLogFailureToUpdatePatientDueToMultipleExistingPatientRecordInSenaite() throws Exception {
    	// setup
    	Exchange exchange = new DefaultExchange(camelContext);
    	exchange.setProperty("patient-name-unique", "Some Patient (Some Id)");
    	exchange.setProperty("patient-id", "790177a-0a73-8c36-4849-fc285035a6ae");
    	
    	// replay
    	producerTemplate.send("direct:update-client-to-senaite", exchange);
    	
    	// verify
    	authenticateToSenaiteRoute.assertExchangeReceived(0);
    	searchClientSenaiteEndpoint.assertIsSatisfied();
    	updateSenaiteEndpoint.assertIsNotSatisfied();
    	assertMessageLogged(Level.INFO, "Could not update patient record identified by uuid = '790177a-0a73-8c36-4849-fc285035a6ae' due to multiple existing records with same identifier in SENAITE");
    }
    
    private void setupExpectations() {
    	searchClientSenaiteEndpoint.whenAnyExchangeReceived(new Processor() {
			@Override
			public void process(Exchange exchange) throws Exception {
				
				if ("86f0b43e-12a2-4e98-9937-6c85d8f05d65".equals(exchange.getProperty("patient-id"))) {
					exchange.getIn().setBody(
							"{\"count\":1,\"items\":[{\"uid\":\"06a29e6454464466876b718466176457\",\"creation_date\":\"2021-11-11T13:43:36+00:00\",\"id\":\"client-1\",\"Name\":\"John Smitha (HCD-3000006)\",\"parent_id\":\"clients\",\"api_url\":\"http://127.0.0.1:8088/senaite/@@API/senaite/v1/client/72499009f97847a98a1cb27456a36079\",\"author\":\"admin\",\"ClientID\":\"86e0b43e-12a2-4e98-9937-6c85d8f05d65\",\"portal_type\":\"Client\",\"language\":\"en\",\"path\":\"/senaite/clients/client-1\",\"parent_path\":\"/senaite/clients\",\"created\":\"2021-11-11T13:43:36+00:00\",\"url\":\"http://127.0.0.1:8088/clients/client-1\",\"title\":\"John Smitha (HCD-3000005)\",\"modified\":\"2021-11-11T13:43:36+00:00\",\"creators\":[\"admin\"]}],\"url\":\"http://127.0.0.1:8088/senaite/@@API/senaite/v1/create\",\"_runtime\":0.22557592391967773}");
				} else if ("77a7901-0a73-4849-8c36-fc5a6ae28503".equals(exchange.getProperty("patient-id"))){
					exchange.getIn().setBody("{\"count\":0,\"pagesize\":25,\"items\":[],\"page\":1,\"_runtime\":0.0023889541625976562,\"next\":null,\"pages\":1,\"previous\":null}");
				} else if ("790177a-0a73-8c36-4849-fc285035a6ae".equals(exchange.getProperty("patient-id"))){
					exchange.getIn().setBody("{\"count\":3,\"pagesize\":25,\"items\":[\"Record1\",\"Record2\",\"Record3\"],\"page\":1,\"_runtime\":0.0023889541625976562,\"next\":null,\"pages\":1,\"previous\":null}");
				}
			}

		});
    	
    	updateSenaiteEndpoint.expectedHeaderReceived(Exchange.HTTP_METHOD, "POST");
    	updateSenaiteEndpoint.expectedBodiesReceived("{\"title\":\"John Smitha Edited (HCD-3000006)\",\"uid\":\"06a29e6454464466876b718466176457\"}");
    	updateSenaiteEndpoint.expectedPropertyReceived("client-uid", "06a29e6454464466876b718466176457");
    }
}
