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
import org.openmrs.eip.mysql.watcher.route.BaseWatcherRouteTest;
import org.springframework.context.annotation.Import;

@MockEndpoints
@Import({ TestConfiguration.class})
public class CreateAnalysisRequestToSenaiteRouteITest extends BaseWatcherRouteTest {  

    @EndpointInject(value = "mock:authenticateToSenaiteRoute")
    private MockEndpoint authenticateToSenaiteRoute;
    
    @EndpointInject(value = "mock:searchAnalysisRequestSenaiteEndpoint")
    private MockEndpoint searchAnalysisRequestSenaiteEndpoint;
    
    @EndpointInject(value = "mock:searchAnalysisRequestTemplateSenaiteEndpoint")
    private MockEndpoint searchAnalysisRequestTemplateSenaiteEndpoint;
    
    @EndpointInject(value = "mock:createAnalysisRequestSenaiteEndpoint")
    private MockEndpoint createAnalysisRequestSenaiteEndpoint;
    
    @Before
    public void setup() throws Exception {
    	loadXmlRoutesInDirectory("senaite", "create-analysisRequest-to-senaite-route.xml");
    	RouteDefinition routeDefinition = camelContext.adapt(ModelCamelContext.class).getRouteDefinitions().stream().filter(routeDef -> "create-analysisRequest-to-senaite".equals(routeDef.getRouteId())).collect(Collectors.toList()).get(0);
    	RouteReifier.adviceWith(routeDefinition, camelContext, new AdviceWithRouteBuilder() {
    	    @Override
    	    public void configure() throws Exception {
    	    	weaveByToString("To[direct:authenticate-to-senaite]").replace().toD("mock:authenticateToSenaiteRoute");
    	    	weaveByToString("DynamicTo[{{senaite.baseUrl}}/@@API/senaite/v1/search?getClientSampleID=${exchangeProperty.lab-order-uuid}&getClientID=${exchangeProperty.patient-id}&catalog=senaite_catalog_sample&complete=true]").replace().toD("mock:searchAnalysisRequestSenaiteEndpoint");
    	    	weaveByToString("DynamicTo[{{senaite.baseUrl}}/@@API/senaite/v1/search?complete=true&Description=${exchangeProperty.service-analysis-template}&catalog=senaite_catalog_setup&portal_type=ARTemplate]").replace().toD("mock:searchAnalysisRequestTemplateSenaiteEndpoint");
    	    	weaveByToString("DynamicTo[{{senaite.baseUrl}}/@@API/senaite/v1/AnalysisRequest/create/${exchangeProperty.client-uid}]").replace().toD("mock:createAnalysisRequestSenaiteEndpoint");
    	    }
    	});
    	
    	setupExpectations();
    	
    }
    
    @After
    public void reset() throws Exception {
    	authenticateToSenaiteRoute.reset();
    	searchAnalysisRequestSenaiteEndpoint.reset();
    	searchAnalysisRequestTemplateSenaiteEndpoint.reset();
    	createAnalysisRequestSenaiteEndpoint.reset();
    }

    @Test
    public void shouldCreateSenaiteAnalysisRequestFromOpenmrsTestOrderPanelGivePatientAlreadyExistsInSenaite() throws Exception {
    	// setup
    	Exchange exchange = new DefaultExchange(camelContext);
    	exchange.setProperty("lab-order-uuid", "8ee73df9-b80e-49d9-9fd1-8a5b6864178f");
    	exchange.setProperty("patient-id", "86f0b43e-12a2-4e98-9937-6c85d8f05d65");
    	exchange.setProperty("service-analysis-template", "ab3b5775-7080-4cb1-8be5-54e367940145");
    	
    	// replay
    	producerTemplate.send("direct:create-analysisRequest-to-senaite", exchange);
    	
    	// verify
    	authenticateToSenaiteRoute.assertExchangeReceived(0);
    	searchAnalysisRequestSenaiteEndpoint.assertIsSatisfied();
    	searchAnalysisRequestTemplateSenaiteEndpoint.assertIsSatisfied();
    	createAnalysisRequestSenaiteEndpoint.assertIsSatisfied();
    }
        
    private void setupExpectations() {
    	searchAnalysisRequestSenaiteEndpoint.whenAnyExchangeReceived(new Processor() {
			@Override
			public void process(Exchange exchange) throws Exception {
				
				if ("8ee73df9-b80e-49d9-9fd1-8a5b6864178f".equals(exchange.getProperty("lab-order-uuid")) && "86f0b43e-12a2-4e98-9937-6c85d8f05d65".equals(exchange.getProperty("patient-id"))) {
					exchange.getIn().setBody("{\"count\":0,\"pagesize\":25,\"items\":[],\"page\":1,\"_runtime\":0.0029439926147460938,\"next\":null,\"pages\":1,\"previous\":null}");
				}
			}

		});
    	searchAnalysisRequestSenaiteEndpoint.expectedHeaderReceived(Exchange.HTTP_METHOD, "GET");
    	searchAnalysisRequestSenaiteEndpoint.expectedPropertyReceived("lab-order-uuid", "8ee73df9-b80e-49d9-9fd1-8a5b6864178f");
    	searchAnalysisRequestSenaiteEndpoint.expectedPropertyReceived("patient-id", "86f0b43e-12a2-4e98-9937-6c85d8f05d65");
    	
    	searchAnalysisRequestTemplateSenaiteEndpoint.whenAnyExchangeReceived(new Processor() {
			@Override
			public void process(Exchange exchange) throws Exception {
				
				if ("ab3b5775-7080-4cb1-8be5-54e367940145".equals(exchange.getProperty("service-analysis-template"))) {
					exchange.getIn().setBody("{\"count\":1,\"pagesize\":25,\"items\":[{\"uid\":\"20d393b2ce3941a9ad0cde380f6f1c5d\",\"getCategoryTitle\":null,\"SamplePoint\":null,\"creation_date\":\"2021-11-11T07:25:17+00:00\",\"AnalysisProfile\":{\"url\":\"http://localhost:8088/senaite/bika_setup/bika_analysisprofiles/analysisprofile-6\",\"uid\":\"aa4123de879443fb8409334d205449de\",\"api_url\":\"http://localhost:8088/senaite/@@API/senaite/v1/analysisprofile/aa4123de879443fb8409334d205449de\"},\"id\":\"artemplate-6\",\"api_url\":\"http://localhost:8088/senaite/@@API/senaite/v1/artemplate/20d393b2ce3941a9ad0cde380f6f1c5d\",\"modification_date\":\"2021-11-11T07:37:47+00:00\",\"parent_path\":\"/senaite/bika_setup/bika_artemplates\",\"Analyses\":[{\"service_uid\":\"a9bec867daa242f2a8fc553efa2f96f7\",\"partition\":\"part-1\"},{\"service_uid\":\"46fdc43470f0423292ce2f1a04122822\",\"partition\":\"part-1\"},{\"service_uid\":\"d51e902bd36441418a95441b9fb1c1c6\",\"partition\":\"part-1\"}],\"parent_id\":\"bika_artemplates\",\"author\":\"admin\",\"review_state\":\"active\",\"description\":\"ab3b5775-7080-4cb1-8be5-54e367940145\",\"language\":\"en\",\"portal_type\":\"ARTemplate\",\"SampleType\":{\"url\":\"http://localhost:8088/senaite/bika_setup/bika_sampletypes/sampletype-1\",\"uid\":\"dc79e224a3f94d5c8cc151b25abff015\",\"api_url\":\"http://localhost:8088/senaite/@@API/senaite/v1/sampletype/dc79e224a3f94d5c8cc151b25abff015\"},\"path\":\"/senaite/bika_setup/bika_artemplates/artemplate-6\",\"creators\":[\"admin\"],\"effective\":\"1000-01-01T00:00:00+00:00\",\"created\":\"2021-11-11T07:25:17+00:00\",\"title\":\"LAB1015 - Thyroid Function Tests(TSH-T3-T4) Template\",\"modified\":\"2021-11-11T07:37:47+00:00\",\"sortable_title\":\"lab1015 - thyroid function ...) template\",\"Partitions\":[{\"value\":\"\",\"part_id\":\"part-1\"}]}],\"page\":1,\"_runtime\":0.054051876068115234,\"next\":null,\"pages\":1,\"previous\":null}");
				}
			}

		});
    	searchAnalysisRequestTemplateSenaiteEndpoint.expectedHeaderReceived(Exchange.HTTP_METHOD, "GET");
    	searchAnalysisRequestTemplateSenaiteEndpoint.expectedPropertyReceived("service-analysis-template", "ab3b5775-7080-4cb1-8be5-54e367940145");
    	
    	createAnalysisRequestSenaiteEndpoint.expectedHeaderReceived(Exchange.HTTP_METHOD, "POST");
    	createAnalysisRequestSenaiteEndpoint.expectedBodiesReceived("{\"Contact\": \"\",\"SampleType\": \"dc79e224a3f94d5c8cc151b25abff015\",\"DateSampled\": \"\",\"Template\": \"20d393b2ce3941a9ad0cde380f6f1c5d\",\"Profiles\": \"aa4123de879443fb8409334d205449de\",\"Analyses\": [\"a9bec867daa242f2a8fc553efa2f96f7\",\"46fdc43470f0423292ce2f1a04122822\",\"d51e902bd36441418a95441b9fb1c1c6\"],\"ClientSampleID\": \"8ee73df9-b80e-49d9-9fd1-8a5b6864178f\"}");
    }
}