/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openmrs.senaite.schedulers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import com.ozonehis.eip.openmrs.senaite.handlers.openmrs.TaskHandler;
import com.ozonehis.eip.openmrs.senaite.handlers.senaite.AnalysisRequestHandler;
import com.ozonehis.eip.openmrs.senaite.model.analysisRequest.response.CatalogSample;
import com.ozonehis.eip.openmrs.senaite.processors.TaskProcessor;
import com.ozonehis.eip.openmrs.senaite.routes.analysisrequest.AnalysisRequestCatalogQuery;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.apache.camel.ProducerTemplate;
import org.hl7.fhir.r4.model.Task;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

class AnalysisRequestChangePollerTest {

    private static final Instant NOW = Instant.parse("2026-10-28T18:00:00Z");

    @Mock
    private AnalysisRequestHandler analysisRequestHandler;

    @Mock
    private TaskHandler taskHandler;

    @Mock
    private TaskProcessor taskProcessor;

    @Mock
    private ProducerTemplate producerTemplate;

    @InjectMocks
    private AnalysisRequestChangePoller poller;

    private AutoCloseable mocksCloser;

    @BeforeEach
    void setUp() {
        mocksCloser = openMocks(this);
        poller.setClock(Clock.fixed(NOW, ZoneOffset.UTC));
        poller.setLookback("PT24H");
        when(taskHandler.doesTaskExists(any())).thenReturn(true);
    }

    @AfterEach
    void tearDown() throws Exception {
        mocksCloser.close();
    }

    @Test
    void shouldAdvanceTheWatermarkPastASyncedSampleAndSkipACompletedTask() throws Exception {
        CatalogSample published = sample("done-sample", "published", "2026-10-28T15:00:00+00:00", null);
        CatalogSample completed = sample("old-sample", "published", "2026-10-28T15:30:00+00:00", null);
        when(analysisRequestHandler.searchAnalysisRequests(any(), any())).thenAnswer(invocation -> {
            String query = invocation.getArgument(1, String.class);
            if (query.contains("review_state=published") && query.contains("b_start=0")) {
                return List.of(published, completed);
            }
            return List.of();
        });
        when(taskHandler.getTaskByServiceRequestID("done-sample")).thenReturn(task(Task.TaskStatus.ACCEPTED));
        when(taskHandler.getTaskByServiceRequestID("old-sample")).thenReturn(task(Task.TaskStatus.COMPLETED));

        poller.poll(producerTemplate);

        verify(taskProcessor, times(1))
                .syncCatalogSample(eq(producerTemplate), any(), eq("done-sample"), eq("published"));
        verify(taskProcessor, never()).syncCatalogSample(any(), any(), eq("old-sample"), any());
        assertEquals("2026-10-28T15:30:00", poller.getPublishedWatermark());
        assertTrue(poller.getRetrySampleIds().isEmpty());
    }

    @Test
    void shouldKeepAFailedSampleForRetryWithoutHoldingNewerPublishes() throws Exception {
        CatalogSample failed = sample("bad-sample", "published", "2026-10-28T15:00:00+00:00", null);
        CatalogSample synced = sample("good-sample", "published", "2026-10-28T16:00:00+00:00", null);
        when(analysisRequestHandler.searchAnalysisRequests(any(), any())).thenAnswer(invocation -> {
            String query = invocation.getArgument(1, String.class);
            if (query.contains("review_state=published")
                    && query.contains("b_start=0")
                    && query.contains("2026-10-27T18%3A00%3A00")) {
                return List.of(failed, synced);
            }
            return List.of();
        });
        when(taskHandler.getTaskByServiceRequestID(any())).thenReturn(task(Task.TaskStatus.ACCEPTED));
        doThrow(new IllegalStateException("obs 400"))
                .when(taskProcessor)
                .syncCatalogSample(any(), any(), eq("bad-sample"), eq("published"));

        poller.poll(producerTemplate);

        assertEquals("2026-10-28T16:00:00", poller.getPublishedWatermark());
        assertEquals(List.of("bad-sample"), List.copyOf(poller.getRetrySampleIds()));

        when(taskHandler.getTaskByServiceRequestID("bad-sample")).thenReturn(task(Task.TaskStatus.ACCEPTED));
        poller.poll(producerTemplate);

        verify(taskProcessor, times(2)).syncCatalogSample(any(), any(), eq("bad-sample"), eq("published"));
        verify(taskProcessor, times(1)).syncCatalogSample(any(), any(), eq("good-sample"), eq("published"));
        assertTrue(poller.getRetrySampleIds().contains("bad-sample"));
    }

    @Test
    void shouldPageUntilAShortPage() throws Exception {
        List<CatalogSample> firstPage = new ArrayList<>();
        for (int i = 0; i < AnalysisRequestCatalogQuery.PAGE_SIZE; i++) {
            firstPage.add(sample("sample-" + i, "published", "2026-10-28T15:00:00+00:00", null));
        }
        CatalogSample last = sample("sample-last", "published", "2026-10-28T16:10:00+00:00", null);
        when(analysisRequestHandler.searchAnalysisRequests(any(), any())).thenAnswer(invocation -> {
            String query = invocation.getArgument(1, String.class);
            if (!query.contains("review_state=published")) {
                return List.of();
            }
            if (query.contains("b_start=0")) {
                return firstPage;
            }
            if (query.contains("b_start=50")) {
                return List.of(last);
            }
            return List.of();
        });
        when(taskHandler.getTaskByServiceRequestID(any())).thenReturn(task(Task.TaskStatus.REQUESTED));

        poller.poll(producerTemplate);

        verify(taskProcessor, times(AnalysisRequestCatalogQuery.PAGE_SIZE + 1))
                .syncCatalogSample(eq(producerTemplate), any(), any(), eq("published"));
        assertEquals("2026-10-28T16:10:00", poller.getPublishedWatermark());
    }

    @Test
    void shouldSkipASampleThatIsAlreadyAccepted() throws Exception {
        CatalogSample received = sample("received-sample", "sample_received", null, "2026-10-28T12:00:00+00:00");
        when(analysisRequestHandler.searchAnalysisRequests(any(), any())).thenAnswer(invocation -> {
            String query = invocation.getArgument(1, String.class);
            if (query.contains("review_state=sample_received") && query.contains("b_start=0")) {
                return List.of(received);
            }
            return List.of();
        });
        when(taskHandler.getTaskByServiceRequestID("received-sample")).thenReturn(task(Task.TaskStatus.ACCEPTED));

        poller.poll(producerTemplate);

        verify(taskProcessor, never()).syncCatalogSample(any(), any(), eq("received-sample"), any());
        assertEquals("2026-10-28T12:00:00", poller.getReceivedWatermark());
    }

    private static CatalogSample sample(
            String clientSampleId, String reviewState, String datePublished, String dateReceived) {
        CatalogSample sample = new CatalogSample();
        sample.setClientSampleId(clientSampleId);
        sample.setClientId("client-1");
        sample.setReviewState(reviewState);
        sample.setDatePublished(datePublished);
        sample.setDateReceived(dateReceived);
        sample.setUid("uid-" + clientSampleId);
        return sample;
    }

    private static Task task(Task.TaskStatus status) {
        Task task = new Task();
        task.setId("Task/1");
        task.setStatus(status);
        return task;
    }
}
