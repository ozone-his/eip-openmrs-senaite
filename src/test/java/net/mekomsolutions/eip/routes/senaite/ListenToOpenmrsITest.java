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
import org.apache.camel.support.DefaultExchange;
import org.apache.camel.test.spring.MockEndpoints;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.eip.mysql.watcher.Event;
import org.openmrs.eip.mysql.watcher.route.BaseWatcherRouteTest;
import org.springframework.context.annotation.Import;

@MockEndpoints
@Import({ TestConfiguration.class})
public class ListenToOpenmrsITest extends BaseWatcherRouteTest {  

    @EndpointInject(value = "mock:cancelOrderToSenaiteRoute")
    private MockEndpoint cancelOrderToSenaiteRoute;
    
    @EndpointInject(value = "mock:authenticateToOpenmrsRoute")
    private MockEndpoint authenticateToOpenmrs;
    
    @EndpointInject(value = "mock:openmrsFhirServiceRequestEndpoint")
    private MockEndpoint openmrsFhirServiceRequestEndpoint;
    
    @EndpointInject(value = "mock:retrievePatientNamesRoute")
    private MockEndpoint retrievePatientNamesRoute;
    
    @EndpointInject(value = "mock:retrievePontacttNamesRoutee")
    private MockEndpoint retrievePontacttNamesRoute;
    
    @EndpointInject(value = "mock:retrievePatientId")
    private MockEndpoint retrievePatientId;

    @EndpointInject(value = "mock:createClientToSenaiteRoute")
    private MockEndpoint createClientToSenaiteRoute;
    
    @EndpointInject(value = "mock:createContactToSenaiteRoute")
    private MockEndpoint createContactToSenaiteRoute;
    
    @EndpointInject(value = "mock:createAnalysisRequestToSenaite")
    private MockEndpoint createAnalysisRequestToSenaite;
    
    @EndpointInject(value = "mock:createServiceRequestTasktoOpenmrs")
    private MockEndpoint createServiceRequestTasktoOpenmrs;
    
    private Exchange exchange;
    
    private int resultWaitTimeMillis = 100; 
    
    @Before
    public void setup() throws Exception {
    	loadXmlRoutesInDirectory("senaite", "listen-to-openmrs-route.xml");
    	RouteDefinition routeDefinition = camelContext.adapt(ModelCamelContext.class).getRouteDefinitions().stream().filter(routeDef -> "listen-to-openmrs".equals(routeDef.getRouteId())).collect(Collectors.toList()).get(0);
    	RouteReifier.adviceWith(routeDefinition, camelContext, new AdviceWithRouteBuilder() {
    	    @Override
    	    public void configure() throws Exception {
    	    	weaveByToString("To[direct:cancel-order-to-senaite]").replace().toD("mock:cancelOrderToSenaiteRoute");
    	    	weaveByToString("To[direct:authenticate-to-openmrs]").replace().toD("mock:authenticateToOpenmrsRoute");
    	    	weaveByToString("DynamicTo[{{fhirR4.baseUrl}}/ServiceRequest/${exchangeProperty.lab-order-uuid}?throwExceptionOnFailure=false]").replace().toD("mock:openmrsFhirServiceRequestEndpoint");
    	    	weaveByToString("To[direct:retrieve-patient-names-from-openmrs]").replace().toD("mock:retrievePatientNamesRoute");
    	    	weaveByToString("To[direct:retrieve-orderer-names-from-openmrs]").replace().toD("mock:retrievePontacttNamesRoutee");
                weaveByToString("To[direct:retrieve-patient-id-from-openmrs]").replace().toD("mock:retrievePatientId");
    	    	weaveByToString("To[direct:create-client-to-senaite]").replace().toD("mock:createClientToSenaiteRoute");
    	    	weaveByToString("To[direct:create-contact-to-senaite]").replace().toD("mock:createContactToSenaiteRoute");
    	    	weaveByToString("To[direct:create-analysisrequest-to-senaite]").replace().toD("mock:createAnalysisRequestToSenaite");
    	    	weaveByToString("To[direct:create-servicerequest-task-to-openmrs]").replace().toD("mock:createServiceRequestTasktoOpenmrs");
    	    }
    	});
    	
    	setupExpectations();
    	
    	exchange = new DefaultExchange(camelContext);
    }
    
    @After
    public void reset() {
    	openmrsFhirServiceRequestEndpoint.reset();
    	cancelOrderToSenaiteRoute.reset();
    	retrievePatientNamesRoute.reset();
    	retrievePontacttNamesRoute.reset();
        retrievePatientId.reset();
    	createClientToSenaiteRoute.reset();
    	createContactToSenaiteRoute.reset();
    	createAnalysisRequestToSenaite.reset();
    	createServiceRequestTasktoOpenmrs.reset();
    }

    @Test
    public void shouldCreateSenaiteAnalysisRequestFromOpenmrsTestOrderPanelGivenNewActiveOrder() throws Exception {
    	// setup
    	Event event = new Event();
    	event.setTableName("test_order");
    	event.setIdentifier("27d730cb-1c04-4ced-a2ed-ad0f18fed728");
    	event.setOperation("c");
    	event.setPrimaryKeyId("1");
    	
    	exchange.getIn().setBody(event);
    	
    	cancelOrderToSenaiteRoute.expectedBodiesReceived("27d730cb-1c04-4ced-a2ed-ad0f18fed728");
    	cancelOrderToSenaiteRoute.setResultWaitTime(resultWaitTimeMillis);
    	
    	// replay
    	producerTemplate.send("direct:listen-to-openmrs", exchange);
    	
    	// verify
    	authenticateToOpenmrs.assertExchangeReceived(0);
    	openmrsFhirServiceRequestEndpoint.assertIsSatisfied();
    	retrievePatientNamesRoute.assertIsSatisfied();         
    	retrievePontacttNamesRoute.assertIsSatisfied();        
    	createClientToSenaiteRoute.assertIsSatisfied();        
    	createContactToSenaiteRoute.assertIsSatisfied();       
    	createAnalysisRequestToSenaite.assertIsSatisfied();   
    	createServiceRequestTasktoOpenmrs.assertIsSatisfied();
    	cancelOrderToSenaiteRoute.assertIsNotSatisfied();
    }
    
    @Test
    public void shouldCancelEquivalentSenaiteAnalysisRequestGivenOpenmrsTestOrderRevoked() throws Exception {
    	// setup
    	Event event = new Event();
    	event.setTableName("test_order");
    	event.setIdentifier("e585eced-0dd5-48eb-a267-a3049ab1ee53");
    	event.setOperation("c");
    	event.setPrimaryKeyId("1");
    	
    	exchange.getIn().setBody(event);
    	
    	cancelOrderToSenaiteRoute.expectedBodiesReceived("6533a4ab-9a03-4c81-af0d-f9b3fc7a4ef3");
    	cancelOrderToSenaiteRoute.setResultWaitTime(resultWaitTimeMillis);
    	
    	// replay
    	producerTemplate.send("direct:listen-to-openmrs", exchange);
    	
    	// verify
    	authenticateToOpenmrs.assertExchangeReceived(0);
    	openmrsFhirServiceRequestEndpoint.assertIsSatisfied();
    	cancelOrderToSenaiteRoute.assertIsSatisfied();
    }
    
    @Test
    public void shouldCancelEquivalentSenaiteAnalysisRequestGivenOpenmrsTestOrderDeleted() throws Exception {
    	// setup
    	Event event = new Event();
    	event.setTableName("test_order");
    	event.setIdentifier("27d730cb-1c04-4ced-a2ed-ad0f18fed728");
    	event.setOperation("d");
    	event.setPrimaryKeyId("1");
    	
    	exchange.getIn().setBody(event);
    	
    	cancelOrderToSenaiteRoute.expectedBodiesReceived("27d730cb-1c04-4ced-a2ed-ad0f18fed728");
    	cancelOrderToSenaiteRoute.setResultWaitTime(resultWaitTimeMillis);
    	
    	// replay
    	producerTemplate.send("direct:listen-to-openmrs", exchange);
    	
    	// verify
    	authenticateToOpenmrs.assertExchangeReceived(0);
    	openmrsFhirServiceRequestEndpoint.assertIsNotSatisfied();
    	cancelOrderToSenaiteRoute.assertIsSatisfied();
    }
    
    @Test
    public void shouldCancelEquivalentSenaiteAnalysisRequestGivenOpenmrsTestOrderVoided() throws Exception {
    	// setup
    	Event event = new Event();
    	event.setTableName("orders");
    	event.setIdentifier("6533a4ab-9a03-4c81-af0d-f9b3fc7a4ef3");
    	event.setOperation("u");
    	event.setPrimaryKeyId("1");
    	
    	exchange.getIn().setBody(event);
    	
    	cancelOrderToSenaiteRoute.expectedBodiesReceived("6533a4ab-9a03-4c81-af0d-f9b3fc7a4ef3");
    	cancelOrderToSenaiteRoute.setResultWaitTime(resultWaitTimeMillis);
    	
    	// replay
    	producerTemplate.send("direct:listen-to-openmrs", exchange);
    	
    	// verify
    	authenticateToOpenmrs.assertExchangeReceived(0);
    	openmrsFhirServiceRequestEndpoint.assertIsSatisfied();
    	cancelOrderToSenaiteRoute.assertIsSatisfied();
    }
    
    private void setupExpectations() {
    	openmrsFhirServiceRequestEndpoint.whenAnyExchangeReceived(new Processor () {
			@Override
			public void process(Exchange exchange) throws Exception {
				if ("e585eced-0dd5-48eb-a267-a3049ab1ee53".equals(exchange.getProperty("lab-order-uuid"))) {
					exchange.getIn().setBody("{\"resourceType\":\"ServiceRequest\",\"id\":\"e585eced-0dd5-48eb-a267-a3049ab1ee53\",\"text\":{\"status\":\"generated\",\"div\":\"<div></div>\"},\"replaces\":[{\"reference\":\"ServiceRequest/6533a4ab-9a03-4c81-af0d-f9b3fc7a4ef3\",\"type\":\"ServiceRequest\",\"identifier\":{\"use\":\"usual\",\"type\":{\"coding\":[{\"system\":\"http://terminology.hl7.org/CodeSystem/v2-0203\",\"code\":\"PLAC\",\"display\":\"Placer Identifier\"}]},\"value\":\"ORD-58\"}}],\"status\":\"revoked\",\"intent\":\"order\",\"code\":{\"coding\":[{\"code\":\"ab3b5775-7080-4cb1-8be5-54e367940145\",\"display\":\"LAB1015 - Thyroid Function Tests\"}]},\"subject\":{\"reference\":\"Patient/0298aa1b-7fa1-4244-93e7-c5138df63bb3\",\"type\":\"Patient\",\"display\":\"Johnson Smith (Numéro Dossier: HCD-3000013)\"},\"encounter\":{\"reference\":\"Encounter/e1c36ec4-1af1-40cb-ba79-ac810e5567c5\",\"type\":\"Encounter\"},\"occurrencePeriod\":{\"start\":\"2021-11-29T15:50:44+00:00\",\"end\":\"2021-11-29T15:50:44+00:00\"},\"requester\":{\"reference\":\"Practitioner/d042d719-1d09-11ec-9616-0242ac1a000a\",\"type\":\"Practitioner\",\"identifier\":{\"value\":\"superman\"},\"display\":\"Super Man (Identifier: superman)\"}}");
				} else if ("27d730cb-1c04-4ced-a2ed-ad0f18fed728".equals(exchange.getProperty("lab-order-uuid"))) {
					exchange.getIn().setBody("{\"resourceType\":\"ServiceRequest\",\"id\":\"27d730cb-1c04-4ced-a2ed-ad0f18fed728\",\"text\":{\"status\":\"generated\",\"div\":\"<div></div>\"},\"status\":\"active\",\"intent\":\"order\",\"code\":{\"coding\":[{\"code\":\"ab3b5775-7080-4cb1-8be5-54e367940145\",\"display\":\"LAB1015 - Thyroid Function Tests\"}]},\"subject\":{\"reference\":\"Patient/0298aa1b-7fa1-4244-93e7-c5138df63bb3\",\"type\":\"Patient\",\"display\":\"Johnson Smith (Numéro Dossier: HCD-3000013)\"},\"encounter\":{\"reference\":\"Encounter/e1c36ec4-1af1-40cb-ba79-ac810e5567c5\",\"type\":\"Encounter\"},\"occurrencePeriod\":{\"start\":\"2021-11-29T13:13:48+00:00\",\"end\":\"2021-11-29T14:13:48+00:00\"},\"requester\":{\"reference\":\"Practitioner/d042d719-1d09-11ec-9616-0242ac1a000a\",\"type\":\"Practitioner\",\"identifier\":{\"value\":\"superman\"},\"display\":\"Super Man (Identifier: superman)\"}}");
				} else if ("6533a4ab-9a03-4c81-af0d-f9b3fc7a4ef3".equals(exchange.getProperty("lab-order-uuid"))) {
					exchange.getIn().setBody("{\"resourceType\":\"OperationOutcome\",\"text\":{\"status\":\"generated\",\"div\":\"<div></div>\"},\"issue\":[{\"severity\":\"error\",\"code\":\"processing\",\"diagnostics\":\"Resource of type ServiceRequest with ID 6533a4ab-9a03-4c81-af0d-f9b3fc7a4ef3 is gone/deleted\"}]}");
				}
			}
    		
    	});
    	openmrsFhirServiceRequestEndpoint.expectedHeaderReceived(Exchange.HTTP_METHOD, "GET");
    	openmrsFhirServiceRequestEndpoint.setResultWaitTime(resultWaitTimeMillis);
    	
    	retrievePatientNamesRoute.expectedPropertyReceived("service-analysis-template", "ab3b5775-7080-4cb1-8be5-54e367940145");
    	retrievePatientNamesRoute.expectedPropertyReceived("lab-order-start-date", "2021-11-29T13:13:48+00:00");
    	retrievePatientNamesRoute.expectedPropertyReceived("patient-reference", "Patient/0298aa1b-7fa1-4244-93e7-c5138df63bb3");
    	retrievePatientNamesRoute.expectedPropertyReceived("requester-reference", "Practitioner/d042d719-1d09-11ec-9616-0242ac1a000a");
    	retrievePatientNamesRoute.setResultWaitTime(resultWaitTimeMillis);
    	
    	retrievePontacttNamesRoute.expectedPropertyReceived("service-analysis-template", "ab3b5775-7080-4cb1-8be5-54e367940145");
    	retrievePontacttNamesRoute.expectedPropertyReceived("lab-order-start-date", "2021-11-29T13:13:48+00:00");
    	retrievePontacttNamesRoute.expectedPropertyReceived("patient-reference", "Patient/0298aa1b-7fa1-4244-93e7-c5138df63bb3");
    	retrievePontacttNamesRoute.expectedPropertyReceived("requester-reference", "Practitioner/d042d719-1d09-11ec-9616-0242ac1a000a");
    	retrievePontacttNamesRoute.setResultWaitTime(resultWaitTimeMillis);
    	
        retrievePatientId.expectedPropertyReceived("patient-reference", "Patient/0298aa1b-7fa1-4244-93e7-c5138df63bb3");
        retrievePatientId.whenAnyExchangeReceived(new Processor () {
            @Override
            public void process(Exchange exchange) throws Exception {
                exchange.setProperty("patient-id", "some-unique-patient-id");
            }
            
        });
        retrievePatientId.setResultWaitTime(resultWaitTimeMillis);

    	createClientToSenaiteRoute.expectedPropertyReceived("service-analysis-template", "ab3b5775-7080-4cb1-8be5-54e367940145");
    	createClientToSenaiteRoute.expectedPropertyReceived("lab-order-start-date", "2021-11-29T13:13:48+00:00");
    	createClientToSenaiteRoute.expectedPropertyReceived("patient-reference", "Patient/0298aa1b-7fa1-4244-93e7-c5138df63bb3");
    	createClientToSenaiteRoute.expectedPropertyReceived("requester-reference", "Practitioner/d042d719-1d09-11ec-9616-0242ac1a000a");
        createClientToSenaiteRoute.expectedPropertyReceived("patient-id", "some-unique-patient-id");
    	createClientToSenaiteRoute.setResultWaitTime(resultWaitTimeMillis);
        
    	createContactToSenaiteRoute.expectedPropertyReceived("service-analysis-template", "ab3b5775-7080-4cb1-8be5-54e367940145");
    	createContactToSenaiteRoute.expectedPropertyReceived("lab-order-start-date", "2021-11-29T13:13:48+00:00");
    	createContactToSenaiteRoute.expectedPropertyReceived("patient-reference", "Patient/0298aa1b-7fa1-4244-93e7-c5138df63bb3");
    	createContactToSenaiteRoute.expectedPropertyReceived("requester-reference", "Practitioner/d042d719-1d09-11ec-9616-0242ac1a000a");
    	createContactToSenaiteRoute.setResultWaitTime(resultWaitTimeMillis);
    	
    	createAnalysisRequestToSenaite.expectedPropertyReceived("service-analysis-template", "ab3b5775-7080-4cb1-8be5-54e367940145");
    	createAnalysisRequestToSenaite.expectedPropertyReceived("lab-order-start-date", "2021-11-29T13:13:48+00:00");
    	createAnalysisRequestToSenaite.expectedPropertyReceived("patient-reference", "Patient/0298aa1b-7fa1-4244-93e7-c5138df63bb3");
    	createAnalysisRequestToSenaite.expectedPropertyReceived("requester-reference", "Practitioner/d042d719-1d09-11ec-9616-0242ac1a000a");
    	createAnalysisRequestToSenaite.setResultWaitTime(resultWaitTimeMillis);
    	
    	createServiceRequestTasktoOpenmrs.expectedPropertyReceived("service-analysis-template", "ab3b5775-7080-4cb1-8be5-54e367940145");
    	createServiceRequestTasktoOpenmrs.expectedPropertyReceived("lab-order-start-date", "2021-11-29T13:13:48+00:00");
    	createServiceRequestTasktoOpenmrs.expectedPropertyReceived("patient-reference", "Patient/0298aa1b-7fa1-4244-93e7-c5138df63bb3");
    	createServiceRequestTasktoOpenmrs.expectedPropertyReceived("requester-reference", "Practitioner/d042d719-1d09-11ec-9616-0242ac1a000a");
    	createServiceRequestTasktoOpenmrs.setResultWaitTime(resultWaitTimeMillis);
    }

}