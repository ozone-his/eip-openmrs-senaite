/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openmrs.senaite.schedulers;

import com.ozonehis.eip.openmrs.senaite.handlers.openmrs.TaskHandler;
import com.ozonehis.eip.openmrs.senaite.handlers.senaite.AnalysisRequestHandler;
import com.ozonehis.eip.openmrs.senaite.model.analysisRequest.response.CatalogSample;
import com.ozonehis.eip.openmrs.senaite.processors.TaskProcessor;
import com.ozonehis.eip.openmrs.senaite.routes.analysisrequest.AnalysisRequestCatalogQuery;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.ProducerTemplate;
import org.hl7.fhir.r4.model.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AnalysisRequestChangePoller {

    static final int MAX_RETRIES = 100;

    static final int MAX_PAGES = 200;

    private static final DateTimeFormatter WATERMARK_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss").withZone(ZoneOffset.UTC);

    private static final String PUBLISHED = "published";

    private static final String SAMPLE_RECEIVED = "sample_received";

    @Autowired
    private AnalysisRequestHandler analysisRequestHandler;

    @Autowired
    private TaskHandler taskHandler;

    @Autowired
    private TaskProcessor taskProcessor;

    @Setter
    private Clock clock = Clock.systemUTC();

    @Value("${task.sync.lookback:PT24H}")
    @Setter
    private String lookback = "PT24H";

    private String publishedWatermark;

    private String receivedWatermark;

    private final Map<String, String> retries = new LinkedHashMap<>();

    public void poll(ProducerTemplate producerTemplate) {
        try {
            ensureWatermarks();
            log.info("Polling SENAITE changes started...");
            Set<String> pendingBeforeScan = Set.copyOf(retries.keySet());
            publishedWatermark = scan(producerTemplate, PUBLISHED, publishedWatermark);
            receivedWatermark = scan(producerTemplate, SAMPLE_RECEIVED, receivedWatermark);
            retry(producerTemplate, pendingBeforeScan);
            log.info(
                    "Polling SENAITE changes completed. publishedWatermark={} receivedWatermark={} retries={}",
                    publishedWatermark,
                    receivedWatermark,
                    retries.size());
        } catch (Exception exception) {
            log.error("Polling SENAITE changes failed", exception);
        }
    }

    public String getPublishedWatermark() {
        return publishedWatermark;
    }

    public String getReceivedWatermark() {
        return receivedWatermark;
    }

    public Set<String> getRetrySampleIds() {
        return Set.copyOf(retries.keySet());
    }

    private void ensureWatermarks() {
        if (publishedWatermark == null || receivedWatermark == null) {
            String start = WATERMARK_FORMAT.format(clock.instant().minus(Duration.parse(lookback)));
            publishedWatermark = start;
            receivedWatermark = start;
        }
    }

    private String scan(ProducerTemplate producerTemplate, String reviewState, String watermark) throws Exception {
        String latest = watermark;
        int offset = 0;
        for (int page = 0; page < MAX_PAGES; page++) {
            List<CatalogSample> samples = analysisRequestHandler.searchAnalysisRequests(
                    producerTemplate, queryFor(reviewState, watermark, offset));
            for (CatalogSample sample : samples) {
                try {
                    syncCatalogSample(producerTemplate, sample.getClientSampleId(), reviewState);
                } catch (Exception exception) {
                    log.error("Failed to sync {} sample {}", reviewState, sample.getClientSampleId(), exception);
                    rememberFailure(sample.getClientSampleId(), reviewState);
                }
                latest = laterWatermark(latest, eventDate(sample, reviewState));
            }
            if (samples.size() < AnalysisRequestCatalogQuery.PAGE_SIZE) {
                return latest;
            }
            offset += samples.size();
        }
        log.warn("Stopped the {} change poll after {} pages", reviewState, MAX_PAGES);
        return latest;
    }

    private void retry(ProducerTemplate producerTemplate, Set<String> pendingIds) {
        for (String clientSampleId : pendingIds) {
            String reviewState = retries.get(clientSampleId);
            if (reviewState == null) {
                continue;
            }
            try {
                syncCatalogSample(producerTemplate, clientSampleId, reviewState);
            } catch (Exception exception) {
                log.error("Failed to retry {} sample {}", reviewState, clientSampleId, exception);
            }
        }
    }

    private void syncCatalogSample(ProducerTemplate producerTemplate, String clientSampleId, String reviewState)
            throws Exception {
        if (clientSampleId == null || clientSampleId.isEmpty()) {
            return;
        }
        Task task = taskHandler.getTaskByServiceRequestID(clientSampleId);
        if (!taskHandler.doesTaskExists(task) || isSettled(task, reviewState)) {
            forgetRetry(clientSampleId, reviewState);
            return;
        }
        taskProcessor.syncCatalogSample(producerTemplate, task, clientSampleId, reviewState);
        forgetRetry(clientSampleId, reviewState);
    }

    private void forgetRetry(String clientSampleId, String reviewState) {
        String pendingReviewState = retries.get(clientSampleId);
        if (PUBLISHED.equalsIgnoreCase(reviewState) || reviewState.equalsIgnoreCase(pendingReviewState)) {
            retries.remove(clientSampleId);
        }
    }

    private void rememberFailure(String clientSampleId, String reviewState) {
        if (clientSampleId == null || clientSampleId.isEmpty()) {
            return;
        }
        String pendingReviewState = retries.get(clientSampleId);
        if (PUBLISHED.equalsIgnoreCase(pendingReviewState) && !PUBLISHED.equalsIgnoreCase(reviewState)) {
            return;
        }
        if (pendingReviewState != null || retries.size() < MAX_RETRIES) {
            retries.put(clientSampleId, reviewState);
        } else {
            log.error("Retry set is full; leaving sample {} for the hourly scan", clientSampleId);
        }
    }

    private static boolean isSettled(Task task, String reviewState) {
        Task.TaskStatus status = task.getStatus();
        if (status == Task.TaskStatus.COMPLETED
                || status == Task.TaskStatus.REJECTED
                || status == Task.TaskStatus.CANCELLED) {
            return true;
        }
        return SAMPLE_RECEIVED.equalsIgnoreCase(reviewState) && status == Task.TaskStatus.ACCEPTED;
    }

    private static String queryFor(String reviewState, String watermark, int offset) {
        if (PUBLISHED.equalsIgnoreCase(reviewState)) {
            return AnalysisRequestCatalogQuery.publishedSince(watermark, offset);
        }
        return AnalysisRequestCatalogQuery.receivedSince(watermark, offset);
    }

    private static String eventDate(CatalogSample sample, String reviewState) {
        if (PUBLISHED.equalsIgnoreCase(reviewState)) {
            return sample.getDatePublished();
        }
        return sample.getDateReceived();
    }

    static String laterWatermark(String current, String catalogDate) {
        Instant instant = parseCatalogDate(catalogDate);
        if (instant == null) {
            return current;
        }
        Instant currentInstant = parseCatalogDate(current);
        if (currentInstant == null || !instant.isBefore(currentInstant)) {
            return WATERMARK_FORMAT.format(instant);
        }
        return current;
    }

    private static Instant parseCatalogDate(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            return OffsetDateTime.parse(value).toInstant();
        } catch (DateTimeParseException ignored) {
            try {
                return LocalDateTime.parse(value).toInstant(ZoneOffset.UTC);
            } catch (DateTimeParseException exception) {
                return null;
            }
        }
    }
}
