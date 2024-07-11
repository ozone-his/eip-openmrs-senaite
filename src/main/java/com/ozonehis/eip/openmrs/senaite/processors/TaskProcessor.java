/*
 * Copyright © 2024, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openmrs.senaite.processors;

import ca.uhn.fhir.context.FhirContext;
import com.ozonehis.eip.openmrs.senaite.Constants;
import com.ozonehis.eip.openmrs.senaite.handlers.openmrs.ServiceRequestHandler;
import com.ozonehis.eip.openmrs.senaite.handlers.openmrs.TaskHandler;
import com.ozonehis.eip.openmrs.senaite.handlers.senaite.AnalysisRequestHandler;
import com.ozonehis.eip.openmrs.senaite.model.analysisRequest.Analyses;
import com.ozonehis.eip.openmrs.senaite.model.analysisRequest.AnalysisRequestResponse;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.CamelExecutionException;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.ProducerTemplate;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.hl7.fhir.r4.model.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Setter
@Getter
@Component
public class TaskProcessor implements Processor {

    @Autowired
    private ServiceRequestHandler serviceRequestHandler;

    @Autowired
    private TaskHandler taskHandler;

    @Autowired
    private AnalysisRequestHandler analysisRequestHandler;

    @Override
    public void process(Exchange exchange) {
        try (ProducerTemplate producerTemplate = exchange.getContext().createProducerTemplate()) {
            String body = exchange.getMessage().getBody(String.class);
            log.info("TaskProcessor: Body {}", body);
            FhirContext ctx = FhirContext.forR4();
            Bundle bundle = ctx.newJsonParser().parseResource(Bundle.class, body);
            List<Bundle.BundleEntryComponent> entries = bundle.getEntry();
            for (Bundle.BundleEntryComponent entry : entries) {
                Task task = null;
                Resource resource = entry.getResource();
                if (resource instanceof Task) {
                    task = (Task) resource;
                }

                if (task == null || task.getStatus() == null) {
                    continue;
                }
                log.info("TaskProcessor: Task {}", task);
                Map<String, Object> headers = new HashMap<>();
                headers.put(
                        Constants.HEADER_SERVICE_REQUEST_ID,
                        task.getBasedOn().get(0).getReference());
                ServiceRequest serviceRequest = serviceRequestHandler.getServiceRequest(producerTemplate, headers);
                if (serviceRequest.getStatus()
                        == ServiceRequest.ServiceRequestStatus.REVOKED) { // TODO: Check for voided & deleted
                    log.info("TaskProcessor: ServiceRequest is voided or deleted {}", task);
                    Task rejectTask = new Task();
                    task.setId(task.getId());
                    task.setStatus(Task.TaskStatus.REJECTED);
                    task.setIntent(Task.TaskIntent.ORDER);
                    Task rejectedTask = taskHandler.updateTask(producerTemplate, rejectTask);
                    log.info("TaskProcessor: Rejected Task {}", rejectedTask);
                } else {
                    headers.put(
                            Constants.HEADER_CLIENT_SAMPLE_ID,
                            task.getBasedOn().get(0).getReference());
                    headers.put(
                            Constants.HEADER_CLIENT_ID,
                            serviceRequest.getSubject().getReference().split("/")[1]);
                    AnalysisRequestResponse analysisRequest =
                            analysisRequestHandler.getAnalysisRequestResponse(producerTemplate, headers);
                    log.info("TaskProcessor: AnalysisRequestResponse {}", analysisRequest);
                    if (analysisRequest != null
                            && analysisRequest.getAnalysisRequestItems() != null
                            && !analysisRequest.getAnalysisRequestItems().isEmpty()) {
                        Analyses[] analyses =
                                analysisRequest.getAnalysisRequestItems().get(0).getAnalyses();
                        String analysisRequestTaskStatus =
                                getTaskStatusCorrespondingToAnalysisRequestStatus(analysisRequest);
                        if (analysisRequestTaskStatus != null
                                && analysisRequestTaskStatus.equalsIgnoreCase("completed")) {
                            // TODO: Create ServiceRequest results in OpenMRS
                            log.info("TaskProcessor: Creating ServiceRequest results in OpenMRS {}", analysisRequest);
                        } else if (analysisRequestTaskStatus != null
                                && !analysisRequestTaskStatus.equalsIgnoreCase(
                                task.getStatus().toString())) {
                            Task updateTask = new Task();
                            updateTask.setId(task.getId());
                            updateTask.setIntent(Task.TaskIntent.ORDER);
                            updateTask.setStatus(Task.TaskStatus.fromCode(analysisRequestTaskStatus));
                            Task updatedTask = taskHandler.updateTask(producerTemplate, task);
                            log.info("TaskProcessor: Updated Task {}", updatedTask);
                        } else {
                            log.info("TaskProcessor: Nothing to update for task {} with status {}", task, task.getStatus());
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new CamelExecutionException("Error processing Task", exchange, e);
        }
    }

    private String getTaskStatusCorrespondingToAnalysisRequestStatus(AnalysisRequestResponse analysisRequestResponse) {
        String analysisRequestStatus =
                analysisRequestResponse.getAnalysisRequestItems().get(0).getReviewState();
        if (analysisRequestStatus.equalsIgnoreCase("sample_due")) {
            return "requested";
        } else if (analysisRequestStatus.equalsIgnoreCase("sample_received")) {
            return "accepted";
        } else if (analysisRequestStatus.equalsIgnoreCase("published")) {
            return "completed";
        } else if (analysisRequestStatus.equalsIgnoreCase("cancelled")) {
            return "rejected";
        }
        return null;
    }
}
