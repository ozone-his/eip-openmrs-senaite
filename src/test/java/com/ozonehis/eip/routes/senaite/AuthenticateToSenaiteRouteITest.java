package com.ozonehis.eip.routes.senaite;

import org.apache.camel.Exchange;
import org.apache.camel.support.DefaultExchange;
import org.apache.camel.test.spring.junit5.MockEndpoints;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openmrs.eip.mysql.watcher.Event;

import static org.junit.jupiter.api.Assertions.assertEquals;

@MockEndpoints
public class AuthenticateToSenaiteRouteITest extends BaseCamelRoutesTest {

	@BeforeEach
    public void setup() throws Exception {
    	loadXmlRoutesInDirectory("camel", "authenticate-to-senaite-route.xml");
    }

	@Test
    public void shouldSetAuthorizationHeaderWithBasicAuthenticationTokenAndPreserveBodyPassedIt() {
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
