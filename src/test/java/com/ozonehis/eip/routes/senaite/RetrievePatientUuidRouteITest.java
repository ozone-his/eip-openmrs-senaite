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
import org.openmrs.eip.mysql.watcher.Event;
import org.springframework.beans.factory.annotation.Value;

@MockEndpoints
public class RetrievePatientUuidRouteITest extends BaseCamelRoutesTest {

    @EndpointInject(value = "mock:selectSqlEndpoint")
    private MockEndpoint selectSqlEndpoint;

    @Value("${bahmni.test.orderType.uuid}")
    private String bahmniTestOrderTypeUuid;

    @BeforeEach
    public void setup() throws Exception {
        loadXmlRoutesInDirectory("camel", "retrieve-patient-uuid-from-openmrs-route.xml");

        advise("retrieve-patient-uuid-from-openmrs", new AdviceWithRouteBuilder() {
            @Override
            public void configure() {
                weaveByToString(
                                "DynamicTo[sql:SELECT uuid FROM person WHERE person_id = (SELECT t.${exchangeProperty.lookUpColumn} FROM ${exchangeProperty.event.tableName} t WHERE t.uuid = '${exchangeProperty.event.identifier}')?dataSource=openmrsDataSource]")
                        .replace()
                        .toD("mock:selectSqlEndpoint");
            }
        });

        selectSqlEndpoint.whenAnyExchangeReceived(
                exchange -> exchange.getIn().setBody("[{\"uuid\": \"some-patient-uuid\"}]"));
    }

    @AfterEach
    public void reset() throws Exception {
        selectSqlEndpoint.reset();
    }

    @Test
    public void shouldProcessPatientUuidFromPatientIdentifierChangeEvent() throws Exception {
        // setup
        Event event = new Event();
        event.setTableName("patient_identifier");
        event.setIdentifier("some-uuid");
        event.setOperation("u");
        event.setPrimaryKeyId("1");
        Exchange exchange = new DefaultExchange(camelContext);
        exchange.setProperty("event", event);
        selectSqlEndpoint.expectedPropertyReceived("lookUpColumn", "patient_id");

        // replay
        producerTemplate.send("direct:retrieve-patient-uuid-from-openmrs", exchange);

        // verify
        selectSqlEndpoint.assertIsSatisfied();
    }

    @Test
    public void shouldProcessPatientUuidFromPatientNameChangeEvent() throws Exception {
        // setup
        Event event = new Event();
        event.setTableName("person_name");
        event.setIdentifier("some-uuid");
        event.setOperation("u");
        event.setPrimaryKeyId("1");
        Exchange exchange = new DefaultExchange(camelContext);
        exchange.setProperty("event", event);
        selectSqlEndpoint.expectedPropertyReceived("lookUpColumn", "person_id");

        // replay
        producerTemplate.send("direct:retrieve-patient-uuid-from-openmrs", exchange);

        // verify
        selectSqlEndpoint.assertIsSatisfied();
    }
}
