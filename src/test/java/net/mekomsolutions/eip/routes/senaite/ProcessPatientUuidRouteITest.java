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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Import;

@MockEndpoints
@Import({ TestConfiguration.class})
public class ProcessPatientUuidRouteITest extends BaseWatcherRouteTest {  
    
    @EndpointInject(value = "mock:selectSqlEndpoint")
    private MockEndpoint selectSqlEndpoint;
    
    @Value("${bahmni.test.orderType.uuid}")
    private String bahmniTestOrderTypeUuid;
    
    @Before
    public void setup() throws Exception {
    	loadXmlRoutesInDirectory("senaite", "process-patientUuid-route.xml");
    	RouteDefinition routeDefinition = camelContext.adapt(ModelCamelContext.class).getRouteDefinitions().stream().filter(routeDef -> "process-patientUuid".equals(routeDef.getRouteId())).collect(Collectors.toList()).get(0);
    	RouteReifier.adviceWith(routeDefinition, camelContext, new AdviceWithRouteBuilder() {
    	    @Override
    	    public void configure() throws Exception {
    	    	weaveByToString("DynamicTo[sql:SELECT uuid FROM person WHERE person_id = (SELECT t.${exchangeProperty.lookUpColumn} FROM ${exchangeProperty.event.tableName} t WHERE t.uuid = '${exchangeProperty.event.identifier}')?dataSource=openmrsDataSource]").replace().toD("mock:selectSqlEndpoint");
    	    }
    	});
    	
    	selectSqlEndpoint.whenAnyExchangeReceived(new Processor () {
			@Override
			public void process(Exchange exchange) throws Exception {
				exchange.getIn().setBody("[{\"uuid\": \"some-patient-uuid\"}]");
			}
    		
    	});
    }
    
    @After
    public void reset() throws Exception {
    	selectSqlEndpoint.reset();
    }

    @Test
    public void shouldProcessPatientUuidFromPatientIdentifierChangeEvent() throws Exception {
    	// setup
    	Event event = new Event();
    	event.setTableName("patient_identifier");
    	event.setIdentifier("some-uuid");
    	event.setOperation("u");
    	event.setPrimaryKeyId("1");
    	Exchange exchange = new DefaultExchange(camelContext);
    	exchange.setProperty("event", event);
    	selectSqlEndpoint.expectedPropertyReceived("lookUpColumn", "patient_id");
    	
    	// replay
    	producerTemplate.send("direct:process-patientUuid", exchange);
    	
    	// verify
    	selectSqlEndpoint.assertIsSatisfied();
    	
    }
    
    @Test
    public void shouldProcessPatientUuidFromPatientNameChangeEvent() throws Exception {
    	// setup
    	Event event = new Event();
    	event.setTableName("person_name");
    	event.setIdentifier("some-uuid");
    	event.setOperation("u");
    	event.setPrimaryKeyId("1");
    	Exchange exchange = new DefaultExchange(camelContext);
    	exchange.setProperty("event", event);
    	selectSqlEndpoint.expectedPropertyReceived("lookUpColumn", "person_id");
    	
    	// replay
    	producerTemplate.send("direct:process-patientUuid", exchange);
    	
    	// verify
    	selectSqlEndpoint.assertIsSatisfied();
    	
    }

}