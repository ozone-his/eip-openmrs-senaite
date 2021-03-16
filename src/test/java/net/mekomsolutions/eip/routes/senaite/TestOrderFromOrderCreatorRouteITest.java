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
import org.junit.Before;
import org.junit.Test;
import org.openmrs.eip.mysql.watcher.Event;
import org.openmrs.eip.mysql.watcher.route.BaseWatcherRouteTest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Import;

@MockEndpoints
@Import({ TestConfiguration.class})
public class TestOrderFromOrderCreatorRouteITest extends BaseWatcherRouteTest {  

    @EndpointInject(value = "mock:labOrderEndpoint")
    private MockEndpoint labOrderEndpoint;
    
    @EndpointInject(value = "mock:sqlEndpoint")
    private MockEndpoint sqlEndpoint;
    
    @Value("${bahmni.test.orderType.uuid}")
    private String bahmniTestOrderTypeUuid;
    
    @Before
    public void setup() throws Exception {
    	loadXmlRoutesInDirectory("senaite", "test_order-from-order-creator-route.xml");
    	RouteDefinition routeDefinition = camelContext.adapt(ModelCamelContext.class).getRouteDefinitions().stream().filter(routeDef -> "test_order-from-order-creator".equals(routeDef.getRouteId())).collect(Collectors.toList()).get(0);
    	RouteReifier.adviceWith(routeDefinition, camelContext, new AdviceWithRouteBuilder() {
    	    @Override
    	    public void configure() throws Exception {

    	    	weaveByToString("DynamicTo[{{openmrs.baseUrl}}/ws/rest/v1/order/${exchangeProperty.lab-order-uuid}]").replace().toD("mock:labOrderEndpoint");;
    	    	weaveByToString("DynamicTo[sql:INSERT INTO test_order(order_id) VALUES (${exchangeProperty.lab-order-id})?dataSource=openmrsDataSource]").replace().toD("mock:sqlEndpoint");
    	    }
    	});
    	
    	labOrderEndpoint.whenAnyExchangeReceived(new Processor () {
			@Override
			public void process(Exchange exchange) throws Exception {
				exchange.getIn().setBody("{\"orderType\": {\"uuid\": \"" + bahmniTestOrderTypeUuid + "\"}}");
			}
    		
    	});
    	labOrderEndpoint.expectedHeaderReceived(Exchange.HTTP_METHOD, "GET");
    	labOrderEndpoint.expectedHeaderReceived("Authorization", "Basic c3VwZXJtYW46QWRtaW4xMjM=");
    	labOrderEndpoint.expectedPropertyReceived("lab-order-uuid", "order-uuid");
    	
    	sqlEndpoint.expectedPropertyReceived("lab-order-id", "1");
    }

    @Test
    public void shouldCreateTestOrderFromOrder() throws Exception {
    	// setup
    	Event event = new Event();
    	event.setTableName("orders");
    	event.setIdentifier("order-uuid");
    	event.setOperation("c");
    	event.setPrimaryKeyId("1");
    	
    	// replay
    	producerTemplate.sendBody("direct:test_order-from-order-creator", event);
    	
    	// verify
    	labOrderEndpoint.assertIsSatisfied();
    	sqlEndpoint.assertIsSatisfied();
    	
    }

}