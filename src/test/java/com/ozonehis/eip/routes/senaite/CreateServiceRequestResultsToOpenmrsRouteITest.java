package com.ozonehis.eip.routes.senaite;

import java.util.HashMap;
import java.util.Map;
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
import org.springframework.test.context.support.TestPropertySourceUtils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

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
    	loadXmlRoutesInDirectory("senaite", "create-servicerequest-results-to-openmrs-route.xml");
    	RouteDefinition routeDefinition = camelContext.adapt(ModelCamelContext.class).getRouteDefinitions().stream().filter(routeDef -> "create-servicerequest-results-to-openmrs".equals(routeDef.getRouteId())).collect(Collectors.toList()).get(0);
    	RouteReifier.adviceWith(routeDefinition, camelContext, new AdviceWithRouteBuilder() {
    	    @Override
    	    public void configure() throws Exception {
    	    	weaveByToString("To[direct:authenticate-to-openmrs]").replace().toD("mock:authenticateToOpenmrsRoute");
    	    	weaveByToString("To[direct:authenticate-to-senaite]").replace().toD("mock:authenticateToSenaiteRoute");
    	    	weaveByToString("DynamicTo[{{openmrs.baseUrl}}/ws/rest/v1/encounter?encounterType={{results.encounterType.uuid}}&patient=${exchangeProperty.patient-uuid}&v=custom:(uuid,encounterDatetime,patient:(uuid),location:(uuid))]").replace().toD("mock:searchEncounterOpenmrsEndpoint");
    	    	weaveByToString("DynamicTo[{{openmrs.baseUrl}}/ws/rest/v1/encounter]").replace().toD("mock:createEncounterOpenmrsEndpoint");
    	    	weaveByToString("DynamicTo[${exchangeProperty.analysis-api_url}]").replace().toD("mock:fetchAnalysisRequestSenaiteEndpoint");
    	    	weaveByToString("DynamicTo[{{fhirR4.baseUrl}}/Observation?code=${exchangeProperty.service-request-concept-uuid}&subject=${exchangeProperty.patient-uuid}&encounter=${exchangeProperty.results-encounter-uuid}&date=${exchangeProperty.service-request-resultCaptureDate}]").replace().toD("mock:searchObservationOpenmrsFhirEndpoint");
    	    	weaveByToString("DynamicTo[{{openmrs.baseUrl}}/ws/rest/v1/obs]").replace().toD("mock:createObservationOpenmrsEndpoint");
    	    }
    	});
    	
    	setupExpectations();
    	
    }
    
    @After
    public void reset() throws Exception {
    	authenticateToOpenmrs.reset();
    	authenticateToSenaiteRoute.reset();
    	searchEncounterOpenmrsEndpoint.reset();
    	createEncounterOpenmrsEndpoint.reset();
    	fetchAnalysisRequestSenaiteEndpoint.reset();
    	searchObservationOpenmrsFhirEndpoint.reset();
    	createObservationOpenmrsEndpoint.reset();
    	TestPropertySourceUtils.addInlinedPropertiesToEnvironment(env, "is.integration.with.bahmniEmr=false");
    }

    @Test
    public void shouldCreateServiceRequestResultsInBahmni() throws Exception {
    	// setup
    	TestPropertySourceUtils.addInlinedPropertiesToEnvironment(env, "is.integration.with.bahmniEmr=true");
    	Exchange exchange = new DefaultExchange(camelContext);
    	exchange.setProperty("patient-uuid", "0298aa1b-7fa1-4244-93e7-c5138df63bb3");
    	exchange.setProperty("service-request-location-uuid", "833d0c66-e29a-4d31-ac13-ca9050d1bfa9");
    	exchange.setProperty("service-request-encounter-datetime", "2021-12-16T06:50:42+00:00");
    	exchange.setProperty("service-request-visit-uuid", "6caa036d-f442-4c0f-85e8-bf284f687ff8");
    	exchange.setProperty("service-request-requester", "d042597b-1d09-11ec-9616-0242ac1a000a");
    	
    	String tests = "[{\"url\":\"http://localhost:8081/senaite/clients/client-2/BLD-0003/TSH\",\"uid\":\"cbbded1109514906a4afe33be29e7df3\",\"api_url\":\"http://localhost:8081/senaite/@@API/senaite/v1/analysis/cbbded1109514906a4afe33be29e7df3\"},{\"url\":\"http://localhost:8081/senaite/clients/client-2/BLD-0003/T3\",\"uid\":\"adc7f66d99b449e095d1f4771fe88c2a\",\"api_url\":\"http://localhost:8081/senaite/@@API/senaite/v1/analysis/adc7f66d99b449e095d1f4771fe88c2a\"},{\"url\":\"http://localhost:8081/senaite/clients/client-2/BLD-0003/T4\",\"uid\":\"22a78ae92e214c0fbf821b6d682a231e\",\"api_url\":\"http://localhost:8081/senaite/@@API/senaite/v1/analysis/22a78ae92e214c0fbf821b6d682a231e\"}]";
    	TypeReference<HashMap<String, String>[]> typeRef = new TypeReference<HashMap<String, String>[]>() {};
    	ObjectMapper mapper = new ObjectMapper();
    	Map<String, String>[] testsMapArray = mapper.readValue(tests, typeRef);
    	exchange.setProperty("service-request-tests", testsMapArray);
    	
    	createObservationOpenmrsEndpoint.expectedHeaderReceived(Exchange.HTTP_METHOD, "POST");
    	createObservationOpenmrsEndpoint.expectedBodiesReceived("{\"encounter\": \"40901ffc-954f-4fa9-abc3-5edc02438708\",\"concept\":\"17e7685c-6301-4690-b676-3731974456c5\",\"order\": \"\",\"person\": \"0298aa1b-7fa1-4244-93e7-c5138df63bb3\",\"obsDatetime\": \"2021-12-16T06:50:42+00:00\",\"groupMembers\":[{\"concept\":\"17e7685c-6301-4690-b676-3731974456c5\",\"order\": \"\",\"person\": \"0298aa1b-7fa1-4244-93e7-c5138df63bb3\",\"obsDatetime\": \"2021-12-16T06:50:42+00:00\",\"groupMembers\":[{\"value\":\"3\",\"order\": \"\",\"person\": \"0298aa1b-7fa1-4244-93e7-c5138df63bb3\",\"obsDatetime\": \"2021-12-16T06:50:42+00:00\",\"concept\":\"17e7685c-6301-4690-b676-3731974456c5\"}]}]},{\"concept\":\"66099fcb-e165-4730-a608-e2f79f789b8a\",\"order\": \"\",\"person\": \"0298aa1b-7fa1-4244-93e7-c5138df63bb3\",\"obsDatetime\": \"2021-12-16T06:50:42+00:00\",\"groupMembers\":[{\"concept\":\"66099fcb-e165-4730-a608-e2f79f789b8a\",\"order\": \"\",\"person\": \"0298aa1b-7fa1-4244-93e7-c5138df63bb3\",\"obsDatetime\": \"2021-12-16T06:50:42+00:00\",\"groupMembers\":[{\"value\":\"1\",\"order\": \"\",\"person\": \"0298aa1b-7fa1-4244-93e7-c5138df63bb3\",\"obsDatetime\": \"2021-12-16T06:50:42+00:00\",\"concept\":\"66099fcb-e165-4730-a608-e2f79f789b8a\"}]}]},{\"concept\":\"dc6783bb-6af8-47a5-8938-e49f70191c24\",\"order\": \"\",\"person\": \"0298aa1b-7fa1-4244-93e7-c5138df63bb3\",\"obsDatetime\": \"2021-12-16T06:50:42+00:00\",\"groupMembers\":[{\"concept\":\"dc6783bb-6af8-47a5-8938-e49f70191c24\",\"order\": \"\",\"person\": \"0298aa1b-7fa1-4244-93e7-c5138df63bb3\",\"obsDatetime\": \"2021-12-16T06:50:42+00:00\",\"groupMembers\":[{\"value\":\"2\",\"order\": \"\",\"person\": \"0298aa1b-7fa1-4244-93e7-c5138df63bb3\",\"obsDatetime\": \"2021-12-16T06:50:42+00:00\",\"concept\":\"dc6783bb-6af8-47a5-8938-e49f70191c24\"}]}]}");
    	
    	// replay
    	producerTemplate.send("direct:create-servicerequest-results-to-openmrs", exchange);
    	
    	// verify
    	authenticateToSenaiteRoute.assertExchangeReceived(0);
    	authenticateToSenaiteRoute.assertExchangeReceived(0);
    	searchEncounterOpenmrsEndpoint.assertIsSatisfied();
    	createEncounterOpenmrsEndpoint.assertIsSatisfied();
    	fetchAnalysisRequestSenaiteEndpoint.assertIsSatisfied();
    	searchObservationOpenmrsFhirEndpoint.assertIsSatisfied();
    	createObservationOpenmrsEndpoint.assertIsSatisfied();
    }
    
    @Test
    public void shouldCreateServiceRequestResultsInOpenmrs() throws Exception {
    	// setup
    	TestPropertySourceUtils.addInlinedPropertiesToEnvironment(env, "is.integration.with.bahmniEmr=false");
    	Exchange exchange = new DefaultExchange(camelContext);
    	exchange.setProperty("patient-uuid", "0298aa1b-7fa1-4244-93e7-c5138df63bb3");
    	exchange.setProperty("service-request-location-uuid", "833d0c66-e29a-4d31-ac13-ca9050d1bfa9");
    	exchange.setProperty("service-request-encounter-datetime", "2021-12-16T06:50:42+00:00");
    	exchange.setProperty("service-request-visit-uuid", "6caa036d-f442-4c0f-85e8-bf284f687ff8");
    	exchange.setProperty("service-request-requester", "d042597b-1d09-11ec-9616-0242ac1a000a");
    	
    	String tests = "[{\"url\":\"http://localhost:8081/senaite/clients/client-2/BLD-0003/TSH\",\"uid\":\"cbbded1109514906a4afe33be29e7df3\",\"api_url\":\"http://localhost:8081/senaite/@@API/senaite/v1/analysis/cbbded1109514906a4afe33be29e7df3\"},{\"url\":\"http://localhost:8081/senaite/clients/client-2/BLD-0003/T3\",\"uid\":\"adc7f66d99b449e095d1f4771fe88c2a\",\"api_url\":\"http://localhost:8081/senaite/@@API/senaite/v1/analysis/adc7f66d99b449e095d1f4771fe88c2a\"},{\"url\":\"http://localhost:8081/senaite/clients/client-2/BLD-0003/T4\",\"uid\":\"22a78ae92e214c0fbf821b6d682a231e\",\"api_url\":\"http://localhost:8081/senaite/@@API/senaite/v1/analysis/22a78ae92e214c0fbf821b6d682a231e\"}]";
    	TypeReference<HashMap<String, String>[]> typeRef = new TypeReference<HashMap<String, String>[]>() {};
    	ObjectMapper mapper = new ObjectMapper();
    	Map<String, String>[] testsMapArray = mapper.readValue(tests, typeRef);
    	exchange.setProperty("service-request-tests", testsMapArray);
    	
    	createObservationOpenmrsEndpoint.expectedHeaderReceived(Exchange.HTTP_METHOD, "POST");
    	createObservationOpenmrsEndpoint.expectedBodiesReceived("{\"encounter\": \"40901ffc-954f-4fa9-abc3-5edc02438708\",\"value\": \"3\", \"order\": \"\", \"person\": \"0298aa1b-7fa1-4244-93e7-c5138df63bb3\", \"obsDatetime\": \"2021-12-16T06:50:42+00:00\", \"concept\": \"17e7685c-6301-4690-b676-3731974456c5\"},{\"value\": \"1\", \"order\": \"\", \"person\": \"0298aa1b-7fa1-4244-93e7-c5138df63bb3\", \"obsDatetime\": \"2021-12-16T06:50:42+00:00\", \"concept\": \"66099fcb-e165-4730-a608-e2f79f789b8a\"},{\"value\": \"2\", \"order\": \"\", \"person\": \"0298aa1b-7fa1-4244-93e7-c5138df63bb3\", \"obsDatetime\": \"2021-12-16T06:50:42+00:00\", \"concept\": \"dc6783bb-6af8-47a5-8938-e49f70191c24\"}");
    	
    	// replay
    	producerTemplate.send("direct:create-servicerequest-results-to-openmrs", exchange);
    	
    	// verify
    	authenticateToSenaiteRoute.assertExchangeReceived(0);
    	authenticateToSenaiteRoute.assertExchangeReceived(0);
    	searchEncounterOpenmrsEndpoint.assertIsSatisfied();
    	createEncounterOpenmrsEndpoint.assertIsSatisfied();
    	fetchAnalysisRequestSenaiteEndpoint.assertIsSatisfied();
    	searchObservationOpenmrsFhirEndpoint.assertIsSatisfied();
    	createObservationOpenmrsEndpoint.assertIsSatisfied();
    }
    
    private void setupExpectations() {
    	searchEncounterOpenmrsEndpoint.whenAnyExchangeReceived(new Processor () {
			@Override
			public void process(Exchange exchange) throws Exception {
				exchange.getIn().setBody("{\"results\": []}");
			}
    		
    	});
    	searchEncounterOpenmrsEndpoint.expectedHeaderReceived(Exchange.HTTP_METHOD, "GET");
    	searchEncounterOpenmrsEndpoint.expectedPropertyReceived("patient-uuid", "0298aa1b-7fa1-4244-93e7-c5138df63bb3");
    	
    	createEncounterOpenmrsEndpoint.whenAnyExchangeReceived(new Processor () {
			@Override
			public void process(Exchange exchange) throws Exception {
				exchange.getIn().setBody("{\"uuid\":\"40901ffc-954f-4fa9-abc3-5edc02438708\",\"encounterDatetime\":\"2021-12-16T06:26:52.000+0000\",\"patient\":{\"uuid\":\"0298aa1b-7fa1-4244-93e7-c5138df63bb3\"},\"location\":{\"uuid\":\"833d0c66-e29a-4d31-ac13-ca9050d1bfa9\"}}");
			}
    		
    	});
    	createEncounterOpenmrsEndpoint.expectedHeaderReceived(Exchange.HTTP_METHOD, "POST");
    	createEncounterOpenmrsEndpoint.expectedPropertyReceived("patient-uuid", "0298aa1b-7fa1-4244-93e7-c5138df63bb3");
    	
    	fetchAnalysisRequestSenaiteEndpoint.whenAnyExchangeReceived(new Processor () {
			@Override
			public void process(Exchange exchange) throws Exception {
				String resultEndPoint1 = "http://localhost:8081/senaite/@@API/senaite/v1/analysis/cbbded1109514906a4afe33be29e7df3";
				String resultEndPoint2 = "http://localhost:8081/senaite/@@API/senaite/v1/analysis/adc7f66d99b449e095d1f4771fe88c2a";
				String resultEndPoint3 = "http://localhost:8081/senaite/@@API/senaite/v1/analysis/22a78ae92e214c0fbf821b6d682a231e";
				if (resultEndPoint1.equals(exchange.getProperty("analysis-api_url"))) {
					exchange.getIn().setBody("{\"count\":1,\"pagesize\":25,\"items\":[{\"Category\":{\"url\":\"http://localhost:8081/senaite/bika_setup/bika_analysiscategories/analysiscategory-1\",\"uid\":\"6b3d881f6d14482d8b4f6d700acc0358\",\"api_url\":\"http://localhost:8081/senaite/@@API/senaite/v1/analysiscategory/6b3d881f6d14482d8b4f6d700acc0358\"},\"Department\":{\"url\":\"http://localhost:8081/senaite/bika_setup/bika_departments/department-1\",\"uid\":\"78813b4bef1b407c88fced5a5b3040ba\",\"api_url\":\"http://localhost:8081/senaite/@@API/senaite/v1/department/78813b4bef1b407c88fced5a5b3040ba\"},\"ShortTitle\":\"LAB1065 - TSH\",\"title\":\"LAB1065 - Thyroid Stimulating Hormone\",\"Precision\":2,\"parent_id\":\"BLD-0003\",\"Unit\":\"mg/l\",\"parent_url\":\"http://localhost:8081/senaite/@@API/senaite/v1/analysisrequest/c6ebbdbb58074b409fb215dc7e9fb48f\",\"Keyword\":\"TSH\",\"UpperDetectionLimit\":\"1000000000.0000000\",\"HiddenManually\":true,\"ResultOptions\":null,\"NumberOfRequiredVerifications\":-1,\"portal_type\":\"Analysis\",\"language\":\"en\",\"ExponentialFormatPrecision\":7,\"CommercialID\":null,\"parent_uid\":\"c6ebbdbb58074b409fb215dc7e9fb48f\",\"VAT\":\"14.00\",\"parent_path\":\"/senaite/clients/client-2/BLD-0003\",\"AnalysisService\":{\"url\":\"http://localhost:8081/senaite/bika_setup/bika_analysisservices/analysisservice-40\",\"uid\":\"5cf3d7b9986c4152bd2438df3dc81950\",\"api_url\":\"http://localhost:8081/senaite/@@API/senaite/v1/analysisservice/5cf3d7b9986c4152bd2438df3dc81950\"},\"modified\":\"2021-12-16T07:11:16+00:00\",\"Attachment\":{},\"DuplicateVariation\":\"10.00\",\"uid\":\"cbbded1109514906a4afe33be29e7df3\",\"BulkPrice\":\"7.50\",\"creation_date\":\"2021-12-16T06:48:51+00:00\",\"Instrument\":{},\"PointOfCapture\":\"lab\",\"id\":\"TSH\",\"ResultsRange\":null,\"ResultCaptureDate\":\"2021-12-16T06:50:42+00:00\",\"end\":null,\"api_url\":\"http://localhost:8081/senaite/@@API/senaite/v1/analysis/cbbded1109514906a4afe33be29e7df3\",\"author\":\"admin\",\"Price\":\"10.00\",\"ResultOptionsType\":\"select\",\"AllowManualDetectionLimit\":null,\"description\":\"LAB1065 - Thyroid Stimulating Hormone(17e7685c-6301-4690-b676-3731974456c5)\",\"tags\":[],\"expires\":\"2499-12-31T00:00:00+00:00\",\"path\":\"/senaite/clients/client-2/BLD-0003/TSH\",\"LowerDetectionLimit\":\"0.0000000\",\"MaxTimeAllowed\":{\"hours\":2,\"minutes\":0,\"days\":0},\"InstrumentEntryOfResults\":null,\"modification_date\":\"2021-12-16T07:11:16+00:00\",\"effective\":\"1000-01-01T00:00:00+00:00\",\"created\":\"2021-12-16T06:48:51+00:00\",\"url\":\"http://localhost:8081/clients/client-2/BLD-0003/TSH\",\"ManualEntryOfResults\":true,\"Result\":\"3\",\"creators\":[\"admin\"],\"SelfVerification\":-1}],\"page\":1,\"_runtime\":0.039296865463256836,\"next\":null,\"pages\":1,\"previous\":null}");
				} else if (resultEndPoint2.equals(exchange.getProperty("analysis-api_url"))) {
					exchange.getIn().setBody("{\"count\":1,\"pagesize\":25,\"items\":[{\"Category\":{\"url\":\"http://localhost:8081/senaite/bika_setup/bika_analysiscategories/analysiscategory-1\",\"uid\":\"6b3d881f6d14482d8b4f6d700acc0358\",\"api_url\":\"http://localhost:8081/senaite/@@API/senaite/v1/analysiscategory/6b3d881f6d14482d8b4f6d700acc0358\"},\"Department\":{\"url\":\"http://localhost:8081/senaite/bika_setup/bika_departments/department-1\",\"uid\":\"78813b4bef1b407c88fced5a5b3040ba\",\"api_url\":\"http://localhost:8081/senaite/@@API/senaite/v1/department/78813b4bef1b407c88fced5a5b3040ba\"},\"RetestOf\":{},\"ShortTitle\":\"LAB1061 - T3\",\"title\":\"LAB1061 - Triiodothyronine\",\"Precision\":2,\"parent_id\":\"BLD-0003\",\"Unit\":\"mg/l\",\"parent_url\":\"http://localhost:8081/senaite/@@API/senaite/v1/analysisrequest/c6ebbdbb58074b409fb215dc7e9fb48f\",\"Keyword\":\"T3\",\"UpperDetectionLimit\":\"1000000000.0000000\",\"HiddenManually\":true,\"ResultOptions\":null,\"NumberOfRequiredVerifications\":-1,\"portal_type\":\"Analysis\",\"language\":\"en\",\"ExponentialFormatPrecision\":7,\"parent_uid\":\"c6ebbdbb58074b409fb215dc7e9fb48f\",\"VAT\":\"14.00\",\"parent_path\":\"/senaite/clients/client-2/BLD-0003\",\"rights\":null,\"Calculation\":null,\"AnalysisService\":{\"url\":\"http://localhost:8081/senaite/bika_setup/bika_analysisservices/analysisservice-38\",\"uid\":\"13ce441890e7402d88963a02c4a3e8a8\",\"api_url\":\"http://localhost:8081/senaite/@@API/senaite/v1/analysisservice/13ce441890e7402d88963a02c4a3e8a8\"},\"modified\":\"2021-12-16T07:11:16+00:00\",\"Attachment\":{},\"DuplicateVariation\":\"10.00\",\"uid\":\"adc7f66d99b449e095d1f4771fe88c2a\",\"BulkPrice\":\"7.50\",\"creation_date\":\"2021-12-16T06:48:51+00:00\",\"PointOfCapture\":\"lab\",\"id\":\"T3\",\"ResultsRange\":null,\"ResultCaptureDate\":\"2021-12-16T06:50:42+00:00\",\"api_url\":\"http://localhost:8081/senaite/@@API/senaite/v1/analysis/adc7f66d99b449e095d1f4771fe88c2a\",\"author\":\"admin\",\"Price\":\"10.00\",\"ResultOptionsType\":\"select\",\"Remarks\":null,\"AllowManualDetectionLimit\":null,\"description\":\"LAB1061 - Triiodothyronine(66099fcb-e165-4730-a608-e2f79f789b8a)\",\"tags\":[],\"expires\":\"2499-12-31T00:00:00+00:00\",\"path\":\"/senaite/clients/client-2/BLD-0003/T3\",\"LowerDetectionLimit\":\"0.0000000\",\"MaxTimeAllowed\":{\"hours\":2,\"minutes\":0,\"days\":0},\"modification_date\":\"2021-12-16T07:11:16+00:00\",\"effective\":\"1000-01-01T00:00:00+00:00\",\"created\":\"2021-12-16T06:48:51+00:00\",\"url\":\"http://localhost:8081/clients/client-2/BLD-0003/T3\",\"ManualEntryOfResults\":true,\"Result\":\"1\",\"creators\":[\"admin\"],\"in_response_to\":null,\"SelfVerification\":-1}],\"page\":1,\"_runtime\":0.034854888916015625,\"pages\":1}");
				} else if (resultEndPoint3.equals(exchange.getProperty("analysis-api_url"))) {
					exchange.getIn().setBody("{\"count\":1,\"pagesize\":25,\"items\":[{\"Category\":{\"url\":\"http://localhost:8081/senaite/bika_setup/bika_analysiscategories/analysiscategory-1\",\"uid\":\"6b3d881f6d14482d8b4f6d700acc0358\",\"api_url\":\"http://localhost:8081/senaite/@@API/senaite/v1/analysiscategory/6b3d881f6d14482d8b4f6d700acc0358\"},\"Department\":{\"url\":\"http://localhost:8081/senaite/bika_setup/bika_departments/department-1\",\"uid\":\"78813b4bef1b407c88fced5a5b3040ba\",\"api_url\":\"http://localhost:8081/senaite/@@API/senaite/v1/department/78813b4bef1b407c88fced5a5b3040ba\"},\"ShortTitle\":\"LAB1062 - T4\",\"title\":\"LAB1062 - Thyroxine\",\"Precision\":2,\"parent_id\":\"BLD-0003\",\"Unit\":\"mg/l\",\"parent_url\":\"http://localhost:8081/senaite/@@API/senaite/v1/analysisrequest/c6ebbdbb58074b409fb215dc7e9fb48f\",\"Keyword\":\"T4\",\"UpperDetectionLimit\":\"1000000000.0000000\",\"HiddenManually\":true,\"ResultOptions\":null,\"NumberOfRequiredVerifications\":-1,\"portal_type\":\"Analysis\",\"language\":\"en\",\"ExponentialFormatPrecision\":7,\"parent_uid\":\"c6ebbdbb58074b409fb215dc7e9fb48f\",\"VAT\":\"14.00\",\"parent_path\":\"/senaite/clients/client-2/BLD-0003\",\"AnalysisService\":{\"url\":\"http://localhost:8081/senaite/bika_setup/bika_analysisservices/analysisservice-39\",\"uid\":\"cadfce404e6649888a6376134bf2f86d\",\"api_url\":\"http://localhost:8081/senaite/@@API/senaite/v1/analysisservice/cadfce404e6649888a6376134bf2f86d\"},\"modified\":\"2021-12-16T07:11:16+00:00\",\"Attachment\":{},\"DuplicateVariation\":\"10.00\",\"uid\":\"22a78ae92e214c0fbf821b6d682a231e\",\"BulkPrice\":\"7.50\",\"creation_date\":\"2021-12-16T06:48:52+00:00\",\"PointOfCapture\":\"lab\",\"id\":\"T4\",\"ResultsRange\":null,\"ResultCaptureDate\":\"2021-12-16T06:50:42+00:00\",\"api_url\":\"http://localhost:8081/senaite/@@API/senaite/v1/analysis/22a78ae92e214c0fbf821b6d682a231e\",\"author\":\"admin\",\"Price\":\"10.00\",\"ResultOptionsType\":\"select\",\"description\":\"LAB1062 - Thyroxine(dc6783bb-6af8-47a5-8938-e49f70191c24)\",\"tags\":[],\"expires\":\"2499-12-31T00:00:00+00:00\",\"path\":\"/senaite/clients/client-2/BLD-0003/T4\",\"LowerDetectionLimit\":\"0.0000000\",\"MaxTimeAllowed\":{\"hours\":2,\"minutes\":0,\"days\":0},\"modification_date\":\"2021-12-16T07:11:16+00:00\",\"effective\":\"1000-01-01T00:00:00+00:00\",\"created\":\"2021-12-16T06:48:52+00:00\",\"url\":\"http://localhost:8081/clients/client-2/BLD-0003/T4\",\"ManualEntryOfResults\":true,\"Result\":\"2\",\"creators\":[\"admin\"],\"SelfVerification\":-1}],\"page\":1,\"_runtime\":0.031529903411865234,\"pages\":1}");
				}
			}
    		
    	});
    	fetchAnalysisRequestSenaiteEndpoint.expectedHeaderReceived(Exchange.HTTP_METHOD, "GET");
    	fetchAnalysisRequestSenaiteEndpoint.expectedPropertyReceived("patient-uuid", "0298aa1b-7fa1-4244-93e7-c5138df63bb3");
    	
    	searchObservationOpenmrsFhirEndpoint.whenAnyExchangeReceived(new Processor () {
			@Override
			public void process(Exchange exchange) throws Exception {
				exchange.getIn().setBody("{\"resourceType\":\"Bundle\",\"id\":\"e2131270-9d4b-49f7-b310-e28fb919682c\",\"meta\":{\"lastUpdated\":\"2021-12-16T08:11:02.232+00:00\"},\"type\":\"searchset\",\"total\":0,\"link\":[{\"relation\":\"self\",\"url\":\"http://openmrs:8080/openmrs/ws/fhir2/R4/Observation?code=ab3b5775-7080-4cb1-8be5-54e367940144&date=2021-12-16T06%3A50%3A42%2000%3A00&encounter=40901ffc-954f-4fa9-abc3-5edc02438708&subject=0298aa1b-7fa1-4244-93e7-c5138df63bb3\"}]}");
			}
    		
    	});
    	searchObservationOpenmrsFhirEndpoint.expectedHeaderReceived(Exchange.HTTP_METHOD, "GET");
    	searchObservationOpenmrsFhirEndpoint.expectedPropertyReceived("patient-uuid", "0298aa1b-7fa1-4244-93e7-c5138df63bb3");
    }
}
