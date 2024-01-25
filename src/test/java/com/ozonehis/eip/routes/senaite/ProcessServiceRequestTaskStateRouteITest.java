package com.ozonehis.eip.routes.senaite;

import org.apache.camel.Exchange;
import org.apache.camel.support.DefaultExchange;
import org.apache.camel.test.spring.junit5.MockEndpoints;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@MockEndpoints
public class ProcessServiceRequestTaskStateRouteITest extends BaseCamelRoutesTest {
    
    @BeforeEach
    public void setup() throws Exception {
    	loadXmlRoutesInDirectory("camel", "process-servicerequest-task-state-route.xml");
    }

    @Test
    public void shouldMapSampleDueAsRequested() throws Exception {
    	// setup
    	String body = "{\"count\":1,\"pagesize\":25,\"items\":[{\"SampleTypeTitle\":\"Blood\",\"getSampleTypeUID\":\"dc79e224a3f94d5c8cc151b25abff015\",\"getClientID\":\"86f0b43e-12a2-4e98-9937-6c85d8f05d65\",\"review_state\":\"sample_due\",\"TemplateTitle\":\"LAB1015 - Thyroid Function Tests(TSH-T3-T4) Template\",\"Analyses\":[{\"url\":\"http://127.0.0.1:8088/senaite/clients/client-1/BLD-0002/T4\",\"uid\":\"c4572efab08a4df2982ce61457c009de\",\"api_url\":\"http://127.0.0.1:8088/senaite/@@API/senaite/v1/analysis/c4572efab08a4df2982ce61457c009de\"},{\"url\":\"http://127.0.0.1:8088/senaite/clients/client-1/BLD-0002/T3\",\"uid\":\"ba5d02894a394a10a7b126b85c9b298f\",\"api_url\":\"http://127.0.0.1:8088/senaite/@@API/senaite/v1/analysis/ba5d02894a394a10a7b126b85c9b298f\"},{\"url\":\"http://127.0.0.1:8088/senaite/clients/client-1/BLD-0002/TSH\",\"uid\":\"24fd02f084d0421493b7801ec66a2a32\",\"api_url\":\"http://127.0.0.1:8088/senaite/@@API/senaite/v1/analysis/24fd02f084d0421493b7801ec66a2a32\"}],\"ProfilesTitle\":[\"LAB1015 - Thyroid Function Tests(TSH-T3-T4)\"],\"creators\":[\"admin\"]}],\"page\":1,\"_runtime\":0.05844402313232422,\"next\":null,\"pages\":1,\"previous\":null}";
    	
    	Exchange exchange = new DefaultExchange(camelContext);
    	exchange.getIn().setBody(body);
    	
    	// replay
    	producerTemplate.send("direct:process-servicerequest-task-state", exchange);
    	
    	// verify
    	assertEquals("requested", exchange.getProperty("service-request-transitioned-status"));
    }
    
    @Test
    public void shouldMapSampleReceivedAsAccepted() throws Exception {
    	// setup
    	String body = "{\"count\":1,\"pagesize\":25,\"items\":[{\"SampleTypeTitle\":\"Blood\",\"getSampleTypeUID\":\"dc79e224a3f94d5c8cc151b25abff015\",\"getClientID\":\"86f0b43e-12a2-4e98-9937-6c85d8f05d65\",\"review_state\":\"sample_received\",\"TemplateTitle\":\"LAB1015 - Thyroid Function Tests(TSH-T3-T4) Template\",\"Analyses\":[{\"url\":\"http://127.0.0.1:8088/senaite/clients/client-1/BLD-0002/T4\",\"uid\":\"c4572efab08a4df2982ce61457c009de\",\"api_url\":\"http://127.0.0.1:8088/senaite/@@API/senaite/v1/analysis/c4572efab08a4df2982ce61457c009de\"},{\"url\":\"http://127.0.0.1:8088/senaite/clients/client-1/BLD-0002/T3\",\"uid\":\"ba5d02894a394a10a7b126b85c9b298f\",\"api_url\":\"http://127.0.0.1:8088/senaite/@@API/senaite/v1/analysis/ba5d02894a394a10a7b126b85c9b298f\"},{\"url\":\"http://127.0.0.1:8088/senaite/clients/client-1/BLD-0002/TSH\",\"uid\":\"24fd02f084d0421493b7801ec66a2a32\",\"api_url\":\"http://127.0.0.1:8088/senaite/@@API/senaite/v1/analysis/24fd02f084d0421493b7801ec66a2a32\"}],\"ProfilesTitle\":[\"LAB1015 - Thyroid Function Tests(TSH-T3-T4)\"],\"creators\":[\"admin\"]}],\"page\":1,\"_runtime\":0.05844402313232422,\"next\":null,\"pages\":1,\"previous\":null}";
    	
    	Exchange exchange = new DefaultExchange(camelContext);
    	exchange.getIn().setBody(body);
    	
    	// replay
    	producerTemplate.send("direct:process-servicerequest-task-state", exchange);
    	
    	// verify
    	assertEquals("accepted", exchange.getProperty("service-request-transitioned-status"));
    }
    
    @Test
    public void shouldMapPublishedAsCompleted() throws Exception {
    	// setup
    	String body = "{\"count\":1,\"pagesize\":25,\"items\":[{\"SampleTypeTitle\":\"Blood\",\"getSampleTypeUID\":\"dc79e224a3f94d5c8cc151b25abff015\",\"getClientID\":\"86f0b43e-12a2-4e98-9937-6c85d8f05d65\",\"review_state\":\"published\",\"TemplateTitle\":\"LAB1015 - Thyroid Function Tests(TSH-T3-T4) Template\",\"Analyses\":[{\"url\":\"http://127.0.0.1:8088/senaite/clients/client-1/BLD-0002/T4\",\"uid\":\"c4572efab08a4df2982ce61457c009de\",\"api_url\":\"http://127.0.0.1:8088/senaite/@@API/senaite/v1/analysis/c4572efab08a4df2982ce61457c009de\"},{\"url\":\"http://127.0.0.1:8088/senaite/clients/client-1/BLD-0002/T3\",\"uid\":\"ba5d02894a394a10a7b126b85c9b298f\",\"api_url\":\"http://127.0.0.1:8088/senaite/@@API/senaite/v1/analysis/ba5d02894a394a10a7b126b85c9b298f\"},{\"url\":\"http://127.0.0.1:8088/senaite/clients/client-1/BLD-0002/TSH\",\"uid\":\"24fd02f084d0421493b7801ec66a2a32\",\"api_url\":\"http://127.0.0.1:8088/senaite/@@API/senaite/v1/analysis/24fd02f084d0421493b7801ec66a2a32\"}],\"ProfilesTitle\":[\"LAB1015 - Thyroid Function Tests(TSH-T3-T4)\"],\"creators\":[\"admin\"]}],\"page\":1,\"_runtime\":0.05844402313232422,\"next\":null,\"pages\":1,\"previous\":null}";
    	
    	Exchange exchange = new DefaultExchange(camelContext);
    	exchange.getIn().setBody(body);
    	
    	// replay
    	producerTemplate.send("direct:process-servicerequest-task-state", exchange);
    	
    	// verify
    	assertEquals("completed", exchange.getProperty("service-request-transitioned-status"));
    }
    
    @Test
    public void shouldMapCancelledAsRejected() throws Exception {
    	// setup
    	String body = "{\"count\":1,\"pagesize\":25,\"items\":[{\"SampleTypeTitle\":\"Blood\",\"getSampleTypeUID\":\"dc79e224a3f94d5c8cc151b25abff015\",\"getClientID\":\"86f0b43e-12a2-4e98-9937-6c85d8f05d65\",\"review_state\":\"cancelled\",\"TemplateTitle\":\"LAB1015 - Thyroid Function Tests(TSH-T3-T4) Template\",\"Analyses\":[{\"url\":\"http://127.0.0.1:8088/senaite/clients/client-1/BLD-0002/T4\",\"uid\":\"c4572efab08a4df2982ce61457c009de\",\"api_url\":\"http://127.0.0.1:8088/senaite/@@API/senaite/v1/analysis/c4572efab08a4df2982ce61457c009de\"},{\"url\":\"http://127.0.0.1:8088/senaite/clients/client-1/BLD-0002/T3\",\"uid\":\"ba5d02894a394a10a7b126b85c9b298f\",\"api_url\":\"http://127.0.0.1:8088/senaite/@@API/senaite/v1/analysis/ba5d02894a394a10a7b126b85c9b298f\"},{\"url\":\"http://127.0.0.1:8088/senaite/clients/client-1/BLD-0002/TSH\",\"uid\":\"24fd02f084d0421493b7801ec66a2a32\",\"api_url\":\"http://127.0.0.1:8088/senaite/@@API/senaite/v1/analysis/24fd02f084d0421493b7801ec66a2a32\"}],\"ProfilesTitle\":[\"LAB1015 - Thyroid Function Tests(TSH-T3-T4)\"],\"creators\":[\"admin\"]}],\"page\":1,\"_runtime\":0.05844402313232422,\"next\":null,\"pages\":1,\"previous\":null}";
    	
    	Exchange exchange = new DefaultExchange(camelContext);
    	exchange.getIn().setBody(body);
    	
    	// replay
    	producerTemplate.send("direct:process-servicerequest-task-state", exchange);
    	
    	// verify
    	assertEquals("rejected", exchange.getProperty("service-request-transitioned-status"));
    }
}
