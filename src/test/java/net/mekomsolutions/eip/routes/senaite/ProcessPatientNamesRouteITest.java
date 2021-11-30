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

@MockEndpoints
@Import({ TestConfiguration.class})
public class ProcessPatientNamesRouteITest extends BaseWatcherRouteTest {  

	@EndpointInject(value = "mock:authenticateToOpenmrsRoute")
    private MockEndpoint authenticateToOpenmrs;
	
    @EndpointInject(value = "mock:patientEndpoint")
    private MockEndpoint patientEndpoint;
    
    @Before
    public void setup() throws Exception {
    	loadXmlRoutesInDirectory("senaite", "process-patientNames-route.xml");
    	RouteDefinition routeDefinition = camelContext.adapt(ModelCamelContext.class).getRouteDefinitions().stream().filter(routeDef -> "process-patientNames".equals(routeDef.getRouteId())).collect(Collectors.toList()).get(0);
    	RouteReifier.adviceWith(routeDefinition, camelContext, new AdviceWithRouteBuilder() {
    	    @Override
    	    public void configure() throws Exception {
    	    	weaveByToString("To[direct:authenticate-toOpenmrs]").replace().toD("mock:authenticateToOpenmrsRoute");
    	    	weaveByToString("DynamicTo[{{openmrs.baseUrl}}/ws/fhir2/R4/${exchangeProperty.patient-reference}]").replace().toD("mock:patientEndpoint");
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
    	producerTemplate.send("direct:process-patientNames", exchange);
    	
    	// verify
    	authenticateToOpenmrs.assertExchangeReceived(0);
    	patientEndpoint.assertIsSatisfied();
    	assertEquals("86f0b43e-12a2-4e98-9937-6c85d8f05d65", exchange.getProperty("patient-id"));
    	assertEquals("Smith", exchange.getProperty("patient-family-name"));
    	assertEquals("John Smith (86f0b43e-12a2-4e98-9937-6c85d8f05d65)", exchange.getProperty("patient-name-unique"));
    	
    }
    
    private void setupExpectations() {
    	patientEndpoint.whenAnyExchangeReceived(new Processor () {
			@Override
			public void process(Exchange exchange) throws Exception {
				exchange.getIn().setBody("{\"resourceType\":\"Patient\",\"id\":\"86f0b43e-12a2-4e98-9937-6c85d8f05d65\",\"text\":{\"status\":\"generated\",\"div\":\"<div/>\"},\"identifier\":[{\"id\":\"e9a12a95-cd97-42e9-82e8-0fba009a1441\",\"use\":\"official\",\"type\":{\"text\":\"Numéro Dossier\"},\"value\":\"HCD-3000012\"}],\"active\":true,\"name\":[{\"id\":\"a7a01f53-27f5-4ab1-ad7d-741b7daa848d\",\"family\":\"Smith\",\"given\":[\"John\"]}],\"gender\":\"male\",\"birthDate\":\"1998-11-11\",\"deceasedBoolean\":false}");
			}
    		
    	});
    	patientEndpoint.expectedHeaderReceived(Exchange.HTTP_METHOD, "GET");
    	patientEndpoint.expectedPropertyReceived("patient-reference", "Patient/86f0b43e-12a2-4e98-9937-6c85d8f05d65");
    }
}