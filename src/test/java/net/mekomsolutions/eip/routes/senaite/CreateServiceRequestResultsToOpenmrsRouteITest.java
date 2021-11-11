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

@MockEndpoints
@Import({ TestConfiguration.class})
public class CreateServiceRequestResultsToOpenmrsRouteITest extends BaseWatcherRouteTest {  
    
    @EndpointInject(value = "mock:authenticateToOpenmrsRoute")
    private MockEndpoint authenticateToOpenmrs;
    
    @EndpointInject(value = "mock:authenticateToSenaiteRoute")
    private MockEndpoint authenticateToSenaiteRoute;
    
    @EndpointInject(value = "mock:searchEncounterOpenmrsEndpoint")
    private MockEndpoint searchEncounterOpenmrsEndpoint;
    
    @EndpointInject(value = "mock:createEncounterOpenmrsEndpoint")
    private MockEndpoint createEncounterOpenmrsEndpoint;
    
    @EndpointInject(value = "mock:fetchAnalysisRequestSenaiteEndpoint")
    private MockEndpoint fetchAnalysisRequestSenaiteEndpoint;
    
    @EndpointInject(value = "mock:searchObservationOpenmrsFhirEndpoint")
    private MockEndpoint searchObservationOpenmrsFhirEndpoint;
    
    @EndpointInject(value = "mock:createObservationOpenmrsEndpoint")
    private MockEndpoint createObservationOpenmrsEndpoint;
    
    @Before
    public void setup() throws Exception {
    	loadXmlRoutesInDirectory("senaite", "create-serviceRequestResults-toOpenmrs-route.xml");
    	RouteDefinition routeDefinition = camelContext.adapt(ModelCamelContext.class).getRouteDefinitions().stream().filter(routeDef -> "create-serviceRequestResults-toOpenmrs".equals(routeDef.getRouteId())).collect(Collectors.toList()).get(0);
    	RouteReifier.adviceWith(routeDefinition, camelContext, new AdviceWithRouteBuilder() {
    	    @Override
    	    public void configure() throws Exception {
    	    	weaveByToString("To[direct:authenticate-toOpenmrs]").replace().toD("mock:authenticateToOpenmrsRoute");
    	    	weaveByToString("To[direct:authenticate-toSenaite]").replace().toD("mock:authenticateToSenaiteRoute");
    	    	weaveByToString("DynamicTo[{{openmrs.baseUrl}}/ws/rest/v1/encounter?encounterType={{results.encounterType.uuid}}&amp;patient=${exchangeProperty.patient-uuid}&amp;v=custom:(uuid,encounterDatetime,patient:(uuid),location:(uuid))]").replace().toD("mock:searchEncounterOpenmrsEndpoint");
    	    	weaveByToString("DynamicTo[{{openmrs.baseUrl}}/ws/rest/v1/encounter").replace().toD("mock:createEncounterOpenmrsEndpoint");
    	    	weaveByToString("DynamicTo[${exchangeProperty.analysis-api_url}]").replace().toD("mock:fetchAnalysisRequestSenaiteEndpoint");
    	    	weaveByToString("DynamicTo[{{openmrs.baseUrl}}/ws/fhir2/R4/Observation?code=${exchangeProperty.service-request-concept-uuid}&amp;subject=${exchangeProperty.patient-uuid}&amp;encounter=${exchangeProperty.results-encounter-uuid}&amp;date=${exchangeProperty.service-request-resultCaptureDate}]").replace().toD("mock:searchObservationOpenmrsFhirEndpoint");
    	    	weaveByToString("DynamicTo[{{openmrs.baseUrl}}/ws/rest/v1/obs]").replace().toD("mock:createObservationOpenmrsEndpoint");
    	    }
    	});
    	
    	setupExpectations();
    	
    }
    
    @After
    public void reset() throws Exception {
    	searchEncounterOpenmrsEndpoint.reset();
    }

    @Test
    public void shouldCreateServiceRequestResultsInOpenmrs() throws Exception {
    	
    }
    
    private void setupExpectations() {
    	searchEncounterOpenmrsEndpoint.whenAnyExchangeReceived(new Processor () {
			@Override
			public void process(Exchange exchange) throws Exception {
				exchange.getIn().setBody("{\"resourceType\":\"ServiceRequest\",\"id\":\"eed578b7-86cb-43f5-91cd-daebdebfe6f8\",\"identifier\":[{\"use\":\"usual\",\"type\":{\"coding\":[{\"system\":\"http://terminology.hl7.org/CodeSystem/v2-0203\",\"code\":\"PLAC\",\"display\":\"Placer Identifier\"}]},\"value\":\"ORD-2\"}],\"status\":\"active\",\"intent\":\"order\",\"code\":{\"coding\":[{\"code\":\"1019AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\",\"display\":\"Complete Blood CountImported\"}]},\"subject\":{\"reference\":\"Patient/a7f86f7c-89b7-4549-ae9c-51c80286b930\",\"type\":\"Patient\",\"display\":\"John Smith (Identifier:3000001)\"},\"encounter\":{\"reference\":\"Encounter/9b30be02-e345-42d9-8949-3a55783fbfa0\",\"type\":\"Encounter\"},\"occurrencePeriod\":{\"start\":\"2021-01-29T09:49:47+00:00\",\"end\":\"2021-01-29T10:49:46+00:00\"},\"requester\":{\"reference\":\"Practitioner/1d0c6b21-60bd-11eb-afa0-0242ac18000a\",\"type\":\"Practitioner\",\"identifier\":{\"value\":\"superman\"},\"display\":\"SuperMan(Identifier:superman)\"}}");
			}
    		
    	});
    	searchEncounterOpenmrsEndpoint.expectedHeaderReceived(Exchange.HTTP_METHOD, "GET");
    	searchEncounterOpenmrsEndpoint.expectedPropertyReceived("lab-order-uuid", "eed578b7-86cb-43f5-91cd-daebdebfe6f8");
    	
    }

}