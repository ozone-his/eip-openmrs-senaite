package com.ozonehis.eip.routes.senaite;

import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.spring.junit5.MockEndpoints;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openmrs.eip.mysql.watcher.Event;
import org.springframework.beans.factory.annotation.Value;

@MockEndpoints
public class WriteBahmniOrderAsOpenmrsOrderRouteITest extends BaseCamelRoutesTest {

    @EndpointInject(value = "mock:authenticateToOpenmrsRoute")
    private MockEndpoint authenticateToOpenmrs;

    @EndpointInject(value = "mock:labOrderEndpoint")
    private MockEndpoint labOrderEndpoint;

    @EndpointInject(value = "mock:insertSqlEndpoint")
    private MockEndpoint insertSqlEndpoint;

    @EndpointInject(value = "mock:selectSqlEndpoint")
    private MockEndpoint selectSqlEndpoint;

    @Value("${bahmni.test.orderType.uuid}")
    private String bahmniTestOrderTypeUuid;

    @BeforeEach
    public void setup() throws Exception {
        loadXmlRoutesInDirectory("camel", "write-bahmniorder-as-openmrsorder-route.xml");

        advise("write-bahmniorder-as-openmrsorder", new AdviceWithRouteBuilder() {
            @Override
            public void configure() {
                weaveByToString("To[direct:authenticate-to-openmrs]").replace().toD("mock:authenticateToOpenmrsRoute");
                weaveByToString(".*/ws/rest/v1/order/\\$\\{exchangeProperty.lab-order-uuid\\}]")
                        .replace()
                        .toD("mock:labOrderEndpoint");
                ;
                weaveByToString(
                                "DynamicTo[sql:INSERT INTO test_order(order_id) VALUES (${exchangeProperty.lab-order-id})?dataSource=openmrsDataSource]")
                        .replace()
                        .toD("mock:insertSqlEndpoint");
                weaveByToString(
                                "DynamicTo[sql:SELECT COUNT(*) total FROM test_order WHERE order_id=${exchangeProperty.lab-order-id}?dataSource=openmrsDataSource]")
                        .replace()
                        .toD("mock:selectSqlEndpoint");
            }
        });

        labOrderEndpoint.whenAnyExchangeReceived(exchange ->
                exchange.getIn().setBody("{\"orderType\": {\"uuid\": \"" + bahmniTestOrderTypeUuid + "\"}}"));
        labOrderEndpoint.expectedHeaderReceived(Exchange.HTTP_METHOD, "GET");
        labOrderEndpoint.expectedPropertyReceived("lab-order-uuid", "order-uuid");

        insertSqlEndpoint.expectedPropertyReceived("lab-order-id", "1");

        selectSqlEndpoint.whenAnyExchangeReceived(exchange -> exchange.getIn().setBody("{\"total\": 0}"));
        selectSqlEndpoint.expectedPropertyReceived("lab-order-id", "1");
    }

    @Test
    public void shouldCreateTestOrderFromOrder() throws Exception {
        // setup
        Event event = new Event();
        event.setTableName("orders");
        event.setIdentifier("order-uuid");
        event.setOperation("c");
        event.setPrimaryKeyId("1");

        // replay
        producerTemplate.sendBody("direct:write-bahmniorder-as-openmrsorder", event);

        // verify
        authenticateToOpenmrs.assertExchangeReceived(0);
        labOrderEndpoint.assertIsSatisfied();
        insertSqlEndpoint.assertIsSatisfied();
        selectSqlEndpoint.assertIsSatisfied();
    }
}
