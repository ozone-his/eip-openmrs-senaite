/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openmrs.senaite.routes.openmrsFhirTask;

import static org.apache.camel.builder.AdviceWith.adviceWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import ca.uhn.fhir.context.FhirContext;
import com.ozonehis.eip.openmrs.senaite.converters.ResourceConverter;
import com.ozonehis.eip.openmrs.senaite.processors.TaskProcessor;
import com.ozonehis.eip.openmrs.senaite.routes.TaskRouting;
import java.util.ArrayList;
import java.util.List;
import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.spring.junit5.CamelSpringTestSupport;
import org.apache.camel.test.spring.junit5.UseAdviceWith;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Task;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.StaticApplicationContext;

@UseAdviceWith
class GetOpenmrsFhirTaskByStatusRouteTest extends CamelSpringTestSupport {
    private static final String GET_BY_STATUS_TASK_ROUTE = "direct:openmrs-get-task-by-status-route";

    private final TaskProcessor taskProcessor = mock(TaskProcessor.class);

    @Override
    protected RoutesBuilder createRouteBuilder() {
        return new GetOpenmrsFhirTaskByStatusRoute();
    }

    @Override
    protected AbstractApplicationContext createApplicationContext() {
        return new StaticApplicationContext();
    }

    @Override
    public boolean isUseAdviceWith() {
        return true;
    }

    @BeforeEach
    public void setup() throws Exception {
        TaskRouting taskRouting = new TaskRouting();
        taskRouting.setTaskProcessor(taskProcessor);
        taskRouting.setResourceConverter(new ResourceConverter());
        context.addRoutes(taskRouting);
        adviceWith("openmrs-get-task-by-status-route", context, new AdviceWithRouteBuilder() {

            @Override
            public void configure() {
                weaveByToUri("fhir:*").replace().to("mock:get-task-by-status-route");
            }
        });
        context.start();

        Endpoint defaultEndpoint = context.getEndpoint(GET_BY_STATUS_TASK_ROUTE);
        template.setDefaultEndpoint(defaultEndpoint);
    }

    @Test
    public void shouldProcessEachPageBeforeFetchingTheNextPage() throws Exception {
        String nextPageUrl = "https://openmrs.example/ws/fhir2/R4?_getpages=page-id&_getpagesoffset=10&_count=10";
        String lastPageUrl = "https://openmrs.example/ws/fhir2/R4?_getpages=page-id&_getpagesoffset=20&_count=10";
        Bundle firstPage = bundleWithTask("first_task");
        firstPage.addLink().setRelation(Bundle.LINK_NEXT).setUrl(nextPageUrl);
        Bundle secondPage = bundleWithTask("second_task");
        secondPage.addLink().setRelation(Bundle.LINK_NEXT).setUrl(lastPageUrl);
        Bundle lastPage = bundleWithTask("last_task");
        List<String> events = new ArrayList<>();

        doAnswer(invocation -> {
                    Exchange exchange = invocation.getArgument(0);
                    Bundle page = exchange.getMessage().getBody(Bundle.class);
                    assertEquals(1, page.getEntry().size());
                    events.add(
                            "process:" + page.getEntryFirstRep().getResource().getIdPart());
                    // Processing may change the body; pagination must already have been saved.
                    exchange.getMessage().setBody(null);
                    return null;
                })
                .when(taskProcessor)
                .process(any(Exchange.class));

        MockEndpoint mockEndpoint = getMockEndpoint("mock:get-task-by-status-route");
        mockEndpoint.expectedMessageCount(3);
        mockEndpoint.setResultWaitTime(100);
        mockEndpoint.whenAnyExchangeReceived(exchange -> {
            String requestedUrl = exchange.getProperty("nextPageUrl", String.class);
            events.add("fetch:" + requestedUrl);
            Bundle response = nextPageUrl.equals(requestedUrl)
                    ? secondPage
                    : lastPageUrl.equals(requestedUrl) ? lastPage : firstPage;
            exchange.getMessage().setBody(FhirContext.forR4().newJsonParser().encodeResourceToString(response));
        });

        Exchange result = template.request(
                "direct:poll-senaite", exchange -> exchange.getMessage().setBody(null));

        mockEndpoint.assertIsSatisfied();
        assertNull(result.getException());
        assertNull(result.getProperty("nextPageUrl"));
        assertEquals(
                List.of(
                        "fetch:" + GetOpenmrsFhirTaskByStatusRoute.GET_BY_STATUS_ENDPOINT,
                        "process:first_task",
                        "fetch:" + nextPageUrl,
                        "process:second_task",
                        "fetch:" + lastPageUrl,
                        "process:last_task"),
                events);
        verify(taskProcessor, times(3)).process(any(Exchange.class));
    }

    @Test
    public void shouldProcessASinglePageOnce() throws Exception {
        assertSinglePageProcessed(bundleWithTask("only_task"));
    }

    @Test
    public void shouldStopAfterAnEmptyPageWithoutANextLink() throws Exception {
        assertSinglePageProcessed(new Bundle());
    }

    @Test
    public void shouldNotFetchTheNextPageWhenProcessingFails() throws Exception {
        Bundle firstPage = bundleWithTask("first_task");
        firstPage.addLink().setRelation(Bundle.LINK_NEXT).setUrl("https://openmrs.example/next-page");
        IllegalStateException failure = new IllegalStateException("Task processing failed");
        doThrow(failure).when(taskProcessor).process(any(Exchange.class));
        MockEndpoint mockEndpoint = getMockEndpoint("mock:get-task-by-status-route");
        mockEndpoint.expectedMessageCount(1);
        mockEndpoint.whenAnyExchangeReceived(exchange -> exchange.getMessage()
                .setBody(FhirContext.forR4().newJsonParser().encodeResourceToString(firstPage)));

        Exchange result = template.request(
                "direct:poll-senaite", exchange -> exchange.getMessage().setBody(null));

        mockEndpoint.assertIsSatisfied();
        assertSame(failure, result.getException());
        verify(taskProcessor).process(any(Exchange.class));
    }

    @Test
    public void shouldOnlyFetchOnePageWithoutProcessingTasks() throws Exception {
        String nextPageUrl = "https://openmrs.example/ws/fhir2/R4?_getpages=page-id&_getpagesoffset=10&_count=10";
        Bundle firstPage = bundleWithTask("first_task");
        firstPage.addLink().setRelation(Bundle.LINK_NEXT).setUrl(nextPageUrl);
        MockEndpoint mockEndpoint = getMockEndpoint("mock:get-task-by-status-route");
        mockEndpoint.expectedMessageCount(1);
        mockEndpoint.whenAnyExchangeReceived(exchange -> {
            assertEquals(
                    GetOpenmrsFhirTaskByStatusRoute.GET_BY_STATUS_ENDPOINT,
                    exchange.getProperty("nextPageUrl", String.class));
            exchange.getMessage().setBody(FhirContext.forR4().newJsonParser().encodeResourceToString(firstPage));
        });

        Exchange result = template.request(
                GET_BY_STATUS_TASK_ROUTE,
                exchange ->
                        exchange.setProperty("nextPageUrl", GetOpenmrsFhirTaskByStatusRoute.GET_BY_STATUS_ENDPOINT));

        mockEndpoint.assertIsSatisfied();
        assertNull(result.getException());
        assertEquals(nextPageUrl, result.getProperty("nextPageUrl"));
        assertEquals(
                "first_task",
                result.getMessage()
                        .getBody(Bundle.class)
                        .getEntryFirstRep()
                        .getResource()
                        .getIdPart());
        verifyNoInteractions(taskProcessor);
    }

    private void assertSinglePageProcessed(Bundle page) throws Exception {
        MockEndpoint mockEndpoint = getMockEndpoint("mock:get-task-by-status-route");
        mockEndpoint.expectedMessageCount(1);
        mockEndpoint.whenAnyExchangeReceived(exchange -> exchange.getMessage()
                .setBody(FhirContext.forR4().newJsonParser().encodeResourceToString(page)));
        doAnswer(invocation -> {
                    Bundle processedPage =
                            ((Exchange) invocation.getArgument(0)).getMessage().getBody(Bundle.class);
                    assertEquals(
                            page.getEntry().size(), processedPage.getEntry().size());
                    return null;
                })
                .when(taskProcessor)
                .process(any(Exchange.class));

        Exchange result = template.request(
                "direct:poll-senaite", exchange -> exchange.getMessage().setBody(null));

        mockEndpoint.assertIsSatisfied();
        assertNull(result.getException());
        assertNull(result.getProperty("nextPageUrl"));
        verify(taskProcessor).process(any(Exchange.class));
    }

    private Bundle bundleWithTask(String taskId) {
        Task task = new Task();
        task.setId(taskId);
        task.setIntent(Task.TaskIntent.ORDER);
        task.addBasedOn().setReference("service_request_id").setType("ServiceRequest");
        Bundle bundle = new Bundle();
        bundle.addEntry().setResource(task);
        return bundle;
    }
}
