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
public class CancelOrderToSenaiteRouteITest extends BaseWatcherRouteTest {  

    @EndpointInject(value = "mock:fetchcanceledOrderFromSenaiteEndpoint")
    private MockEndpoint fetchcanceledOrderFromSenaiteEndpoint;
    
    @EndpointInject(value = "mock:fetchActiveOrderFromSenaiteEndpoint")
    private MockEndpoint fetchActiveOrderFromSenaiteEndpoint;
    
    @EndpointInject(value = "mock:updateSenaiteWithoutThrowingEndpoint")
    private MockEndpoint updateSenaiteWithoutThrowingEndpoint;
    
    @EndpointInject(value = "mock:updateSenaiteEndpoint")
    private MockEndpoint updateSenaiteEndpoint;
    
    @EndpointInject(value = "mock:authenticateToSenaiteRoute")
    private MockEndpoint authenticateToSenaiteRoute;
    
    @Before
    public void setup() throws Exception {
    	loadXmlRoutesInDirectory("senaite", "cancel-order-toSenaite-route.xml");
    	RouteDefinition routeDefinition = camelContext.adapt(ModelCamelContext.class).getRouteDefinitions().stream().filter(routeDef -> "cancel-order-toSenaite".equals(routeDef.getRouteId())).collect(Collectors.toList()).get(0);
    	RouteReifier.adviceWith(routeDefinition, camelContext, new AdviceWithRouteBuilder() {
    	    @Override
    	    public void configure() throws Exception {
    	    	weaveByToString("DynamicTo[{{senaite.baseUrl}}/@@API/senaite/v1/search?getClientSampleID=${exchangeProperty.order-to-cancel}&catalog=bika_catalog_analysisrequest_listing&complete=true&review_state=cancelled]").replace().toD("mock:fetchcanceledOrderFromSenaiteEndpoint");
    	    	weaveByToString("DynamicTo[{{senaite.baseUrl}}/@@API/senaite/v1/search?getClientSampleID=${exchangeProperty.order-to-cancel}&catalog=bika_catalog_analysisrequest_listing&complete=true]").replace().toD("mock:fetchActiveOrderFromSenaiteEndpoint");
    	    	weaveByToString("DynamicTo[{{senaite.baseUrl}}/@@API/senaite/v1/update?throwExceptionOnFailure=false]").replace().toD("mock:updateSenaiteWithoutThrowingEndpoint");
    	    	weaveByToString("DynamicTo[{{senaite.baseUrl}}/@@API/senaite/v1/update]").replace().toD("mock:updateSenaiteEndpoint");
    	    	weaveByToString("To[direct:authenticate-toSenaite]").replace().toD("mock:authenticateToSenaiteRoute");
    	    	
    	    }
    	});
    	
    	setupExpectations();
    	
    }
    
    @After
    public void reset() throws Exception {
    	fetchcanceledOrderFromSenaiteEndpoint.reset();
    }

    @Test
    public void shouldCancelOrder() throws Exception {
    	
    }
    
    private void setupExpectations() {
    	fetchcanceledOrderFromSenaiteEndpoint.whenAnyExchangeReceived(new Processor () {
			@Override
			public void process(Exchange exchange) throws Exception {
				exchange.getIn().setBody("{\"resourceType\":\"ServiceRequest\",\"id\":\"eed578b7-86cb-43f5-91cd-daebdebfe6f8\",\"identifier\":[{\"use\":\"usual\",\"type\":{\"coding\":[{\"system\":\"http://terminology.hl7.org/CodeSystem/v2-0203\",\"code\":\"PLAC\",\"display\":\"Placer Identifier\"}]},\"value\":\"ORD-2\"}],\"status\":\"active\",\"intent\":\"order\",\"code\":{\"coding\":[{\"code\":\"1019AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\",\"display\":\"Complete Blood CountImported\"}]},\"subject\":{\"reference\":\"Patient/a7f86f7c-89b7-4549-ae9c-51c80286b930\",\"type\":\"Patient\",\"display\":\"John Smith (Identifier:3000001)\"},\"encounter\":{\"reference\":\"Encounter/9b30be02-e345-42d9-8949-3a55783fbfa0\",\"type\":\"Encounter\"},\"occurrencePeriod\":{\"start\":\"2021-01-29T09:49:47+00:00\",\"end\":\"2021-01-29T10:49:46+00:00\"},\"requester\":{\"reference\":\"Practitioner/1d0c6b21-60bd-11eb-afa0-0242ac18000a\",\"type\":\"Practitioner\",\"identifier\":{\"value\":\"superman\"},\"display\":\"SuperMan(Identifier:superman)\"}}");
			}
    		
    	});
    	fetchcanceledOrderFromSenaiteEndpoint.expectedHeaderReceived(Exchange.HTTP_METHOD, "GET");
    	fetchcanceledOrderFromSenaiteEndpoint.expectedPropertyReceived("lab-order-uuid", "eed578b7-86cb-43f5-91cd-daebdebfe6f8");
    	
    }

}