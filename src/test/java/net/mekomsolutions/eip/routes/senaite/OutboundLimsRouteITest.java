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
public class OutboundLimsRouteITest extends BaseWatcherRouteTest {  

    @EndpointInject(value = "mock:openmrsFhirServiceRequestEndpoint")
    private MockEndpoint openmrsFhirServiceRequestEndpoint;
    
    @EndpointInject(value = "mock:openmrsFhirPatientEndpoint")
    private MockEndpoint openmrsFhirPatientEndpoint;
    
    @EndpointInject(value = "mock:openmrsFhirRequesterEndpoint")
    private MockEndpoint openmrsFhirRequesterEndpoint;
    
    @EndpointInject(value = "mock:senaiteClientSearchEndpoint")
    private MockEndpoint senaiteClientSearchEndpoint;
    
    @EndpointInject(value = "mock:senaiteCreateEndpoint")
    private MockEndpoint senaiteCreateEndpoint;
    
    @EndpointInject(value = "mock:senaiteAnalysisRequestTemplateEndpoint")
    private MockEndpoint senaiteAnalysisRequestTemplateEndpoint;
    
    @EndpointInject(value = "mock:senaiteAnalysisRequestCreationEndpoint")
    private MockEndpoint senaiteAnalysisRequestCreationEndpoint;
    
    @EndpointInject(value = "mock:openmrsFhirTaskEndpoint")
    private MockEndpoint openmrsFhirTaskEndpoint;
    
    private int resultWaitTimeMillis = 100; 
    
    @Before
    public void setup() throws Exception {
    	loadXmlRoutesInDirectory("senaite", "outbound-lims-route.xml", "openmrs-authenticate-route.xml", "senaite-authenticate-route.xml");
    	RouteDefinition routeDefinition = camelContext.adapt(ModelCamelContext.class).getRouteDefinitions().stream().filter(routeDef -> "outbound-lims".equals(routeDef.getRouteId())).collect(Collectors.toList()).get(0);
    	RouteReifier.adviceWith(routeDefinition, camelContext, new AdviceWithRouteBuilder() {
    	    @Override
    	    public void configure() throws Exception {
    	    	weaveByToString("DynamicTo[{{openmrs.baseUrl}}/ws/fhir2/R4/ServiceRequest/${exchangeProperty.lab-order-uuid}]").replace().toD("mock:openmrsFhirServiceRequestEndpoint");
    	    	weaveByToString("DynamicTo[{{openmrs.baseUrl}}/ws/fhir2/R4/${exchangeProperty.patient-reference}]").replace().toD("mock:openmrsFhirPatientEndpoint");
    	    	weaveByToString("DynamicTo[{{openmrs.baseUrl}}/ws/fhir2/R4/${exchangeProperty.requester-reference}]").replace().toD("mock:openmrsFhirRequesterEndpoint");
    	    	weaveByToString("DynamicTo[{{senaite.baseUrl}}/@@API/senaite/v1/search?portal_type=Client&getName=${exchangeProperty.patient-name-unique}]").replace().toD("mock:senaiteClientSearchEndpoint");
    	    	weaveByToString("DynamicTo[{{senaite.baseUrl}}/@@API/senaite/v1/create]").replace().toD("mock:senaiteCreateEndpoint");
    	    	weaveByToString("DynamicTo[{{senaite.baseUrl}}/@@API/senaite/v1/search?complete=true&Description=${exchangeProperty.service-analysis-template}&catalog=bika_setup_catalog&portal_type=ARTemplate]").replace().toD("mock:senaiteAnalysisRequestTemplateEndpoint");
    	    	weaveByToString("DynamicTo[{{senaite.baseUrl}}/@@API/senaite/v1/AnalysisRequest/create/${exchangeProperty.client-uid}]").replace().toD("mock:senaiteAnalysisRequestCreationEndpoint");
    	    	weaveByToString("DynamicTo[{{openmrs.baseUrl}}/ws/fhir2/R4/Task]").replace().toD("mock:openmrsFhirTaskEndpoint");
    	    }
    	});
    	
    	setupExpectations();
    	
    }
    
    @After
    public void reset() throws Exception {
    	openmrsFhirServiceRequestEndpoint.reset();
    	openmrsFhirPatientEndpoint.reset();
    	openmrsFhirRequesterEndpoint.reset();
    	senaiteClientSearchEndpoint.reset();
    	senaiteCreateEndpoint.reset();
    	senaiteAnalysisRequestTemplateEndpoint.reset();
    	senaiteAnalysisRequestCreationEndpoint.reset();
    	openmrsFhirTaskEndpoint.reset();
    }

    @Test
    public void shouldCreateSenaiteAnalysisRequestFromOpenmrsTestOrderPanelGivePatientAlreadyExistsInSenaite() throws Exception {
    	// setup
    	senaiteClientSearchEndpoint.whenAnyExchangeReceived(new Processor () {
			@Override
			public void process(Exchange exchange) throws Exception {
				exchange.getIn().setBody("{\"count\":1,\"pagesize\":25,\"items\":[{\"uid\":\"8bf244463fc142d79625b14615b78e83\",\"exclude_from_nav\":false,\"id\":\"client-2\",\"parent_id\":\"clients\",\"end\":null,\"api_url\":\"http://localhost:8088/senaite/@@API/senaite/v1/client/8bf244463fc142d79625b14615b78e83\",\"analysisRequestTemplates\":null,\"author\":\"admin\",\"is_folderish\":true,\"location\":\"\",\"parent_url\":\"http://localhost:8088/senaite/@@API/senaite/v1/clientfolder/2640659b5d3b4a17afe4ab9c4f41954f\",\"getClientID\":\"a7f86f7c-89b7-4549-ae9c-51c80286b930\",\"review_state\":\"active\",\"description\":\"\",\"tags\":[],\"portal_type\":\"Client\",\"expires\":\"2499-12-31T00:00:00+00:00\",\"path\":\"/senaite/clients/client-2\",\"parent_uid\":\"2640659b5d3b4a17afe4ab9c4f41954f\",\"parent_path\":\"/senaite/clients\",\"effective\":\"1000-01-01T00:00:00+00:00\",\"created\":\"2021-02-15T08:54:02+00:00\",\"url\":\"http://localhost:8088/senaite/clients/client-2\",\"title\":\"John Smith (a7f86f7c-89b7-4549-ae9c-51c80286b930)\",\"modified\":\"2021-02-15T08:54:02+00:00\"}],\"page\":1,\"_runtime\":0.00439906120300293,\"next\":null,\"pages\":1}");
			}
    		
    	});
    	senaiteClientSearchEndpoint.expectedPropertyReceived("patient-name-unique", "John Smith (a7f86f7c-89b7-4549-ae9c-51c80286b930)");
    	senaiteCreateEndpoint.expectedBodiesReceivedInAnyOrder("{\"portal_type\": \"Contact\",\"parent_path\": \"/senaite/clients/client-2\",\"Firstname\": \"Super\",\"Surname\": \"Man\"}");
    	
    	Event event = new Event();
    	event.setTableName("test_order");
    	event.setIdentifier("eed578b7-86cb-43f5-91cd-daebdebfe6f8");
    	event.setOperation("c");
    	event.setPrimaryKeyId("1");
    	
    	// replay
    	producerTemplate.sendBody("direct:outbound-lims", event);
    	
    	// verify
    	openmrsFhirServiceRequestEndpoint.assertIsSatisfied();
    	openmrsFhirPatientEndpoint.assertIsSatisfied();
    	openmrsFhirRequesterEndpoint.assertIsSatisfied();
    	senaiteClientSearchEndpoint.assertIsSatisfied();
    	senaiteCreateEndpoint.assertIsSatisfied();
    	senaiteAnalysisRequestTemplateEndpoint.assertIsSatisfied();
    	senaiteAnalysisRequestCreationEndpoint.assertIsSatisfied();
    	openmrsFhirTaskEndpoint.assertIsSatisfied();
    }
    
    @Test
    public void shouldCreateSenaiteAnalysisRequestFromOpenmrsTestOrderPanelAfterCreatingPatientInSenaite() throws Exception {
    	// setup
    	senaiteClientSearchEndpoint.whenAnyExchangeReceived(new Processor () {
			@Override
			public void process(Exchange exchange) throws Exception {
				exchange.getIn().setBody("{\"count\":0,\"pagesize\":25,\"items\":[],\"page\":1,\"_runtime\":0.004194021224975586,\"pages\":1}");
			}
    		
    	});
    	senaiteClientSearchEndpoint.expectedPropertyReceived("patient-name-unique", "John Smith (a7f86f7c-89b7-4549-ae9c-51c80286b930)");
    	senaiteCreateEndpoint.expectedBodiesReceivedInAnyOrder("{\"portal_type\":\"Client\",\"title\":\"John Smith (a7f86f7c-89b7-4549-ae9c-51c80286b930)\",\"ClientID\":\"a7f86f7c-89b7-4549-ae9c-51c80286b930\",\"parent_path\":\"/senaite/clients\"}","{\"portal_type\": \"Contact\",\"parent_path\": \"/senaite/clients/client-2\",\"Firstname\": \"Super\",\"Surname\": \"Man\"}");
    	
    	Event event = new Event();
    	event.setTableName("test_order");
    	event.setIdentifier("eed578b7-86cb-43f5-91cd-daebdebfe6f8");
    	event.setOperation("c");
    	event.setPrimaryKeyId("1");
    	
    	// replay
    	producerTemplate.sendBody("direct:outbound-lims", event);
    	
    	// verify
    	openmrsFhirServiceRequestEndpoint.assertIsSatisfied();
    	openmrsFhirPatientEndpoint.assertIsSatisfied();
    	openmrsFhirRequesterEndpoint.assertIsSatisfied();
    	senaiteClientSearchEndpoint.assertIsSatisfied();
    	senaiteCreateEndpoint.assertIsSatisfied();
    	senaiteAnalysisRequestTemplateEndpoint.assertIsSatisfied();
    	senaiteAnalysisRequestCreationEndpoint.assertIsSatisfied();
    	openmrsFhirTaskEndpoint.assertIsSatisfied();
    }
    
    @Test
    public void shouldSkipRouteImplementationGivenEventIsNotFromTestOrderTable() throws Exception {
    	// setup
    	Event event = new Event();
    	event.setTableName("example_order");
    	event.setIdentifier("eed578b7-86cb-43f5-91cd-daebdebfe6f8");
    	event.setOperation("c");
    	event.setPrimaryKeyId("1");
    	
    	// replay
    	producerTemplate.sendBody("direct:outbound-lims", event);
    	
    	// verify
    	openmrsFhirServiceRequestEndpoint.assertIsNotSatisfied();
    	openmrsFhirPatientEndpoint.assertIsNotSatisfied();
    	openmrsFhirRequesterEndpoint.assertIsNotSatisfied();
    	senaiteClientSearchEndpoint.assertIsNotSatisfied();
    	senaiteCreateEndpoint.assertIsNotSatisfied();
    	senaiteAnalysisRequestTemplateEndpoint.assertIsNotSatisfied();
    	senaiteAnalysisRequestCreationEndpoint.assertIsNotSatisfied();
    	openmrsFhirTaskEndpoint.assertIsNotSatisfied();
    	
    }
    
    private void setupExpectations() {
    	openmrsFhirServiceRequestEndpoint.whenAnyExchangeReceived(new Processor () {
			@Override
			public void process(Exchange exchange) throws Exception {
				exchange.getIn().setBody("{\"resourceType\":\"ServiceRequest\",\"id\":\"eed578b7-86cb-43f5-91cd-daebdebfe6f8\",\"identifier\":[{\"use\":\"usual\",\"type\":{\"coding\":[{\"system\":\"http://terminology.hl7.org/CodeSystem/v2-0203\",\"code\":\"PLAC\",\"display\":\"Placer Identifier\"}]},\"value\":\"ORD-2\"}],\"status\":\"active\",\"intent\":\"order\",\"code\":{\"coding\":[{\"code\":\"1019AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\",\"display\":\"Complete Blood CountImported\"}]},\"subject\":{\"reference\":\"Patient/a7f86f7c-89b7-4549-ae9c-51c80286b930\",\"type\":\"Patient\",\"display\":\"John Smith (Identifier:3000001)\"},\"encounter\":{\"reference\":\"Encounter/9b30be02-e345-42d9-8949-3a55783fbfa0\",\"type\":\"Encounter\"},\"occurrencePeriod\":{\"start\":\"2021-01-29T09:49:47+00:00\",\"end\":\"2021-01-29T10:49:46+00:00\"},\"requester\":{\"reference\":\"Practitioner/1d0c6b21-60bd-11eb-afa0-0242ac18000a\",\"type\":\"Practitioner\",\"identifier\":{\"value\":\"superman\"},\"display\":\"SuperMan(Identifier:superman)\"}}");
			}
    		
    	});
    	openmrsFhirServiceRequestEndpoint.expectedHeaderReceived(Exchange.HTTP_METHOD, "GET");
    	openmrsFhirServiceRequestEndpoint.expectedHeaderReceived("Authorization", "Basic c3VwZXJtYW46QWRtaW4xMjM=");
    	openmrsFhirServiceRequestEndpoint.expectedPropertyReceived("lab-order-uuid", "eed578b7-86cb-43f5-91cd-daebdebfe6f8");
    	openmrsFhirServiceRequestEndpoint.setResultWaitTime(resultWaitTimeMillis);
    	
    	openmrsFhirPatientEndpoint.whenAnyExchangeReceived(new Processor () {
			@Override
			public void process(Exchange exchange) throws Exception {
				exchange.getIn().setBody("{\"resourceType\":\"Patient\",\"id\":\"a7f86f7c-89b7-4549-ae9c-51c80286b930\",\"identifier\":[{\"id\":\"8f5b7232-ec0b-460b-8912-f595b3daa4ce\",\"use\":\"official\",\"type\":{\"text\":\"Identifier\"},\"value\":\"3000001\"}],\"active\":true,\"name\":[{\"id\":\"30d0a6b8-6500-4996-87c5-a5e8ddf5c46c\",\"family\":\"Smith\",\"given\":[\"John\"]}],\"gender\":\"male\",\"birthDate\":\"1991-01-29\",\"deceasedBoolean\":false}");
			}
    		
    	});
    	openmrsFhirPatientEndpoint.expectedHeaderReceived(Exchange.HTTP_METHOD, "GET");
    	openmrsFhirPatientEndpoint.expectedHeaderReceived("Authorization", "Basic c3VwZXJtYW46QWRtaW4xMjM=");
    	openmrsFhirPatientEndpoint.expectedPropertyReceived("patient-reference", "Patient/a7f86f7c-89b7-4549-ae9c-51c80286b930");
    	openmrsFhirPatientEndpoint.setResultWaitTime(resultWaitTimeMillis);
    	
    	openmrsFhirRequesterEndpoint.whenAnyExchangeReceived(new Processor () {
			@Override
			public void process(Exchange exchange) throws Exception {
				exchange.getIn().setBody("{\"resourceType\":\"Practitioner\",\"id\":\"1d0c6b21-60bd-11eb-afa0-0242ac18000a\",\"identifier\":[{\"system\":\"http://fhir.openmrs.org/ext/provider/identifier\",\"value\":\"superman\"}],\"active\":false,\"name\":[{\"id\":\"1d0b630f-60bd-11eb-afa0-0242ac18000a\",\"family\":\"Man\",\"given\":[\"Super\"]}],\"gender\":\"male\"}");
			}
    		
    	});
    	openmrsFhirRequesterEndpoint.expectedHeaderReceived(Exchange.HTTP_METHOD, "GET");
    	openmrsFhirRequesterEndpoint.expectedHeaderReceived("Authorization", "Basic c3VwZXJtYW46QWRtaW4xMjM=");
    	openmrsFhirRequesterEndpoint.expectedPropertyReceived("requester-reference", "Practitioner/1d0c6b21-60bd-11eb-afa0-0242ac18000a");
    	openmrsFhirRequesterEndpoint.setResultWaitTime(resultWaitTimeMillis);
    	
    	senaiteClientSearchEndpoint.expectedHeaderReceived(Exchange.HTTP_METHOD, "GET");
    	senaiteClientSearchEndpoint.expectedHeaderReceived("Authorization", "Basic YWRtaW46YWRtaW4=");
    	senaiteClientSearchEndpoint.expectedHeaderReceived("Content-Type", "application/json");
    	senaiteClientSearchEndpoint.setResultWaitTime(resultWaitTimeMillis);
    	
    	senaiteCreateEndpoint.whenAnyExchangeReceived(new Processor () {
			@Override
			public void process(Exchange exchange) throws Exception {
				if(exchange.getProperty("client-storage-path") == null) {
					exchange.getIn().setBody("{\"count\":1,\"items\":[{\"uid\":\"8bf244463fc142d79625b14615b78e83\",\"DefaultDecimalMark\":true,\"creation_date\":\"2021-02-15T09:50:54+00:00\",\"id\":\"client-2\",\"Name\":\"John Smith (a7f86f7c-89b7-4549-ae9c-51c80286b930)\",\"parent_id\":\"clients\",\"api_url\":\"http://localhost:8088/senaite/@@API/senaite/v1/client/8bf244463fc142d79625b14615b78e83\",\"author\":\"admin\",\"DecimalMark\":\".\",\"ClientID\":\"a7f86f7c-89b7-4549-ae9c-51c80286b930\",\"parent_url\":\"http://localhost:8088/senaite/@@API/senaite/v1/clientfolder/2640659b5d3b4a17afe4ab9c4f41954f\",\"tags\":[],\"portal_type\":\"Client\",\"expires\":\"2499-12-31T00:00:00+00:00\",\"language\":\"en\",\"DefaultResultsDistributionToPatients\":true,\"path\":\"/senaite/clients/client-2\",\"BulkDiscount\":null,\"parent_uid\":\"2640659b5d3b4a17afe4ab9c4f41954f\",\"modification_date\":\"2021-02-15T09:50:55+00:00\",\"parent_path\":\"/senaite/clients\",\"effective\":\"1000-01-01T00:00:00+00:00\",\"created\":\"2021-02-15T09:50:54+00:00\",\"url\":\"http://localhost:8088/clients/client-2\",\"title\":\"UniqueName(1234-5678-9)\",\"modified\":\"2021-02-15T09:50:55+00:00\",\"creators\":[\"admin\"]}],\"url\":\"http://localhost:8088/senaite/@@API/senaite/v1/create\",\"_runtime\":0.12548017501831055}");
				} else {
					exchange.getIn().setBody("{\"count\":1,\"items\":[{\"uid\":\"4467613ed7af427e8d0e5fb525106c08\",\"creation_date\":\"2021-02-15T10:07:40+00:00\",\"id\":\"contact-1\",\"parent_id\":\"client-2\",\"api_url\":\"http://localhost:8088/senaite/@@API/senaite/v1/contact/4467613ed7af427e8d0e5fb525106c08\",\"analysisRequestTemplates\":null,\"author\":\"admin\",\"Firstname\":\"Super\",\"parent_url\":\"http://localhost:8088/senaite/@@API/senaite/v1/client/623da26df9a14a728c8f6e5552ede75f\",\"tags\":[],\"portal_type\":\"Contact\",\"expires\":\"2499-12-31T00:00:00+00:00\",\"language\":\"en\",\"path\":\"/senaite/clients/client-2/contact-1\",\"Fullname\":\"RequesterFirstGiven\",\"parent_uid\":\"623da26df9a14a728c8f6e5552ede75f\",\"modification_date\":\"2021-02-15T10:07:40+00:00\",\"Surname\":\"Man\",\"parent_path\":\"/senaite/clients/client-2\",\"effective\":\"1000-01-01T00:00:00+00:00\",\"created\":\"2021-02-15T10:07:40+00:00\",\"url\":\"http://localhost:8088/clients/client-2/contact-1\",\"title\":\"RequesterFirstGiven\",\"modified\":\"2021-02-15T10:07:40+00:00\",\"creators\":[\"admin\"],\"HomePhone\":null}],\"url\":\"http://localhost:8088/senaite/@@API/senaite/v1/create\",\"_runtime\":0.08779597282409668}");
				}
			}
    		
    	});
    	senaiteCreateEndpoint.expectedHeaderReceived(Exchange.HTTP_METHOD, "POST");
    	senaiteCreateEndpoint.expectedHeaderReceived("Authorization", "Basic YWRtaW46YWRtaW4=");
    	senaiteCreateEndpoint.expectedHeaderReceived("Content-Type", "application/json");
    	senaiteCreateEndpoint.setResultWaitTime(resultWaitTimeMillis);
    	
    	senaiteAnalysisRequestTemplateEndpoint.whenAnyExchangeReceived(new Processor () {
			@Override
			public void process(Exchange exchange) throws Exception {
				exchange.getIn().setBody("{\"count\":1,\"pagesize\":25,\"items\":[{\"uid\":\"7aa1758fa4fa4a759ff3a239959724df\",\"getCategoryTitle\":null,\"SamplePoint\":null,\"creation_date\":\"2021-02-15T08:25:22+00:00\",\"AnalysisProfile\":{\"url\":\"http://localhost:8088/senaite/bika_setup/bika_analysisprofiles/analysisprofile-1\",\"uid\":\"31c0f8750acc41b9a69a814186a214f0\",\"api_url\":\"http://localhost:8088/senaite/@@API/senaite/v1/analysisprofile/31c0f8750acc41b9a69a814186a214f0\"},\"id\":\"artemplate-1\",\"api_url\":\"http://localhost:8088/senaite/@@API/senaite/v1/artemplate/7aa1758fa4fa4a759ff3a239959724df\",\"modification_date\":\"2021-02-15T08:26:46+00:00\",\"parent_path\":\"/senaite/bika_setup/bika_artemplates\",\"Composite\":null,\"Analyses\":[{\"service_uid\":\"d95d8342028d4268a6c7502569fd3b61\",\"partition\":\"part-1\"},{\"service_uid\":\"ea5839433b1a493aa1b7dbe7baef54db\",\"partition\":\"part-1\"},{\"service_uid\":\"1075242c77b54817b2bbe577c80a5909\",\"partition\":\"part-1\"},{\"service_uid\":\"002e723c7a814cc48a188f2aa9a2aac2\",\"partition\":\"part-1\"},{\"service_uid\":\"51fbe2c4559446a9aec66a89b02c7631\",\"partition\":\"part-1\"},{\"service_uid\":\"364dae23e38146fab0f58894516047b9\",\"partition\":\"part-1\"},{\"service_uid\":\"cf82a32b2c304dbd9e447bc1f7586f6e\",\"partition\":\"part-1\"},{\"service_uid\":\"84f8ec6acde04b3eb7d68194b275855d\",\"partition\":\"part-1\"},{\"service_uid\":\"368588f785944ea9a06399ca1857f0e2\",\"partition\":\"part-1\"},{\"service_uid\":\"a62387be591a451bb5d01beeb3f9dc6c\",\"partition\":\"part-1\"},{\"service_uid\":\"ff44dc2aa1ac4f8689368d59cc0c0e8a\",\"partition\":\"part-1\"}],\"parent_id\":\"bika_artemplates\",\"author\":\"admin\",\"contributors\":null,\"parent_url\":\"http://localhost:8088/senaite/@@API/senaite/v1/artemplates/efc00382eb1343b98659ad6ef7b139fb\",\"review_state\":\"active\",\"location\":null,\"SamplePointUID\":null,\"description\":\"1019AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\",\"tags\":[],\"language\":\"en\",\"portal_type\":\"ARTemplate\",\"SampleType\":{\"url\":\"http://localhost:8088/senaite/bika_setup/bika_sampletypes/sampletype-1\",\"uid\":\"9b8d423640cb4499b12d2b69b6107053\",\"api_url\":\"http://localhost:8088/senaite/@@API/senaite/v1/sampletype/9b8d423640cb4499b12d2b69b6107053\"},\"Remarks\":null,\"getKeyword\":null,\"path\":\"/senaite/bika_setup/bika_artemplates/artemplate-1\",\"AnalysisServicesSettings\":[{\"hidden\":false,\"uid\":\"d95d8342028d4268a6c7502569fd3b61\"},{\"hidden\":false,\"uid\":\"ea5839433b1a493aa1b7dbe7baef54db\"},{\"hidden\":false,\"uid\":\"1075242c77b54817b2bbe577c80a5909\"},{\"hidden\":false,\"uid\":\"002e723c7a814cc48a188f2aa9a2aac2\"},{\"hidden\":false,\"uid\":\"51fbe2c4559446a9aec66a89b02c7631\"},{\"hidden\":false,\"uid\":\"364dae23e38146fab0f58894516047b9\"},{\"hidden\":false,\"uid\":\"cf82a32b2c304dbd9e447bc1f7586f6e\"},{\"hidden\":false,\"uid\":\"84f8ec6acde04b3eb7d68194b275855d\"},{\"hidden\":false,\"uid\":\"368588f785944ea9a06399ca1857f0e2\"},{\"hidden\":false,\"uid\":\"a62387be591a451bb5d01beeb3f9dc6c\"},{\"hidden\":false,\"uid\":\"ff44dc2aa1ac4f8689368d59cc0c0e8a\"}],\"AutoPartition\":true,\"parent_uid\":\"efc00382eb1343b98659ad6ef7b139fb\",\"SamplingRequired\":null,\"getClientUID\":\"\",\"creators\":[\"admin\"],\"effective\":\"1000-01-01T00:00:00+00:00\",\"created\":\"2021-02-15T08:25:22+00:00\",\"url\":\"http://localhost:8088/senaite/bika_setup/bika_artemplates/artemplate-1\",\"title\":\"Complete Blood CountTemplate\",\"modified\":\"2021-02-15T08:26:46+00:00\",\"Partitions\":[{\"value\":\"\",\"part_id\":\"part-1\"}]}],\"page\":1,\"_runtime\":0.03587698936462402,\"pages\":1}");
			}
    		
    	});
    	senaiteAnalysisRequestTemplateEndpoint.expectedHeaderReceived(Exchange.HTTP_METHOD, "GET");
    	senaiteAnalysisRequestTemplateEndpoint.expectedHeaderReceived("Authorization", "Basic YWRtaW46YWRtaW4=");
    	senaiteAnalysisRequestTemplateEndpoint.expectedHeaderReceived("Content-Type", "application/json");
    	senaiteAnalysisRequestTemplateEndpoint.expectedPropertyReceived("service-analysis-template", "1019AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
    	senaiteAnalysisRequestTemplateEndpoint.setResultWaitTime(resultWaitTimeMillis);
    	
    	senaiteAnalysisRequestCreationEndpoint.whenAnyExchangeReceived(new Processor () {
			@Override
			public void process(Exchange exchange) throws Exception {
				exchange.getIn().setBody("{\"count\":1}");
			}
    		
    	});
    	senaiteAnalysisRequestCreationEndpoint.expectedHeaderReceived(Exchange.HTTP_METHOD, "POST");
    	senaiteAnalysisRequestCreationEndpoint.expectedHeaderReceived("Authorization", "Basic YWRtaW46YWRtaW4=");
    	senaiteAnalysisRequestCreationEndpoint.expectedHeaderReceived("Content-Type", "application/json");
    	senaiteAnalysisRequestCreationEndpoint.expectedPropertyReceived("client-uid", "8bf244463fc142d79625b14615b78e83");
    	senaiteAnalysisRequestCreationEndpoint.expectedBodiesReceived("{\"Contact\": \"4467613ed7af427e8d0e5fb525106c08\",\"SampleType\": \"9b8d423640cb4499b12d2b69b6107053\",\"DateSampled\": \"2021-01-29T09:49:47+00:00\",\"Template\": \"7aa1758fa4fa4a759ff3a239959724df\",\"Profiles\": \"31c0f8750acc41b9a69a814186a214f0\",\"Analyses\": [\"d95d8342028d4268a6c7502569fd3b61\",\"ea5839433b1a493aa1b7dbe7baef54db\",\"1075242c77b54817b2bbe577c80a5909\",\"002e723c7a814cc48a188f2aa9a2aac2\",\"51fbe2c4559446a9aec66a89b02c7631\",\"364dae23e38146fab0f58894516047b9\",\"cf82a32b2c304dbd9e447bc1f7586f6e\",\"84f8ec6acde04b3eb7d68194b275855d\",\"368588f785944ea9a06399ca1857f0e2\",\"a62387be591a451bb5d01beeb3f9dc6c\",\"ff44dc2aa1ac4f8689368d59cc0c0e8a\"],\"ClientSampleID\": \"eed578b7-86cb-43f5-91cd-daebdebfe6f8\"}");
    	senaiteAnalysisRequestCreationEndpoint.setResultWaitTime(resultWaitTimeMillis);
    	
    	openmrsFhirTaskEndpoint.whenAnyExchangeReceived(new Processor () {
			@Override
			public void process(Exchange exchange) throws Exception {
				exchange.getIn().setBody("{\"resourceType\": \"Task\", \"status\": \"requested\", \"intent\": \"order\", \"basedOn\": [{\"reference\":\"eed578b7-86cb-43f5-91cd-daebdebfe6f8\", \"type\": \"ServiceRequest\"}]}");
			}
    		
    	});
    	openmrsFhirTaskEndpoint.expectedHeaderReceived(Exchange.HTTP_METHOD, "POST");
    	openmrsFhirTaskEndpoint.expectedHeaderReceived("Authorization", "Basic c3VwZXJtYW46QWRtaW4xMjM=");
    	openmrsFhirTaskEndpoint.expectedPropertyReceived("lab-order-uuid", "eed578b7-86cb-43f5-91cd-daebdebfe6f8");
    	openmrsFhirTaskEndpoint.expectedBodiesReceived("{\"resourceType\": \"Task\", \"status\": \"requested\", \"intent\": \"order\", \"basedOn\": [{\"reference\":\"eed578b7-86cb-43f5-91cd-daebdebfe6f8\", \"type\": \"ServiceRequest\"}]}");
    	openmrsFhirTaskEndpoint.setResultWaitTime(resultWaitTimeMillis);
    }

}