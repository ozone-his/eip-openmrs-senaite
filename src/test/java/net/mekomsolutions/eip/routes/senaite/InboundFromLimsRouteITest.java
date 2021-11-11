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
public class InboundFromLimsRouteITest extends BaseWatcherRouteTest {

	@EndpointInject(value = "mock:fetchServiceRequestTasksRoute")
	private MockEndpoint fetchServiceRequestTasksRoute;
	
	@EndpointInject(value = "mock:serviceRequestEndpoint")
	private MockEndpoint serviceRequestEndpoint;
	
	@EndpointInject(value = "mock:taskEndpoint")
	private MockEndpoint taskEndpoint;
	
	@EndpointInject(value = "mock:encounterEndpoint")
	private MockEndpoint encounterEndpoint;
	
	@EndpointInject(value = "mock:authenticateToSenaiteRoute")
	private MockEndpoint authenticateToSenaiteRoute;
	
	@EndpointInject(value = "mock:analysisRequestSearchEndpoint")
	private MockEndpoint analysisRequestSearchEndpoint;
	
	@EndpointInject(value = "mock:processServiceRequestTaskStateRoute")
	private MockEndpoint processServiceRequestTaskStateRoute;
	
	@EndpointInject(value = "mock:createServiceRequestResultsToOpenmrsRoute")
	private MockEndpoint createServiceRequestResultsToOpenmrsRoute;
	
	@EndpointInject(value = "mock:updateServiceRequestTaskRoute")
	private MockEndpoint updateServiceRequestTaskRoute;

	private int initialDelay = 10000;

	private int resultWaitTimeMillis = 100;

	@Before
	public void setup() throws Exception {
		loadXmlRoutesInDirectory("senaite", "inbound-fromLims-route.xml");
		RouteDefinition routeDefinition = camelContext.adapt(ModelCamelContext.class).getRouteDefinitions().stream()
				.filter(routeDef -> "inbound-fromLims".equals(routeDef.getRouteId())).collect(Collectors.toList()).get(0);
		RouteReifier.adviceWith(routeDefinition, camelContext, new AdviceWithRouteBuilder() {
			@Override
			public void configure() throws Exception {
				weaveByToString("To[direct:fetch-ServiceRequestTasks]").replace().toD("mock:fetchServiceRequestTasksRoute");
				weaveByToString("DynamicTo[{{openmrs.baseUrl}}/ws/fhir2/R4/ServiceRequest/${exchangeProperty.service-request-id}?throwExceptionOnFailure=false]").replace().toD("mock:serviceRequestEndpoint");
				weaveByToString("DynamicTo[{{openmrs.baseUrl}}/ws/fhir2/R4/Task/${exchangeProperty.task-id}]").replace().toD("mock:taskEndpoint");
				weaveByToString("DynamicTo[{{openmrs.baseUrl}}/ws/rest/v1/encounter/${exchangeProperty.service-request-encounter-reference}]").replace().toD("mock:encounterEndpoint");
				weaveByToString("To[direct:authenticate-toSenaite]").replace().toD("mock:authenticateToSenaiteRoute");
				weaveByToString("DynamicTo[{{senaite.baseUrl}}/@@API/senaite/v1/search?getClientSampleID=${exchangeProperty.service-request-id}&getClientID=${exchangeProperty.patient-uuid}&catalog=bika_catalog_analysisrequest_listing&complete=true]").replace().toD("mock:analysisRequestSearchEndpoint");
				weaveByToString("To[direct:process-serviceRequest-taskState]").replace().toD("mock:processServiceRequestTaskStateRoute");
				weaveByToString("To[direct:create-serviceRequestResults-toOpenmrs]").replace().toD("mock:createServiceRequestResultsToOpenmrsRoute");
				weaveByToString("To[direct:update-serviceRequest-task]").replace().toD("mock:updateServiceRequestTaskRoute");
			}
		});

		setupExpectations();

	}

	@After
	public void reset() throws Exception {
		fetchServiceRequestTasksRoute.reset();
	}

	@Test
	public void shouldFulfillOpenmrsTestOrdersWithResultsFromSenaite() throws Exception {
		// setup

		// replay
		Thread.sleep(initialDelay);

		// verify

	}

	private void setupExpectations() {
		fetchServiceRequestTasksRoute.whenAnyExchangeReceived(new Processor() {
			@Override
			public void process(Exchange exchange) throws Exception {
				exchange.getIn().setBody(
						"{\"resourceType\":\"Bundle\",\"id\":\"a5ef1bd1-eb4e-4906-a2ca-28c0a5ca078b\",\"meta\":{\"lastUpdated\":\"2021-02-17T12:48:48.882+00:00\"},\"type\":\"searchset\",\"total\":1,\"link\":[{\"relation\":\"self\",\"url\":\"http://openmrs:8080/openmrs/ws/fhir2/R4/Task?status=requested%2Caccepted\"}],\"entry\":[{\"fullUrl\":\"http://openmrs:8080/openmrs/ws/fhir2/R4/Task/15df1d72-f400-4428-b229-1febb94a6a9a\",\"resource\":{\"resourceType\":\"Task\",\"id\":\"15df1d72-f400-4428-b229-1febb94a6a9a\",\"identifier\":[{\"system\":\"http://openmrs.org/identifier\",\"value\":\"15df1d72-f400-4428-b229-1febb94a6a9a\"}],\"basedOn\":[{\"reference\":\"f1e7ed13-5512-49b9-9d90-90dd66e8e397\",\"type\":\"ServiceRequest\"}],\"status\":\"requested\",\"intent\":\"order\",\"authoredOn\":\"2021-02-17T12:48:25+00:00\",\"lastModified\":\"2021-02-17T12:48:25+00:00\"}}]}");
			}

		});
		fetchServiceRequestTasksRoute.expectedHeaderReceived(Exchange.HTTP_METHOD, "GET");
		fetchServiceRequestTasksRoute.setResultWaitTime(resultWaitTimeMillis);

	}

}