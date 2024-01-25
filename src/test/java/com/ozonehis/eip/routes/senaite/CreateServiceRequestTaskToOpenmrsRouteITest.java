package com.ozonehis.eip.routes.senaite;

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
public class CreateServiceRequestTaskToOpenmrsRouteITest extends BaseCamelRoutesTest {

    @EndpointInject(value = "mock:authenticateToOpenmrsRoute")
    private MockEndpoint authenticateToOpenmrs;

    @EndpointInject(value = "mock:createTaskOpenmrsEndpoint")
    private MockEndpoint createTaskOpenmrsEndpoint;

    @EndpointInject(value = "mock:searchTaskBasedOnServiceRequestOpenmrsEndpoint")
    private MockEndpoint searchTaskBasedOnServiceRequestOpenmrsEndpoint;

    @BeforeEach
    public void setup() throws Exception {
        loadXmlRoutesInDirectory("camel", "create-servicerequest-task-to-openmrs-route.xml");

        advise("create-servicerequest-task-to-openmrs", new AdviceWithRouteBuilder() {
            @Override
            public void configure() {
                weaveByToString("To[direct:authenticate-to-openmrs]").replace().toD("mock:authenticateToOpenmrsRoute");
                weaveByToString(".*/Task]").replace().toD("mock:createTaskOpenmrsEndpoint");
                weaveByToString(
                                ".*/Task\\?based-on:ServiceRequest=\\$\\{exchangeProperty.lab-order-uuid\\}\\&throwExceptionOnFailure=false]")
                        .replace()
                        .toD("mock:searchTaskBasedOnServiceRequestOpenmrsEndpoint");
            }
        });

        setupExpectations();
    }

    @AfterEach
    public void reset() throws Exception {
        createTaskOpenmrsEndpoint.reset();
        searchTaskBasedOnServiceRequestOpenmrsEndpoint.reset();
    }

    @Test
    public void shouldCreateServiceRequestTaskOnOpenmrsGivenTaskDoesNotExist() throws Exception {
        // setup
        Exchange exchange = new DefaultExchange(camelContext);
        exchange.setProperty("lab-order-uuid", "8ee73df9-b80e-49d9-9fd1-8a5b6864178f");

        // replay
        producerTemplate.send("direct:create-servicerequest-task-to-openmrs", exchange);

        // verify
        authenticateToOpenmrs.assertExchangeReceived(0);
        authenticateToOpenmrs.assertIsSatisfied();
        searchTaskBasedOnServiceRequestOpenmrsEndpoint.assertIsSatisfied();
        createTaskOpenmrsEndpoint.assertIsSatisfied();
    }

    @Test
    public void shouldNotCreateServiceRequestTaskOnOpenmrsGivenTaskExists() throws Exception {
        // setup
        Exchange exchange = new DefaultExchange(camelContext);
        exchange.setProperty("lab-order-uuid", "order-with-corresponding-existing-FHIR-task");

        // replay
        producerTemplate.send("direct:create-servicerequest-task-to-openmrs", exchange);

        // verify
        authenticateToOpenmrs.assertExchangeReceived(0);
        authenticateToOpenmrs.assertIsSatisfied();
        searchTaskBasedOnServiceRequestOpenmrsEndpoint.assertIsSatisfied();
        createTaskOpenmrsEndpoint.assertIsNotSatisfied();
    }

    private void setupExpectations() {
        createTaskOpenmrsEndpoint.expectedHeaderReceived(Exchange.HTTP_METHOD, "POST");
        createTaskOpenmrsEndpoint.expectedBodiesReceived(
                "{\"resourceType\": \"Task\", \"status\": \"requested\", \"intent\": \"order\", \"basedOn\": [{\"reference\":\"8ee73df9-b80e-49d9-9fd1-8a5b6864178f\", \"type\": \"ServiceRequest\"}]}");
        createTaskOpenmrsEndpoint.expectedPropertyReceived("lab-order-uuid", "8ee73df9-b80e-49d9-9fd1-8a5b6864178f");
        searchTaskBasedOnServiceRequestOpenmrsEndpoint.whenAnyExchangeReceived(exchange -> {
            if ("8ee73df9-b80e-49d9-9fd1-8a5b6864178f".equals(exchange.getProperty("lab-order-uuid"))) {
                exchange.getIn().setBody("{\"total\":0,\"rest-of-the-FHIR-bundle-payload\": {}}");
            } else {
                exchange.getIn().setBody("{\"total\":1,\"rest-of-the-FHIR-bundle-payload\": {}}");
            }
        });
    }
}
