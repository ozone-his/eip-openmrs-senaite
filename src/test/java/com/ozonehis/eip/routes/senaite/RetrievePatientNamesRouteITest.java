package com.ozonehis.eip.routes.senaite;

import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.support.DefaultExchange;
import org.apache.camel.test.spring.junit5.MockEndpoints;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@MockEndpoints
public class RetrievePatientNamesRouteITest extends BaseCamelRoutesTest {

	@EndpointInject(value = "mock:authenticateToOpenmrsRoute")
    private MockEndpoint authenticateToOpenmrs;
	
    @EndpointInject(value = "mock:patientEndpoint")
    private MockEndpoint patientEndpoint;
    
    @BeforeEach
    public void setup() throws Exception {
    	loadXmlRoutesInDirectory("camel", "retrieve-patient-names-from-openmrs-route.xml");
		
    	advise("retrieve-patient-names-from-openmrs", new AdviceWithRouteBuilder() {
    	    @Override
    	    public void configure() {
    	    	weaveByToString("To[direct:authenticate-to-openmrs]").replace().toD("mock:authenticateToOpenmrsRoute");
    	    	weaveByToString(".*/\\$\\{exchangeProperty.patient-reference\\}]").replace().toD("mock:patientEndpoint");
    	    }
    	});
    	setupExpectations();
    }
    
    @Test
    public void shouldSetFamilyAndGivenPatientNames() throws Exception {
    	// setup
    	Exchange exchange = new DefaultExchange(camelContext);
    	exchange.setProperty("patient-reference", "Patient/86f0b43e-12a2-4e98-9937-6c85d8f05d65");
    	
    	// replay
    	producerTemplate.send("direct:retrieve-patient-names-from-openmrs", exchange);
    	
    	// verify
    	authenticateToOpenmrs.assertExchangeReceived(0);
    	patientEndpoint.assertIsSatisfied();
    	assertEquals("HCD-3000005", exchange.getProperty("patient-preferred-id"));
    	assertEquals("Smith", exchange.getProperty("patient-family-name"));
    	assertEquals("John Smith (HCD-3000005)", exchange.getProperty("patient-name-unique"));
    	
    }
    
    private void setupExpectations() {
    	patientEndpoint.whenAnyExchangeReceived(
			    exchange -> exchange.getIn().setBody("{\"resourceType\":\"Patient\",\"id\":\"86f0b43e-12a2-4e98-9937-6c85d8f05d65\",\"text\":{\"status\":\"generated\",\"div\":\"<div/>\"},\"identifier\":[{\"id\":\"e9a12a95-cd97-42e9-82e8-0fba009a1441\",\"use\":\"official\",\"type\":{\"text\":\"Numéro Dossier\"},\"value\":\"HCD-3000005\"}],\"active\":true,\"name\":[{\"id\":\"a7a01f53-27f5-4ab1-ad7d-741b7daa848d\",\"family\":\"Smith\",\"given\":[\"John\"]}],\"gender\":\"male\",\"birthDate\":\"1998-11-11\",\"deceasedBoolean\":false}"));
    	patientEndpoint.expectedHeaderReceived(Exchange.HTTP_METHOD, "GET");
    	patientEndpoint.expectedPropertyReceived("patient-reference", "Patient/86f0b43e-12a2-4e98-9937-6c85d8f05d65");
    }
}
