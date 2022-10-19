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
import org.openmrs.eip.mysql.watcher.route.BaseWatcherRouteTest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.TestExecutionListeners.MergeMode;

@MockEndpoints
@Import({ TestConfiguration.class })
@TestExecutionListeners(value = {}, mergeMode = MergeMode.REPLACE_DEFAULTS)
public class PollSenaiteITest extends BaseWatcherRouteTest {

	@EndpointInject(value = "mock:fetchServiceRequestTasksRoute")
	private MockEndpoint fetchServiceRequestTasksRoute;
	
	@EndpointInject(value = "mock:serviceRequestEndpoint")
	private MockEndpoint serviceRequestEndpoint;
	
	@EndpointInject(value = "mock:taskEndpoint")
	private MockEndpoint taskEndpoint;
	
	@EndpointInject(value = "mock:encounterEndpoint")
	private MockEndpoint encounterEndpoint;
	
	@EndpointInject(value = "mock:authenticateToOpenmrsRoute")
	private MockEndpoint authenticateToOpenmrsRoute;
	
	@EndpointInject(value = "mock:authenticateToSenaiteRoute")
	private MockEndpoint authenticateToSenaiteRoute;

	@EndpointInject(value = "mock:retrievePatientId")
    private MockEndpoint retrievePatientId;
	
	@EndpointInject(value = "mock:analysisRequestSearchEndpoint")
	private MockEndpoint analysisRequestSearchEndpoint;
	
	@EndpointInject(value = "mock:createServiceRequestResultsToOpenmrsRoute")
	private MockEndpoint createServiceRequestResultsToOpenmrsRoute;
	
	@EndpointInject(value = "mock:updateServiceRequestTaskRoute")
	private MockEndpoint updateServiceRequestTaskRoute;

	private int initialDelay = 10000;

	private int resultWaitTimeMillis = 100;

	@Before
	public void setup() throws Exception {
		loadXmlRoutesInDirectory("senaite", "poll-senaite-route.xml", "process-serviceRequest-taskState-route.xml");
		RouteDefinition routeDefinition = camelContext.adapt(ModelCamelContext.class).getRouteDefinitions().stream()
				.filter(routeDef -> "poll-senaite".equals(routeDef.getRouteId())).collect(Collectors.toList()).get(0);
		RouteReifier.adviceWith(routeDefinition, camelContext, new AdviceWithRouteBuilder() {
			@Override
			public void configure() throws Exception {
				weaveByToString("To[direct:fetch-serviceRequestTasks-from-openmrs]").replace().toD("mock:fetchServiceRequestTasksRoute");
				weaveByToString("DynamicTo[{{fhirR4.baseUrl}}/ServiceRequest/${exchangeProperty.service-request-id}?throwExceptionOnFailure=false]").replace().toD("mock:serviceRequestEndpoint");
				weaveByToString("DynamicTo[{{fhirR4.baseUrl}}/Task/${exchangeProperty.task-id}]").replace().toD("mock:taskEndpoint");
				weaveByToString("DynamicTo[{{openmrs.baseUrl}}/ws/rest/v1/encounter/${exchangeProperty.service-request-encounter-reference}]").replace().toD("mock:encounterEndpoint");
				weaveByToString("To[direct:authenticate-to-openmrs]").replace().toD("mock:authenticateToOpenmrsRoute");
				weaveByToString("To[direct:authenticate-to-senaite]").replace().toD("mock:authenticateToSenaiteRoute");
				weaveByToString("To[direct:retrieve-patientId]").replace().toD("mock:retrievePatientId");
				weaveByToString("DynamicTo[{{senaite.baseUrl}}/@@API/senaite/v1/search?getClientSampleID=${exchangeProperty.service-request-id}&getClientID=${exchangeProperty.patient-id}&catalog=senaite_catalog_sample&complete=true]").replace().to("mock:analysisRequestSearchEndpoint");
				weaveByToString("To[direct:create-serviceRequestResults-to-openmrs]").replace().to("mock:createServiceRequestResultsToOpenmrsRoute");
				weaveByToString("To[direct:update-serviceRequest-task-to-openmrs]").replace().to("mock:updateServiceRequestTaskRoute");
			}
		});

		setupExpectations();

	}

	@After
	public void reset() throws Exception {
		fetchServiceRequestTasksRoute.reset();
		serviceRequestEndpoint.reset();
		taskEndpoint.reset();
		encounterEndpoint.reset();
		authenticateToOpenmrsRoute.reset();
		authenticateToSenaiteRoute.reset();
		retrievePatientId.reset();
		createServiceRequestResultsToOpenmrsRoute.reset();
		updateServiceRequestTaskRoute.reset();
	}

	@Test
	public void shouldFulfillOpenmrsTestOrdersWithResultsFromSenaite() throws Exception {
		// setup

		// replay
		Thread.sleep(initialDelay);

		// verify
		fetchServiceRequestTasksRoute.assertIsSatisfied();
		serviceRequestEndpoint.assertIsSatisfied();
		taskEndpoint.assertIsSatisfied();
		encounterEndpoint.assertIsSatisfied();
		authenticateToSenaiteRoute.assertIsSatisfied();
		createServiceRequestResultsToOpenmrsRoute.assertIsSatisfied();
		updateServiceRequestTaskRoute.assertIsSatisfied();

	}

	private void setupExpectations() {
		fetchServiceRequestTasksRoute.whenAnyExchangeReceived(new Processor() {
			@Override
			public void process(Exchange exchange) throws Exception {
				exchange.getIn().setBody(
						"[{\"fullUrl\":\"http://openmrs:8080/openmrs/ws/fhir2/R4/Task/9d219129-2624-4432-911a-9fe3e400a93c\",\"resource\":{\"resourceType\":\"Task\",\"id\":\"9d219129-2624-4432-911a-9fe3e400a93c\",\"text\":{\"status\":\"generated\",\"div\":\"<div></div>\"},\"contained\":[{\"resourceType\":\"Provenance\",\"id\":\"b3fcc874-5066-45b4-8aa3-43d31f26d5fc\",\"recorded\":\"2021-11-29T13:17:40.000+00:00\",\"activity\":{\"coding\":[{\"system\":\"http://terminology.hl7.org/CodeSystemv3-DataOperation\",\"code\":\"CREATE\",\"display\":\"create\"}]},\"agent\":[{\"type\":{\"coding\":[{\"system\":\"http://terminology.hl7.org/CodeSystemprovenance-participant-type\",\"code\":\"author\",\"display\":\"Author\"}]},\"role\":[{\"coding\":[{\"system\":\"http://terminology.hl7.org/CodeSystemv3-ParticipationType\",\"code\":\"AUT\",\"display\":\"author\"}]}],\"who\":{\"reference\":\"Practitioner/d042597b-1d09-11ec-9616-0242ac1a000a\",\"type\":\"Practitioner\",\"display\":\"Super Man\"}}]}],\"identifier\":[{\"system\":\"http://fhir.openmrs.org/ext/task/identifier\",\"value\":\"9d219129-2624-4432-911a-9fe3e400a93c\"}],\"basedOn\":[{\"reference\":\"27d730cb-1c04-4ced-a2ed-ad0f18fed728\",\"type\":\"ServiceRequest\"}],\"status\":\"requested\",\"intent\":\"order\",\"authoredOn\":\"2021-11-29T13:17:40+00:00\",\"lastModified\":\"2021-11-29T13:17:40+00:00\"}}]");
			}

		});
		fetchServiceRequestTasksRoute.setResultWaitTime(resultWaitTimeMillis);

		serviceRequestEndpoint.whenAnyExchangeReceived(new Processor() {
			@Override
			public void process(Exchange exchange) throws Exception {
				exchange.getIn().setBody(
						"{\"resourceType\":\"ServiceRequest\",\"id\":\"27d730cb-1c04-4ced-a2ed-ad0f18fed728\",\"text\":{\"status\":\"generated\",\"div\":\"<div></div>\"},\"status\":\"completed\",\"intent\":\"order\",\"code\":{\"coding\":[{\"code\":\"ab3b5775-7080-4cb1-8be5-54e367940145\",\"display\":\"LAB1015 - Thyroid Function Tests\"}]},\"subject\":{\"reference\":\"Patient/0298aa1b-7fa1-4244-93e7-c5138df63bb3\",\"type\":\"Patient\",\"display\":\"Johnson Smith (Numéro Dossier: HCD-3000013)\"},\"encounter\":{\"reference\":\"Encounter/e1c36ec4-1af1-40cb-ba79-ac810e5567c5\",\"type\":\"Encounter\"},\"occurrencePeriod\":{\"start\":\"2021-11-29T13:13:48+00:00\",\"end\":\"2021-11-29T14:13:48+00:00\"},\"requester\":{\"reference\":\"Practitioner/d042d719-1d09-11ec-9616-0242ac1a000a\",\"type\":\"Practitioner\",\"identifier\":{\"value\":\"superman\"},\"display\":\"Super Man (Identifier: superman)\"}}");
			}

		});
		serviceRequestEndpoint.expectedHeaderReceived(Exchange.HTTP_METHOD, "GET");
		serviceRequestEndpoint.setResultWaitTime(resultWaitTimeMillis);
		
		encounterEndpoint.whenAnyExchangeReceived(new Processor() {
			@Override
			public void process(Exchange exchange) throws Exception {
				exchange.getIn().setBody(
						"{\"uuid\":\"e1c36ec4-1af1-40cb-ba79-ac810e5567c5\",\"display\":\"Consultation 29/11/2021\",\"encounterDatetime\":\"2021-11-29T13:13:48.000+0000\",\"patient\":{\"uuid\":\"0298aa1b-7fa1-4244-93e7-c5138df63bb3\",\"display\":\"HCD-3000013 - Johnson Smith\",\"links\":[{\"rel\":\"self\",\"uri\":\"http://openmrs:8080/openmrs/ws/rest/v1/patient/0298aa1b-7fa1-4244-93e7-c5138df63bb3\"}]},\"location\":{\"uuid\":\"47816337-9fbe-4b09-821f-8690dc8031c0\",\"display\":\"Hôpital Sacré-Coeur\",\"links\":[{\"rel\":\"self\",\"uri\":\"http://openmrs:8080/openmrs/ws/rest/v1/location/47816337-9fbe-4b09-821f-8690dc8031c0\"}]},\"form\":null,\"encounterType\":{\"uuid\":\"dd528487-82a5-4082-9c72-ed246bd49591\",\"display\":\"Consultation\",\"links\":[{\"rel\":\"self\",\"uri\":\"http://openmrs:8080/openmrs/ws/rest/v1/encountertype/dd528487-82a5-4082-9c72-ed246bd49591\"}]},\"obs\":[],\"orders\":[{\"uuid\":\"6533a4ab-9a03-4c81-af0d-f9b3fc7a4ef3\",\"display\":\"LAB1015 - Thyroid Function Tests\",\"voided\":true,\"links\":[{\"rel\":\"self\",\"uri\":\"http://openmrs:8080/openmrs/ws/rest/v1/order/6533a4ab-9a03-4c81-af0d-f9b3fc7a4ef3\"}],\"type\":\"testorder\"}],\"voided\":false,\"visit\":{\"uuid\":\"a1ab85c3-b524-4727-a352-2aefa26181cd\",\"display\":\"Général @ Hôpital Sacré-Coeur - 29/11/2021 12:39\",\"links\":[{\"rel\":\"self\",\"uri\":\"http://openmrs:8080/openmrs/ws/rest/v1/visit/a1ab85c3-b524-4727-a352-2aefa26181cd\"}]},\"encounterProviders\":[{\"uuid\":\"390697f6-8d21-4f3e-9aa0-79a87ac32e3b\",\"display\":\"Super Man: Unknown\",\"links\":[{\"rel\":\"self\",\"uri\":\"http://openmrs:8080/openmrs/ws/rest/v1/encounter/e1c36ec4-1af1-40cb-ba79-ac810e5567c5/encounterprovider/390697f6-8d21-4f3e-9aa0-79a87ac32e3b\"}]}],\"links\":[{\"rel\":\"self\",\"uri\":\"http://openmrs:8080/openmrs/ws/rest/v1/encounter/e1c36ec4-1af1-40cb-ba79-ac810e5567c5\"},{\"rel\":\"full\",\"uri\":\"http://openmrs:8080/openmrs/ws/rest/v1/encounter/e1c36ec4-1af1-40cb-ba79-ac810e5567c5?v=full\"}],\"resourceVersion\":\"1.9\"}");
			}

		});
		encounterEndpoint.expectedHeaderReceived(Exchange.HTTP_METHOD, "GET");
		encounterEndpoint.setResultWaitTime(resultWaitTimeMillis);

		retrievePatientId.expectedPropertyReceived("patient-reference", "Patient/0298aa1b-7fa1-4244-93e7-c5138df63bb3");
        retrievePatientId.whenAnyExchangeReceived(new Processor () {
            @Override
            public void process(Exchange exchange) throws Exception {
                exchange.setProperty("patient-id", "some-unique-patient-id");
            }
            
        });
        retrievePatientId.setResultWaitTime(resultWaitTimeMillis);
		
		analysisRequestSearchEndpoint.whenAnyExchangeReceived(new Processor() {
			@Override
			public void process(Exchange exchange) throws Exception {
				exchange.getIn().setBody(
						"{\"count\":1,\"pagesize\":25,\"items\":[{\"SampleTypeTitle\":\"Blood\",\"getSampleTypeUID\":\"520131495de146a88ab58c614839054b\",\"getContactFullName\":\"Super Man\",\"ProfilesUID\":[\"ae25816a6d804edba01ebdb24ffcd14e\"],\"ResultsInterpretation\":null,\"Template\":{\"url\":\"http://localhost:8081/senaite/bika_setup/bika_artemplates/artemplate-6\",\"uid\":\"13da8d9611d6485099c6f7c08a2c6355\",\"api_url\":\"http://localhost:8081/senaite/@@API/senaite/v1/artemplate/13da8d9611d6485099c6f7c08a2c6355\"},\"ContactUID\":\"4f8b31a780bf4754af104d42173d3c41\",\"getInternalUse\":false,\"getSamplerEmail\":\"\",\"ClientSampleID\":\"27d730cb-1c04-4ced-a2ed-ad0f18fed728\",\"title\":\"BLD-0001\",\"MemberDiscount\":\"0.00\",\"parent_id\":\"client-1\",\"getClientReference\":\"\",\"parent_url\":\"http://localhost:8081/senaite/@@API/senaite/v1/client/b774b05115824532bb589cbf47715570\",\"TemplateURL\":\"/senaite/bika_setup/bika_artemplates/artemplate-6\",\"assigned_state\":\"unassigned\",\"getProfilesUID\":[\"ae25816a6d804edba01ebdb24ffcd14e\"],\"getSamplerFullName\":\"\",\"Batch\":null,\"getTemplateTitle\":\"LAB1015 - Thyroid Function Tests(TSH-T3-T4) Template\",\"getContactURL\":\"/senaite/clients/client-1/contact-1\",\"TemplateTitle\":\"LAB1015 - Thyroid Function Tests(TSH-T3-T4) Template\",\"getContactUID\":\"4f8b31a780bf4754af104d42173d3c41\",\"parent_uid\":\"b774b05115824532bb589cbf47715570\",\"getContactEmail\":\"\",\"SamplingDeviation\":{},\"getSampler\":\"\",\"getClientSampleID\":\"27d730cb-1c04-4ced-a2ed-ad0f18fed728\",\"getProfilesTitleStr\":\"LAB1015 - Thyroid Function Tests(TSH-T3-T4)\",\"creation_date\":\"2021-11-29T13:17:37+00:00\",\"modification_date\":\"2021-11-29T13:17:39+00:00\",\"getClientTitle\":\"Johnson Smith (0298aa1b-7fa1-4244-93e7-c5138df63bb3)\",\"getAnalysesNum\":[0,3,3,0],\"getClientID\":\"0298aa1b-7fa1-4244-93e7-c5138df63bb3\",\"review_state\":\"published\",\"getProfilesURL\":[\"/senaite/bika_setup/bika_analysisprofiles/analysisprofile-6\"],\"Profiles\":[{\"url\":\"http://localhost:8081/senaite/bika_setup/bika_analysisprofiles/analysisprofile-6\",\"uid\":\"ae25816a6d804edba01ebdb24ffcd14e\",\"api_url\":\"http://localhost:8081/senaite/@@API/senaite/v1/analysisprofile/ae25816a6d804edba01ebdb24ffcd14e\"}],\"SamplingDate\":null,\"AnalysisServicesSettings\":null,\"Contact\":{\"url\":\"http://localhost:8081/senaite/clients/client-1/contact-1\",\"uid\":\"4f8b31a780bf4754af104d42173d3c41\",\"api_url\":\"http://localhost:8081/senaite/@@API/senaite/v1/contact/4f8b31a780bf4754af104d42173d3c41\"},\"path\":\"/senaite/clients/client-1/BLD-0001\",\"CreatorFullName\":\"admin\",\"language\":\"en\",\"created\":\"2021-11-29T13:17:37+00:00\",\"getSampleTypeTitle\":\"Blood\",\"getCreatorFullName\":\"admin\",\"ResultsInterpretationDepts\":null,\"StorageLocation\":{},\"getInvoiceExclude\":false,\"ContactFullName\":\"Super Man\",\"TemplateUID\":\"13da8d9611d6485099c6f7c08a2c6355\",\"getStorageLocationUID\":\"\",\"getBatchID\":\"\",\"Analyses\":[{\"url\":\"http://localhost:8081/senaite/clients/client-1/BLD-0001/TSH\",\"uid\":\"f1f60a5498bb4a64bd6bfcda2ce28a5a\",\"api_url\":\"http://localhost:8081/senaite/@@API/senaite/v1/analysis/f1f60a5498bb4a64bd6bfcda2ce28a5a\"},{\"url\":\"http://localhost:8081/senaite/clients/client-1/BLD-0001/T3\",\"uid\":\"8dc73cfcfaca4d2892d1de9b959a51c1\",\"api_url\":\"http://localhost:8081/senaite/@@API/senaite/v1/analysis/8dc73cfcfaca4d2892d1de9b959a51c1\"},{\"url\":\"http://localhost:8081/senaite/clients/client-1/BLD-0001/T4\",\"uid\":\"84aa9d51a11e4fa8b205c1e40bc29831\",\"api_url\":\"http://localhost:8081/senaite/@@API/senaite/v1/analysis/84aa9d51a11e4fa8b205c1e40bc29831\"}],\"portal_type\":\"AnalysisRequest\",\"getClientOrderNumber\":\"\",\"getClientURL\":\"/senaite/clients/client-1\",\"modified\":\"2021-11-29T13:17:39+00:00\",\"getTemplateUID\":\"13da8d9611d6485099c6f7c08a2c6355\",\"ProfilesTitle\":[\"LAB1015 - Thyroid Function Tests(TSH-T3-T4)\"],\"uid\":\"0b493f7934da4d618d57938e4fdc4246\",\"ProfilesURL\":[\"/senaite/bika_setup/bika_analysisprofiles/analysisprofile-6\"],\"id\":\"BLD-0001\",\"getTemplateURL\":\"/senaite/bika_setup/bika_artemplates/artemplate-6\",\"api_url\":\"http://localhost:8081/senaite/@@API/senaite/v1/analysisrequest/0b493f7934da4d618d57938e4fdc4246\",\"author\":\"admin\",\"SampleTypeUID\":\"520131495de146a88ab58c614839054b\",\"Remarks\":null,\"description\":\"BLD-0001 Johnson Smith (0298aa1b-7fa1-4244-93e7-c5138df63bb3)\",\"SampleType\":{\"url\":\"http://localhost:8081/senaite/bika_setup/bika_sampletypes/sampletype-1\",\"uid\":\"520131495de146a88ab58c614839054b\",\"api_url\":\"http://localhost:8081/senaite/@@API/senaite/v1/sampletype/520131495de146a88ab58c614839054b\"},\"ProfilesTitleStr\":\"LAB1015 - Thyroid Function Tests(TSH-T3-T4)\",\"getClientUID\":\"b774b05115824532bb589cbf47715570\",\"effective\":\"1000-01-01T00:00:00+00:00\",\"getDateVerified\":null,\"getProfilesTitle\":[\"LAB1015 - Thyroid Function Tests(TSH-T3-T4)\"],\"DetachedFrom\":null,\"creators\":[\"admin\"]}],\"page\":1,\"_runtime\":0.09549593925476074,\"pages\":1}");
			}

		});
		analysisRequestSearchEndpoint.expectedHeaderReceived(Exchange.HTTP_METHOD, "GET");
		analysisRequestSearchEndpoint.expectedPropertyReceived("patient-id", "some-unique-patient-id");
		analysisRequestSearchEndpoint.setResultWaitTime(resultWaitTimeMillis);
		
		createServiceRequestResultsToOpenmrsRoute.setAssertPeriod(resultWaitTimeMillis);
		updateServiceRequestTaskRoute.setAssertPeriod(resultWaitTimeMillis);
	}

}