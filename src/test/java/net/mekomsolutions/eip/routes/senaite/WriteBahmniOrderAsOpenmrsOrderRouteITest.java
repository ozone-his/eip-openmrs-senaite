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
public class WriteBahmniOrderAsOpenmrsOrderRouteITest extends BaseWatcherRouteTest {  

	@EndpointInject(value = "mock:authenticateToOpenmrsRoute")
    private MockEndpoint authenticateToOpenmrs;
    
    @EndpointInject(value = "mock:labOrderEndpoint")
    private MockEndpoint labOrderEndpoint;
    
    @EndpointInject(value = "mock:insertSqlEndpoint")
    private MockEndpoint insertSqlEndpoint;
    
    @EndpointInject(value = "mock:selectSqlEndpoint")
    private MockEndpoint selectSqlEndpoint;
    
    @Value("${bahmni.test.orderType.uuid}")
    private String bahmniTestOrderTypeUuid;
    
    @Before
    public void setup() throws Exception {
    	loadXmlRoutesInDirectory("senaite", "write-bahmniorder-as-openmrsorder-route.xml");
    	RouteDefinition routeDefinition = camelContext.adapt(ModelCamelContext.class).getRouteDefinitions().stream().filter(routeDef -> "write-bahmniorder-as-openmrsorder".equals(routeDef.getRouteId())).collect(Collectors.toList()).get(0);
    	RouteReifier.adviceWith(routeDefinition, camelContext, new AdviceWithRouteBuilder() {
    	    @Override
    	    public void configure() throws Exception {
    	    	weaveByToString("To[direct:authenticate-to-openmrs]").replace().toD("mock:authenticateToOpenmrsRoute");
    	    	weaveByToString("DynamicTo[{{openmrs.baseUrl}}/ws/rest/v1/order/${exchangeProperty.lab-order-uuid}]").replace().toD("mock:labOrderEndpoint");;
    	    	weaveByToString("DynamicTo[sql:INSERT INTO test_order(order_id) VALUES (${exchangeProperty.lab-order-id})?dataSource=openmrsDataSource]").replace().toD("mock:insertSqlEndpoint");
    	    	weaveByToString("DynamicTo[sql:SELECT COUNT(*) total FROM test_order WHERE order_id=${exchangeProperty.lab-order-id}?dataSource=openmrsDataSource]").replace().toD("mock:selectSqlEndpoint");
    	    }
    	});
    	
    	labOrderEndpoint.whenAnyExchangeReceived(new Processor () {
			@Override
			public void process(Exchange exchange) throws Exception {
				exchange.getIn().setBody("{\"orderType\": {\"uuid\": \"" + bahmniTestOrderTypeUuid + "\"}}");
			}
    		
    	});
    	labOrderEndpoint.expectedHeaderReceived(Exchange.HTTP_METHOD, "GET");
    	labOrderEndpoint.expectedPropertyReceived("lab-order-uuid", "order-uuid");
    	
    	insertSqlEndpoint.expectedPropertyReceived("lab-order-id", "1");
    	
    	selectSqlEndpoint.whenAnyExchangeReceived(new Processor () {
			@Override
			public void process(Exchange exchange) throws Exception {
				exchange.getIn().setBody("{\"total\": 0}");
			}
    		
    	});
    	selectSqlEndpoint.expectedPropertyReceived("lab-order-id", "1");
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
    	producerTemplate.sendBody("direct:write-bahmniorder-as-openmrsorder", event);
    	
    	// verify
    	authenticateToOpenmrs.assertExchangeReceived(0);
    	labOrderEndpoint.assertIsSatisfied();
    	insertSqlEndpoint.assertIsSatisfied();
    	selectSqlEndpoint.assertIsSatisfied();
    	
    }

}