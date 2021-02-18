package net.mekomsolutions.senaite.integration;

import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.model.ModelCamelContext;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.reifier.RouteReifier;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.eip.component.entity.Event;
import org.springframework.beans.factory.annotation.Value;

public class InboundLimsRouteITest extends BaseCamelContextSensitiveTest {  

    @EndpointInject(value = "mock:openmrsFhirTaskSearchEndpoint")
    private MockEndpoint openmrsFhirTaskSearchEndpoint;
    
    @EndpointInject(value = "mock:openmrsFhirServiceRequestEndpoint")
    private MockEndpoint openmrsFhirServiceRequestEndpoint;
    
    @EndpointInject(value = "mock:openmrsEncounterApiEndpoint")
    private MockEndpoint openmrsEncounterApiEndpoint;
    
    @EndpointInject(value = "mock:senaiteClientsSampleSearchEndpoint")
    private MockEndpoint senaiteClientsSampleSearchEndpoint;
    
    @EndpointInject(value = "mock:openmrsFhirTaskEndpoint")
    private MockEndpoint openmrsFhirTaskEndpoint;
    
    @EndpointInject(value = "mock:openmrsPatientEncounterForResultsEndpoint")
    private MockEndpoint openmrsPatientEncounterForResultsEndpoint;
    
    @EndpointInject(value = "mock:openmrsEncounterCreationEndpoint")
    private MockEndpoint openmrsEncounterCreationEndpoint;
    
    @EndpointInject(value = "mock:senaiteAnalysisResultsEndpoint")
    private MockEndpoint senaiteAnalysisResultsEndpoint;
    
    @EndpointInject(value = "mock:openmrsObsCreationEndpoint")
    private MockEndpoint openmrsObsCreationEndpoint;
    
    @Value("${serviceRequest-task-status.update.initial.delay}") 
    private String initialDelay;
    
    private int resultWaitTimeMillis = 100; 
    
    @Before
    public void setup() throws Exception {
    	loadXmlDefinedRoute("inbound-lims-route.xml");  
    	RouteDefinition routeDefinition = camelContext.adapt(ModelCamelContext.class).getRouteDefinitions().get(0);
    	RouteReifier.adviceWith(routeDefinition, camelContext, new AdviceWithRouteBuilder() {
    	    @Override
    	    public void configure() throws Exception {
    	    	weaveByToString("DynamicTo[{{openmrs.baseUrl}}/ws/fhir2/R4/Task?status=requested,accepted]").replace().toD("mock:openmrsFhirTaskSearchEndpoint");
    	    	weaveByToString("DynamicTo[{{openmrs.baseUrl}}/ws/fhir2/R4/ServiceRequest/${exchangeProperty.service-request-id}]").replace().toD("mock:openmrsFhirServiceRequestEndpoint");
    	    	weaveByToString("DynamicTo[{{openmrs.baseUrl}}/ws/rest/v1/encounter/${exchangeProperty.service-request-encounter-reference}]").replace().toD("mock:openmrsEncounterApiEndpoint");
    	    	weaveByToString("DynamicTo[{{senaite.baseUrl}}/@@API/senaite/v1/search?getClientSampleID=${exchangeProperty.service-request-id}&getClientID=${exchangeProperty.patient-uuid}&catalog=bika_catalog_analysisrequest_listing&complete=true]").replace().toD("mock:senaiteClientsSampleSearchEndpoint");
    	    	weaveByToString("DynamicTo[{{openmrs.baseUrl}}/ws/fhir2/R4/Task/${exchangeProperty.task-id}]").replace().toD("mock:openmrsFhirTaskEndpoint");
    	    	weaveByToString("DynamicTo[{{openmrs.baseUrl}}/ws/rest/v1/encounter?encounterType={{results.encounterType.uuid}}&patient=${exchangeProperty.patient-uuid}&v=custom:(uuid,encounterDatetime,patient:(uuid),location:(uuid))]").replace().toD("mock:openmrsPatientEncounterForResultsEndpoint");
    	    	weaveByToString("DynamicTo[{{openmrs.baseUrl}}/ws/rest/v1/encounter]").replace().toD("mock:openmrsEncounterCreationEndpoint");
    	    	weaveByToString("DynamicTo[${exchangeProperty.analysis-api_url}]").replace().toD("mock:senaiteAnalysisResultsEndpoint");
    	    	weaveByToString("DynamicTo[{{openmrs.baseUrl}}/ws/rest/v1/obs]").replace().toD("mock:openmrsObsCreationEndpoint");
    	    }
    	});
    	
    	setupExpectations();
    	
    }
    
    @After
    public void reset() throws Exception {
    	openmrsFhirTaskSearchEndpoint.reset();
    	openmrsFhirServiceRequestEndpoint.reset();
    	openmrsEncounterApiEndpoint.reset();
    	senaiteClientsSampleSearchEndpoint.reset();
    	openmrsFhirTaskEndpoint.reset();
    	openmrsPatientEncounterForResultsEndpoint.reset();
    	openmrsEncounterCreationEndpoint.reset();
    	senaiteAnalysisResultsEndpoint.reset();
    	openmrsObsCreationEndpoint.reset();
    }

    @Test
    public void shouldFulfillOpenmrsTestOrdersWithResultsFromSenaite() throws Exception {
    	// setup
    	
    	// replay
    	Thread.sleep(Integer.parseInt(initialDelay));
    	
    	// verify
    	openmrsFhirTaskSearchEndpoint.assertIsSatisfied();
    	openmrsFhirServiceRequestEndpoint.assertIsSatisfied();
    	openmrsEncounterApiEndpoint.assertIsSatisfied();
    	senaiteClientsSampleSearchEndpoint.assertIsSatisfied();
    	openmrsFhirTaskEndpoint.assertIsSatisfied();
    	openmrsPatientEncounterForResultsEndpoint.assertIsSatisfied();
    	openmrsEncounterCreationEndpoint.assertIsSatisfied();
    	openmrsFhirTaskEndpoint.assertIsSatisfied();
    	openmrsObsCreationEndpoint.assertIsSatisfied();
    }
    
    private void setupExpectations() {
    	openmrsFhirTaskSearchEndpoint.whenAnyExchangeReceived(new Processor () {
			@Override
			public void process(Exchange exchange) throws Exception {
				exchange.getIn().setBody("{\"resourceType\":\"Bundle\",\"id\":\"a5ef1bd1-eb4e-4906-a2ca-28c0a5ca078b\",\"meta\":{\"lastUpdated\":\"2021-02-17T12:48:48.882+00:00\"},\"type\":\"searchset\",\"total\":1,\"link\":[{\"relation\":\"self\",\"url\":\"http://openmrs:8080/openmrs/ws/fhir2/R4/Task?status=requested%2Caccepted\"}],\"entry\":[{\"fullUrl\":\"http://openmrs:8080/openmrs/ws/fhir2/R4/Task/15df1d72-f400-4428-b229-1febb94a6a9a\",\"resource\":{\"resourceType\":\"Task\",\"id\":\"15df1d72-f400-4428-b229-1febb94a6a9a\",\"identifier\":[{\"system\":\"http://openmrs.org/identifier\",\"value\":\"15df1d72-f400-4428-b229-1febb94a6a9a\"}],\"basedOn\":[{\"reference\":\"f1e7ed13-5512-49b9-9d90-90dd66e8e397\",\"type\":\"ServiceRequest\"}],\"status\":\"requested\",\"intent\":\"order\",\"authoredOn\":\"2021-02-17T12:48:25+00:00\",\"lastModified\":\"2021-02-17T12:48:25+00:00\"}}]}");
			}
    		
    	});
    	openmrsFhirTaskSearchEndpoint.expectedHeaderReceived(Exchange.HTTP_METHOD, "GET");
    	openmrsFhirTaskSearchEndpoint.expectedHeaderReceived("Authorization", "Basic YWRtaW46QWRtaW4xMjM=");
    	openmrsFhirTaskSearchEndpoint.setResultWaitTime(resultWaitTimeMillis);
    	
    	openmrsFhirServiceRequestEndpoint.whenAnyExchangeReceived(new Processor () {
			@Override
			public void process(Exchange exchange) throws Exception {
				exchange.getIn().setBody("{\"resourceType\":\"ServiceRequest\",\"id\":\"f1e7ed13-5512-49b9-9d90-90dd66e8e397\",\"identifier\":[{\"use\":\"usual\",\"type\":{\"coding\":[{\"system\":\"http://terminology.hl7.org/CodeSystem/v2-0203\",\"code\":\"PLAC\",\"display\":\"PlacerIdentifier\"}]},\"value\":\"ORD-8\"}],\"status\":\"active\",\"intent\":\"order\",\"code\":{\"coding\":[{\"code\":\"1019AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\",\"display\":\"Complete Blood Count\"}]},\"subject\":{\"reference\":\"Patient/8bd3db1d-533b-4e56-a945-b045bd9d3cb2\",\"type\":\"Patient\",\"display\":\"John Smith (Identifier: 3000004)\"},\"encounter\":{\"reference\":\"Encounter/ca51b8c1-3dba-44ba-99b1-9f04119130ad\",\"type\":\"Encounter\"},\"occurrencePeriod\":{\"start\":\"2021-02-17T12:47:38+00:00\",\"end\":\"2021-02-17T13:47:37+00:00\"},\"requester\":{\"reference\":\"Practitioner/1d0c6b21-60bd-11eb-afa0-0242ac18000a\",\"type\":\"Practitioner\",\"identifier\":{\"value\":\"superman\"},\"display\":\"Super Man (Identifier: superman)\"}}");
			}
    		
    	});
    	openmrsFhirServiceRequestEndpoint.expectedHeaderReceived(Exchange.HTTP_METHOD, "GET");
    	openmrsFhirServiceRequestEndpoint.expectedHeaderReceived("Authorization", "Basic YWRtaW46QWRtaW4xMjM=");
    	openmrsFhirServiceRequestEndpoint.expectedPropertyReceived("service-request-id", "f1e7ed13-5512-49b9-9d90-90dd66e8e397");
    	openmrsFhirServiceRequestEndpoint.setResultWaitTime(resultWaitTimeMillis);
    	
    	openmrsEncounterApiEndpoint.whenAnyExchangeReceived(new Processor () {
			@Override
			public void process(Exchange exchange) throws Exception {
				exchange.getIn().setBody("{\"visit\":{\"uuid\":\"16301f3a-6ba1-498a-bcb1-13a1d20092b8\"},\"location\":{\"uuid\":\"833d0c66-e29a-4d31-ac13-ca9050d1bfa9\"},\"encounterDatetime\":\"2021-02-17T12:39:20.000+0000\"}");
			}
    		
    	});
    	openmrsEncounterApiEndpoint.expectedHeaderReceived(Exchange.HTTP_METHOD, "GET");
    	openmrsEncounterApiEndpoint.expectedHeaderReceived("Authorization", "Basic YWRtaW46QWRtaW4xMjM=");
    	openmrsEncounterApiEndpoint.expectedPropertyReceived("service-request-encounter-reference", "ca51b8c1-3dba-44ba-99b1-9f04119130ad");
    	openmrsEncounterApiEndpoint.setResultWaitTime(resultWaitTimeMillis);
    	
    	senaiteClientsSampleSearchEndpoint.whenAnyExchangeReceived(new Processor () {
			@Override
			public void process(Exchange exchange) throws Exception {
				exchange.getIn().setBody("{\"count\":1,\"pagesize\":25,\"items\":[{\"SampleTypeTitle\":\"Blood\",\"getSampleTypeUID\":\"9b8d423640cb4499b12d2b69b6107053\",\"RejectionReasons\":null,\"getContactFullName\":\"SuperMan\",\"ProfilesUID\":[\"31c0f8750acc41b9a69a814186a214f0\"],\"ResultsInterpretation\":null,\"Template\":{\"url\":\"http://localhost:8088/senaite/bika_setup/bika_artemplates/artemplate-1\",\"uid\":\"7aa1758fa4fa4a759ff3a239959724df\",\"api_url\":\"http://localhost:8088/senaite/@@API/senaite/v1/artemplate/7aa1758fa4fa4a759ff3a239959724df\"},\"ContactUID\":\"659d62f704d04aa8919be4c474024dbe\",\"getInternalUse\":false,\"getSamplerEmail\":\"\",\"ClientSampleID\":\"f1e7ed13-5512-49b9-9d90-90dd66e8e397\",\"title\":\"BLD-0008\",\"MemberDiscount\":\"0.00\",\"ContactUsername\":null,\"DatePublished\":null,\"parent_id\":\"client-5\",\"parent_url\":\"http://localhost:8088/senaite/@@API/senaite/v1/client/27b34d7bdb2e4ea289a1e4afb3277719\",\"TemplateURL\":\"/senaite/bika_setup/bika_artemplates/artemplate-1\",\"getProfilesUID\":[\"31c0f8750acc41b9a69a814186a214f0\"],\"getSamplerFullName\":\"\",\"Batch\":null,\"getTemplateTitle\":\"Complete Blood CountTemplate\",\"getContactURL\":\"/senaite/clients/client-5/contact-9\",\"TemplateTitle\":\"Complete Blood CountTemplate\",\"getContactUID\":\"659d62f704d04aa8919be4c474024dbe\",\"parent_uid\":\"27b34d7bdb2e4ea289a1e4afb3277719\",\"SamplingDeviation\":{},\"getSampler\":\"\",\"getClientSampleID\":\"f1e7ed13-5512-49b9-9d90-90dd66e8e397\",\"getProfilesTitleStr\":\"Complete Blood Count\",\"SamplePoint\":{},\"creation_date\":\"2021-02-17T12:48:23+00:00\",\"Priority\":\"3\",\"getClientTitle\":\"John Smith (8bd3db1d-533b-4e56-a945-b045bd9d3cb2)\",\"getAnalysesNum\":[0,11,11,0],\"getClientID\":\"8bd3db1d-533b-4e56-a945-b045bd9d3cb2\",\"review_state\":\"published\",\"SamplePointUID\":null,\"tags\":[],\"getProfilesURL\":[\"/senaite/bika_setup/bika_analysisprofiles/analysisprofile-1\"],\"Profiles\":[{\"url\":\"http://localhost:8088/senaite/bika_setup/bika_analysisprofiles/analysisprofile-1\",\"uid\":\"31c0f8750acc41b9a69a814186a214f0\",\"api_url\":\"http://localhost:8088/senaite/@@API/senaite/v1/analysisprofile/31c0f8750acc41b9a69a814186a214f0\"}],\"Contact\":{\"url\":\"http://localhost:8088/senaite/clients/client-5/contact-9\",\"uid\":\"659d62f704d04aa8919be4c474024dbe\",\"api_url\":\"http://localhost:8088/senaite/@@API/senaite/v1/contact/659d62f704d04aa8919be4c474024dbe\"},\"path\":\"/senaite/clients/client-5/BLD-0008\",\"CreatorFullName\":\"admin\",\"EnvironmentalConditions\":null,\"language\":\"en\",\"created\":\"2021-02-17T12:48:23+00:00\",\"getSampleTypeTitle\":\"Blood\",\"getCreatorFullName\":\"admin\",\"getInvoiceExclude\":false,\"StorageLocation\":{},\"ContactFullName\":\"SuperMan\",\"TemplateUID\":\"7aa1758fa4fa4a759ff3a239959724df\",\"parent_path\":\"/senaite/clients/client-5\",\"getStorageLocationUID\":\"\",\"getBatchID\":\"\",\"Analyses\":[{\"url\":\"http://localhost:8088/senaite/clients/client-5/BLD-0008/HCT\",\"uid\":\"2a5004b0bc224d958dff8c859219b24f\",\"api_url\":\"http://localhost:8088/senaite/@@API/senaite/v1/analysis/2a5004b0bc224d958dff8c859219b24f\"},{\"url\":\"http://localhost:8088/senaite/clients/client-5/BLD-0008/RBC\",\"uid\":\"fc3d6f2affa24e2b9a7749893b433a3c\",\"api_url\":\"http://localhost:8088/senaite/@@API/senaite/v1/analysis/fc3d6f2affa24e2b9a7749893b433a3c\"}],\"portal_type\":\"AnalysisRequest\",\"getClientOrderNumber\":\"\",\"getPrioritySortkey\":\"3.2021-02-17T12:48:23+00:00\",\"getSamplingWorkflowEnabled\":false,\"getClientURL\":\"/senaite/clients/client-5\",\"modified\":\"2021-02-17T12:48:25+00:00\",\"Preservation\":{},\"getSamplingDeviationTitle\":\"\",\"getTemplateUID\":\"7aa1758fa4fa4a759ff3a239959724df\",\"ProfilesTitle\":[\"Complete Blood Count\"],\"Container\":{},\"uid\":\"8af51c0951a242a6aec52cc1c7b5e1d0\",\"ProfilesURL\":[\"/senaite/bika_setup/bika_analysisprofiles/analysisprofile-1\"],\"getPrinted\":\"0\",\"id\":\"BLD-0008\",\"getTemplateURL\":\"/senaite/bika_setup/bika_artemplates/artemplate-1\",\"api_url\":\"http://localhost:8088/senaite/@@API/senaite/v1/analysisrequest/8af51c0951a242a6aec52cc1c7b5e1d0\",\"author\":\"admin\",\"getSamplePointTitle\":\"\",\"SampleTypeUID\":\"9b8d423640cb4499b12d2b69b6107053\",\"BatchID\":null,\"SampleCondition\":{},\"getDescendantsUIDs\":[],\"Remarks\":null,\"getHazardous\":false,\"Profile\":null,\"DatePreserved\":null,\"description\":\"BLD-0008John Smith (8bd3db1d-533b-4e56-a945-b045bd9d3cb2)\",\"SampleType\":{\"url\":\"http://localhost:8088/senaite/bika_setup/bika_sampletypes/sampletype-1\",\"uid\":\"9b8d423640cb4499b12d2b69b6107053\",\"api_url\":\"http://localhost:8088/senaite/@@API/senaite/v1/sampletype/9b8d423640cb4499b12d2b69b6107053\"},\"ProfilesTitleStr\":\"Complete Blood Count\",\"getBatchURL\":\"\",\"getProgress\":0,\"getClientUID\":\"27b34d7bdb2e4ea289a1e4afb3277719\",\"modification_date\":\"2021-02-17T12:48:25+00:00\",\"effective\":\"1000-01-01T00:00:00+00:00\",\"url\":\"http://localhost:8088/senaite/clients/client-5/BLD-0008\",\"getPhysicalPath\":[\"\",\"senaite\",\"clients\",\"client-5\",\"BLD-0008\"],\"getCreatorEmail\":\"\",\"getProfilesTitle\":[\"Complete Blood Count\"],\"DetachedFrom\":null,\"creators\":[\"admin\"]}],\"page\":1,\"_runtime\":0.06883907318115234,\"next\":null,\"pages\":1,\"previous\":null}");
			}
    		
    	});
    	senaiteClientsSampleSearchEndpoint.expectedHeaderReceived(Exchange.HTTP_METHOD, "GET");
    	senaiteClientsSampleSearchEndpoint.expectedHeaderReceived("Authorization", "Basic YWRtaW46YWRtaW4=");
    	senaiteClientsSampleSearchEndpoint.expectedPropertyReceived("service-request-id", "f1e7ed13-5512-49b9-9d90-90dd66e8e397");
    	senaiteClientsSampleSearchEndpoint.expectedPropertyReceived("patient-uuid", "8bd3db1d-533b-4e56-a945-b045bd9d3cb2");
    	senaiteClientsSampleSearchEndpoint.setResultWaitTime(resultWaitTimeMillis);
    	
    	openmrsFhirTaskEndpoint.whenAnyExchangeReceived(new Processor () {
			@Override
			public void process(Exchange exchange) throws Exception {
				exchange.getIn().setBody("{}");
			}
    		
    	});
    	openmrsFhirTaskEndpoint.expectedHeaderReceived(Exchange.HTTP_METHOD, "PUT");
    	openmrsFhirTaskEndpoint.expectedHeaderReceived("Authorization", "Basic YWRtaW46QWRtaW4xMjM=");
    	openmrsFhirTaskEndpoint.expectedPropertyReceived("task-id", "15df1d72-f400-4428-b229-1febb94a6a9a");
    	openmrsFhirTaskEndpoint.expectedPropertyReceived("service-request-transitioned-status", "completed");
    	openmrsFhirTaskEndpoint.expectedBodiesReceived("{\"resourceType\": \"Task\", \"id\": \"15df1d72-f400-4428-b229-1febb94a6a9a\", \"status\": \"completed\", \"intent\": \"order\"}");
    	openmrsFhirTaskEndpoint.setResultWaitTime(resultWaitTimeMillis);
    	
    	openmrsPatientEncounterForResultsEndpoint.whenAnyExchangeReceived(new Processor () {
			@Override
			public void process(Exchange exchange) throws Exception {
				exchange.getIn().setBody("{\"results\": []}");
			}
    		
    	});
    	openmrsPatientEncounterForResultsEndpoint.expectedHeaderReceived(Exchange.HTTP_METHOD, "GET");
    	openmrsPatientEncounterForResultsEndpoint.expectedHeaderReceived("Authorization", "Basic YWRtaW46QWRtaW4xMjM=");
    	openmrsPatientEncounterForResultsEndpoint.expectedPropertyReceived("patient-uuid", "8bd3db1d-533b-4e56-a945-b045bd9d3cb2");
    	openmrsPatientEncounterForResultsEndpoint.setResultWaitTime(resultWaitTimeMillis);
    	
    	openmrsEncounterCreationEndpoint.whenAnyExchangeReceived(new Processor () {
			@Override
			public void process(Exchange exchange) throws Exception {
				exchange.getIn().setBody("{\"uuid\":\"359eeb8f-e938-4558-ad4a-cbf2b2449a5d\"}");
			}
    		
    	});
    	openmrsEncounterCreationEndpoint.expectedHeaderReceived(Exchange.HTTP_METHOD, "POST");
    	openmrsEncounterCreationEndpoint.expectedHeaderReceived("Authorization", "Basic YWRtaW46QWRtaW4xMjM=");
    	openmrsEncounterCreationEndpoint.expectedPropertyReceived("service-request-location-uuid", "833d0c66-e29a-4d31-ac13-ca9050d1bfa9");
    	openmrsEncounterCreationEndpoint.expectedPropertyReceived("service-request-encounter-datetime", "2021-02-17T12:39:20.000+0000");
    	openmrsEncounterCreationEndpoint.expectedPropertyReceived("patient-uuid", "8bd3db1d-533b-4e56-a945-b045bd9d3cb2");
    	openmrsEncounterCreationEndpoint.expectedPropertyReceived("service-request-visit-uuid", "16301f3a-6ba1-498a-bcb1-13a1d20092b8");
    	openmrsEncounterCreationEndpoint.expectedPropertyReceived("service-request-requester", "1d0c6b21-60bd-11eb-afa0-0242ac18000a");
    	openmrsEncounterCreationEndpoint.expectedBodiesReceived("{\"location\": \"833d0c66-e29a-4d31-ac13-ca9050d1bfa9\",\"encounterType\": \"0fe04e74-562f-11eb-87ac-0242ac1f0002\",\"encounterDatetime\": \"2021-02-17T12:39:20.000+0000\",\"patient\": \"8bd3db1d-533b-4e56-a945-b045bd9d3cb2\",\"visit\":\"16301f3a-6ba1-498a-bcb1-13a1d20092b8\", \"encounterProviders\":[{\"provider\": \"1d0c6b21-60bd-11eb-afa0-0242ac18000a\",\"encounterRole\": \"a0b03050-c99b-11e0-9572-0800200c9a66\"}]}");
    	openmrsEncounterCreationEndpoint.setResultWaitTime(resultWaitTimeMillis);
    	
    	senaiteAnalysisResultsEndpoint.whenAnyExchangeReceived(new Processor () {
			@Override
			public void process(Exchange exchange) throws Exception {
				if ( "http://localhost:8088/senaite/@@API/senaite/v1/analysis/2a5004b0bc224d958dff8c859219b24f".equals(exchange.getProperty("analysis-api_url"))) {
					exchange.getIn().setBody("{\"count\":1,\"pagesize\":25,\"items\":[{\"Keyword\":\"HCT\",\"portal_type\":\"Analysis\",\"language\":\"en\",\"Unit\":\"mg/l\",\"creation_date\":\"2021-02-17T12:48:24+00:00\",\"PointOfCapture\":\"lab\",\"ResultCaptureDate\":\"2021-02-17T15:38:01+00:00\",\"author\":\"admin\",\"description\":\"Percent of whole blood that is composed of red blood cells (1015AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA)\",\"ManualEntryOfResults\":true,\"title\":\"HEMATOCRIT\",\"Result\":\"3\",\"creators\":[\"admin\"]}],\"page\":1}");
				} else if ("http://localhost:8088/senaite/@@API/senaite/v1/analysis/fc3d6f2affa24e2b9a7749893b433a3c".equals(exchange.getProperty("analysis-api_url"))) {
					exchange.getIn().setBody("{\"count\":1,\"pagesize\":25,\"items\":[{\"Keyword\":\"RBC\",\"portal_type\":\"Analysis\",\"language\":\"en\",\"Unit\":\"mg/l\",\"creation_date\":\"2021-02-17T12:48:24+00:00\",\"PointOfCapture\":\"lab\",\"ResultCaptureDate\":\"2021-02-17T15:38:01+00:00\",\"description\":\"Blood test to measure the number of red blood cells (679AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA)\",\"ManualEntryOfResults\":true,\"title\":\"RED BLOOD CELLS\",\"Result\":\"9\",\"creators\":[\"admin\"]}],\"page\":1}");
				}
			}
    		
    	});
    	senaiteAnalysisResultsEndpoint.expectedHeaderReceived(Exchange.HTTP_METHOD, "GET");
    	senaiteAnalysisResultsEndpoint.expectedHeaderReceived("Authorization", "Basic YWRtaW46YWRtaW4=");
    	senaiteAnalysisResultsEndpoint.expectedHeaderReceived("Content-Type", "application/json");
    	senaiteAnalysisResultsEndpoint.setResultWaitTime(resultWaitTimeMillis);
    	
    	openmrsObsCreationEndpoint.whenAnyExchangeReceived(new Processor () {
			@Override
			public void process(Exchange exchange) throws Exception {
				exchange.getIn().setBody("{}");
			}
    		
    	});
    	openmrsObsCreationEndpoint.expectedHeaderReceived(Exchange.HTTP_METHOD, "POST");
    	openmrsObsCreationEndpoint.expectedHeaderReceived("Authorization", "Basic YWRtaW46QWRtaW4xMjM=");
    	openmrsObsCreationEndpoint.expectedBodiesReceived("{\"concept\":\"1019AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\",\"order\": \"f1e7ed13-5512-49b9-9d90-90dd66e8e397\",\"person\": \"8bd3db1d-533b-4e56-a945-b045bd9d3cb2\",\"encounter\": \"359eeb8f-e938-4558-ad4a-cbf2b2449a5d\",\"obsDatetime\": \"2021-02-17T15:38:01+00:00\",\"groupMembers\":[{\"concept\":\"1015AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\",\"order\": \"f1e7ed13-5512-49b9-9d90-90dd66e8e397\",\"person\": \"8bd3db1d-533b-4e56-a945-b045bd9d3cb2\",\"obsDatetime\": \"2021-02-17T15:38:01+00:00\",\"groupMembers\":[{\"concept\":\"1015AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\",\"order\": \"f1e7ed13-5512-49b9-9d90-90dd66e8e397\",\"person\": \"8bd3db1d-533b-4e56-a945-b045bd9d3cb2\",\"obsDatetime\": \"2021-02-17T15:38:01+00:00\",\"groupMembers\":[{\"value\":\"3\",\"order\": \"f1e7ed13-5512-49b9-9d90-90dd66e8e397\",\"person\": \"8bd3db1d-533b-4e56-a945-b045bd9d3cb2\",\"obsDatetime\": \"2021-02-17T15:38:01+00:00\",\"concept\":\"1015AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\"}]}]},{\"concept\":\"679AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\",\"order\": \"f1e7ed13-5512-49b9-9d90-90dd66e8e397\",\"person\": \"8bd3db1d-533b-4e56-a945-b045bd9d3cb2\",\"obsDatetime\": \"2021-02-17T15:38:01+00:00\",\"groupMembers\":[{\"concept\":\"679AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\",\"order\": \"f1e7ed13-5512-49b9-9d90-90dd66e8e397\",\"person\": \"8bd3db1d-533b-4e56-a945-b045bd9d3cb2\",\"obsDatetime\": \"2021-02-17T15:38:01+00:00\",\"groupMembers\":[{\"value\":\"9\",\"order\": \"f1e7ed13-5512-49b9-9d90-90dd66e8e397\",\"person\": \"8bd3db1d-533b-4e56-a945-b045bd9d3cb2\",\"obsDatetime\": \"2021-02-17T15:38:01+00:00\",\"concept\":\"679AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\"}]}]}]}");
    	openmrsObsCreationEndpoint.setResultWaitTime(resultWaitTimeMillis);
    }

}