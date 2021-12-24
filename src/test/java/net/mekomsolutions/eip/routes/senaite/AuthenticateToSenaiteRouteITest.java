package net.mekomsolutions.eip.routes.senaite;

import static org.junit.Assert.assertEquals;

import org.apache.camel.Exchange;
import org.apache.camel.support.DefaultExchange;
import org.apache.camel.test.spring.MockEndpoints;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.eip.mysql.watcher.Event;
import org.openmrs.eip.mysql.watcher.route.BaseWatcherRouteTest;
import org.springframework.context.annotation.Import;

@MockEndpoints
@Import({ TestConfiguration.class})
public class AuthenticateToSenaiteRouteITest extends BaseWatcherRouteTest {  

	@Before
    public void setup() throws Exception {
    	loadXmlRoutesInDirectory("senaite", "authenticate-to-senaite-route.xml");
    }

	@Test
    public void shouldSetAuthorizationHeaderWithBasicAuthenticationTokenAndPreserveBodyPassedIt() throws Exception {
    	// setup
    	Exchange exchange = new DefaultExchange(camelContext);
		
    	Event event = new Event();
    	event.setTableName("test_order");
    	event.setIdentifier("eed578b7-86cb-43f5-91cd-daebdebfe6f8");
    	event.setOperation("c");
    	event.setPrimaryKeyId("1");
    	
    	exchange.getIn().setBody(event);
    	
    	// replay
    	producerTemplate.send("direct:authenticate-to-senaite", exchange);
    	
    	// verify
    	assertEquals("Basic YWRtaW46YWRtaW4=", exchange.getIn().getHeader("Authorization"));
    	assertEquals(event, exchange.getIn().getBody());
    }

}