package com.ozonehis.eip.routes.senaite;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.support.DefaultExchange;
import org.apache.camel.test.spring.junit5.MockEndpoints;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@MockEndpoints
public class FetchServiceRequestTasksFromOpenmrsRouteITest extends BaseCamelRoutesTest {

    @EndpointInject(value = "mock:authenticateToOpenmrsRoute")
    private MockEndpoint authenticateToOpenmrs;

    @EndpointInject(value = "mock:fetchTasksEndpoint")
    private MockEndpoint fetchTasksEndpoint;

    @BeforeEach
    public void setup() throws Exception {
        loadXmlRoutesInDirectory("camel", "fetch-servicerequest-tasks-from-openmrs-route.xml");

        advise("fetch-servicerequest-tasks-from-openmrs", new AdviceWithRouteBuilder() {
            @Override
            public void configure() {
                weaveByToString("To[direct:authenticate-to-openmrs]").replace().toD("mock:authenticateToOpenmrsRoute");
                weaveByToString(".*/Task\\?status=requested,accepted]")
                        .replace()
                        .toD("mock:fetchTasksEndpoint");
            }
        });

        setupExpectations();
    }

    @AfterEach
    public void reset() throws Exception {
        fetchTasksEndpoint.reset();
    }

    @Test
    public void shouldFethRequestedAndOrAcceptedServiceRequestTasks() throws Exception {
        // setup
        String expectedBody =
                "[{fullUrl=http://openmrs:8080/openmrs/ws/fhir2/R4/Task/77ee5f8b-c9ec-4d1f-b3a3-fdae006f1032, resource={resourceType=Task, id=77ee5f8b-c9ec-4d1f-b3a3-fdae006f1032, text={status=generated, div=<div />}, identifier=[{system=http://fhir.openmrs.org/ext/task/identifier, value=77ee5f8b-c9ec-4d1f-b3a3-fdae006f1032}], basedOn=[{reference=8ee73df9-b80e-49d9-9fd1-8a5b6864178f, type=ServiceRequest}], status=requested, intent=order, authoredOn=2021-11-11T08:27:55+00:00, lastModified=2021-11-11T08:27:55+00:00}}]";
        Exchange exchange = new DefaultExchange(camelContext);

        // replay
        producerTemplate.send("direct:fetch-servicerequest-tasks-from-openmrs", exchange);

        // verify
        authenticateToOpenmrs.assertExchangeReceived(0);
        fetchTasksEndpoint.assertIsSatisfied();
        assertEquals(expectedBody, exchange.getIn().getBody().toString());
    }

    private void setupExpectations() {
        fetchTasksEndpoint.whenAnyExchangeReceived(
                exchange -> exchange.getIn()
                        .setBody(
                                "{\"resourceType\":\"Bundle\",\"id\":\"481814d7-436d-43ee-aa1e-5ebc4b10441a\",\"meta\":{\"lastUpdated\":\"2021-11-11T17:21:33.783+00:00\"},\"type\":\"searchset\",\"total\":1,\"link\":[{\"relation\":\"self\",\"url\":\"http://openmrs:8080/openmrs/ws/fhir2/R4/Task?status=requested%2Caccepted\"}],\"entry\":[{\"fullUrl\":\"http://openmrs:8080/openmrs/ws/fhir2/R4/Task/77ee5f8b-c9ec-4d1f-b3a3-fdae006f1032\",\"resource\":{\"resourceType\":\"Task\",\"id\":\"77ee5f8b-c9ec-4d1f-b3a3-fdae006f1032\",\"text\":{\"status\":\"generated\",\"div\":\"<div />\"},\"identifier\":[{\"system\":\"http://fhir.openmrs.org/ext/task/identifier\",\"value\":\"77ee5f8b-c9ec-4d1f-b3a3-fdae006f1032\"}],\"basedOn\":[{\"reference\":\"8ee73df9-b80e-49d9-9fd1-8a5b6864178f\",\"type\":\"ServiceRequest\"}],\"status\":\"requested\",\"intent\":\"order\",\"authoredOn\":\"2021-11-11T08:27:55+00:00\",\"lastModified\":\"2021-11-11T08:27:55+00:00\"}}]}"));
        fetchTasksEndpoint.expectedHeaderReceived(Exchange.HTTP_METHOD, "GET");
    }
}
