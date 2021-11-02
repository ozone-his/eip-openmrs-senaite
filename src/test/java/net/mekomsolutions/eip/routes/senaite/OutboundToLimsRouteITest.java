package net.mekomsolutions.eip.routes.senaite;

import java.util.stream.Collectors;

import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.model.ModelCamelContext;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.reifier.RouteReifier;
import org.apache.camel.test.spring.MockEndpoints;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.eip.mysql.watcher.Event;
import org.openmrs.eip.mysql.watcher.route.BaseWatcherRouteTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

@MockEndpoints
@Import({ TestConfiguration.class})
public class OutboundToLimsRouteITest extends BaseWatcherRouteTest {  

    @EndpointInject(value = "mock:cancelOrderToSenaiteRoute")
    private MockEndpoint cancelOrderToSenaiteRoute;
    
    @EndpointInject(value = "mock:authenticateToOpenmrsRoute")
    private MockEndpoint authenticateToOpenmrs;
    
    @EndpointInject(value = "mock:openmrsFhirServiceRequestEndpoint")
    private MockEndpoint openmrsFhirServiceRequestEndpoint;
    
    @EndpointInject(value = "mock:processPatientNamesRoute")
    private MockEndpoint processPatientNamesRoute;
    
    @EndpointInject(value = "mock:processContacttNamesRoutee")
    private MockEndpoint processContacttNamesRoute;
    
    @EndpointInject(value = "mock:creatClientToSenaiteRoute")
    private MockEndpoint creatClientToSenaiteRoute;
    
    @EndpointInject(value = "mock:creatContactToSenaiteRoute")
    private MockEndpoint creatContactToSenaiteRoute;
    
    @EndpointInject(value = "mock:createAnalysisRequestToSenaite")
    private MockEndpoint createAnalysisRequestToSenaite;
    
    @EndpointInject(value = "mock:createServiceRequestTasktoOpenmrs")
    private MockEndpoint createServiceRequestTasktoOpenmrs;
    
    private int resultWaitTimeMillis = 100; 
    
    @Before
    public void setup() throws Exception {
    	loadXmlRoutesInDirectory("senaite", "outbound-toLims-route.xml");
    	RouteDefinition routeDefinition = camelContext.adapt(ModelCamelContext.class).getRouteDefinitions().stream().filter(routeDef -> "outbound-toLims".equals(routeDef.getRouteId())).collect(Collectors.toList()).get(0);
    	RouteReifier.adviceWith(routeDefinition, camelContext, new AdviceWithRouteBuilder() {
    	    @Override
    	    public void configure() throws Exception {
    	    	weaveByToString("To[direct:cancel-order-toSenaite]").replace().toD("mock:cancelOrderToSenaiteRoute");
    	    	weaveByToString("To[direct:authenticate-toOpenmrs]").replace().toD("mock:authenticateToOpenmrsRoute");
    	    	weaveByToString("DynamicTo[{{openmrs.baseUrl}}/ws/fhir2/R4/ServiceRequest/${exchangeProperty.lab-order-uuid}?throwExceptionOnFailure=false]").replace().toD("mock:openmrsFhirServiceRequestEndpoint");
    	    	weaveByToString("To[direct:process-patientNames]").replace().toD("mock:processPatientNamesRoute");
    	    	weaveByToString("To[direct:process-contactNames]").replace().toD("mock:processContacttNamesRoutee");
    	    	weaveByToString("To[direct:create-client-toSenaite]").replace().toD("mock:creatClientToSenaiteRoute");
    	    	weaveByToString("To[direct:create-contact-toSenaite]").replace().toD("mock:creatContactToSenaiteRoute");
    	    	weaveByToString("To[direct:create-analysisRequest-toSenaite]").replace().toD("mock:createAnalysisRequestToSenaite");
    	    	weaveByToString("To[direct:create-serviceRequestTask-toOpenmrs]").replace().toD("mock:createServiceRequestTasktoOpenmrs");
    	    }
    	});
    	
    	setupExpectations();
    	
    }
    
    @After
    public void reset() throws Exception {
    	openmrsFhirServiceRequestEndpoint.reset();
    }

    @Test
    public void shouldCreateSenaiteAnalysisRequestFromOpenmrsTestOrderPanelGivePatientAlreadyExistsInSenaite() throws Exception {
    	// setup
    	
    	Event event = new Event();
    	event.setTableName("test_order");
    	event.setIdentifier("eed578b7-86cb-43f5-91cd-daebdebfe6f8");
    	event.setOperation("c");
    	event.setPrimaryKeyId("1");
    	
    	// replay
    	producerTemplate.sendBody("direct:outbound-toLims", event);
    	
    	// verify
    	openmrsFhirServiceRequestEndpoint.assertIsSatisfied();
    }
    
    @Test
    public void shouldCreateSenaiteAnalysisRequestFromOpenmrsTestOrderPanelAfterCreatingPatientInSenaite() throws Exception {
    	// setup
    	
    	Event event = new Event();
    	event.setTableName("test_order");
    	event.setIdentifier("eed578b7-86cb-43f5-91cd-daebdebfe6f8");
    	event.setOperation("c");
    	event.setPrimaryKeyId("1");
    	
    	// replay
    	producerTemplate.sendBody("direct:outbound-toLims", event);
    	
    	// verify
    	openmrsFhirServiceRequestEndpoint.assertIsSatisfied();
    	
    }
    
    @Test
    public void shouldSkipRouteImplementationGivenEventIsNotFromTestOrderTable() throws Exception {
    	// setup
    	Event event = new Event();
    	event.setTableName("example_order");
    	event.setIdentifier("eed578b7-86cb-43f5-91cd-daebdebfe6f8");
    	event.setOperation("c");
    	event.setPrimaryKeyId("1");
    	
    	// replay
    	producerTemplate.sendBody("direct:outbound-toLims", event);
    	
    	// verify
    	openmrsFhirServiceRequestEndpoint.assertIsNotSatisfied();
    	
    }
    
    private void setupExpectations() {
    	openmrsFhirServiceRequestEndpoint.whenAnyExchangeReceived(new Processor () {
			@Override
			public void process(Exchange exchange) throws Exception {
				exchange.getIn().setBody("{\"resourceType\":\"ServiceRequest\",\"id\":\"eed578b7-86cb-43f5-91cd-daebdebfe6f8\",\"identifier\":[{\"use\":\"usual\",\"type\":{\"coding\":[{\"system\":\"http://terminology.hl7.org/CodeSystem/v2-0203\",\"code\":\"PLAC\",\"display\":\"Placer Identifier\"}]},\"value\":\"ORD-2\"}],\"status\":\"active\",\"intent\":\"order\",\"code\":{\"coding\":[{\"code\":\"1019AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\",\"display\":\"Complete Blood CountImported\"}]},\"subject\":{\"reference\":\"Patient/a7f86f7c-89b7-4549-ae9c-51c80286b930\",\"type\":\"Patient\",\"display\":\"John Smith (Identifier:3000001)\"},\"encounter\":{\"reference\":\"Encounter/9b30be02-e345-42d9-8949-3a55783fbfa0\",\"type\":\"Encounter\"},\"occurrencePeriod\":{\"start\":\"2021-01-29T09:49:47+00:00\",\"end\":\"2021-01-29T10:49:46+00:00\"},\"requester\":{\"reference\":\"Practitioner/1d0c6b21-60bd-11eb-afa0-0242ac18000a\",\"type\":\"Practitioner\",\"identifier\":{\"value\":\"superman\"},\"display\":\"SuperMan(Identifier:superman)\"}}");
			}
    		
    	});
    	openmrsFhirServiceRequestEndpoint.expectedHeaderReceived(Exchange.HTTP_METHOD, "GET");
    	openmrsFhirServiceRequestEndpoint.expectedPropertyReceived("lab-order-uuid", "eed578b7-86cb-43f5-91cd-daebdebfe6f8");
    	openmrsFhirServiceRequestEndpoint.setResultWaitTime(resultWaitTimeMillis);
    	
    }

}