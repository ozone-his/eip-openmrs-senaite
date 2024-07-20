/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openmrs.senaite.processors;

import ca.uhn.fhir.context.FhirContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.ozonehis.eip.openmrs.senaite.handlers.openmrs.DiagnosticReportHandler;
import com.ozonehis.eip.openmrs.senaite.handlers.openmrs.EncounterHandler;
import com.ozonehis.eip.openmrs.senaite.handlers.openmrs.ObservationHandler;
import com.ozonehis.eip.openmrs.senaite.handlers.openmrs.ServiceRequestHandler;
import com.ozonehis.eip.openmrs.senaite.handlers.openmrs.TaskHandler;
import com.ozonehis.eip.openmrs.senaite.handlers.senaite.AnalysesHandler;
import com.ozonehis.eip.openmrs.senaite.handlers.senaite.AnalysisRequestHandler;
import com.ozonehis.eip.openmrs.senaite.model.analyses.AnalysesDetails;
import com.ozonehis.eip.openmrs.senaite.model.analysisRequest.Analyses;
import com.ozonehis.eip.openmrs.senaite.model.analysisRequest.AnalysisRequestResponse;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.CamelExecutionException;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.ProducerTemplate;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Observation;
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

    @Autowired
    private EncounterHandler encounterHandler;

    @Autowired
    private AnalysesHandler analysesHandler;

    @Autowired
    private ObservationHandler observationHandler;

    @Autowired
    private DiagnosticReportHandler diagnosticReportHandler;

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
                ServiceRequest serviceRequest = serviceRequestHandler.getServiceRequestByID(
                        producerTemplate, task.getBasedOn().get(0).getReference());
                if (serviceRequest.getStatus() == ServiceRequest.ServiceRequestStatus.REVOKED) {
                    Task rejectedTask = taskHandler.updateTask(
                            producerTemplate, taskHandler.markTaskRejected(task), task.getIdPart());
                    log.info("TaskProcessor: Rejected Task {}", rejectedTask);
                } else {
                    AnalysisRequestResponse analysisRequest =
                            analysisRequestHandler.getAnalysisRequestResponseByClientIDAndClientSampleID(
                                    producerTemplate,
                                    serviceRequest.getSubject().getReference().split("/")[1],
                                    task.getBasedOn().get(0).getReference());
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
                            log.info("TaskProcessor: Creating ServiceRequest results in OpenMRS {}", analysisRequest);
                            createResultsInOpenMRS(producerTemplate, serviceRequest, analyses);
                        } else {
                            log.info(
                                    "TaskProcessor: Nothing to update for task {} with status {}",
                                    task,
                                    task.getStatus());
                        }
                        if (analysisRequestTaskStatus != null
                                && !analysisRequestTaskStatus.equalsIgnoreCase(
                                        task.getStatus().toString())) {
                            Task updatedTask = taskHandler.updateTask(
                                    producerTemplate,
                                    taskHandler.updateTaskStatus(task, analysisRequestTaskStatus),
                                    task.getIdPart());
                            log.info("TaskProcessor: Updated Task {}", updatedTask);
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

    private void createResultsInOpenMRS(
            ProducerTemplate producerTemplate, ServiceRequest serviceRequest, Analyses[] analyses)
            throws JsonProcessingException {
        // TODO: Fetch typeID from config
        Encounter resultEncounter = encounterHandler.getEncounterByTypeAndSubject(
                producerTemplate,
                "3596fafb-6f6f-4396-8c87-6e63a0f1bd71",
                serviceRequest.getSubject().getReference().split("/")[1]);
        if (resultEncounter != null
                && resultEncounter.getPeriod().getStart().getTime()
                        == serviceRequest.getOccurrencePeriod().getStart().getTime()) {
            // Result Encounter exists
            log.info(
                    "TaskProcessor: Result Encounter {} exists for serviceRequest id {}",
                    resultEncounter.getId(),
                    serviceRequest.getId());
            saveObservationAndDiagnosticReport(producerTemplate, serviceRequest, analyses, resultEncounter);
        } else {
            String encounterID = serviceRequest.getEncounter().getReference().split("/")[1];
            Encounter orderEncounter = encounterHandler.getEncounterByEncounterID(producerTemplate, encounterID);
            Encounter savedResultEncounter = encounterHandler.sendEncounter(
                    producerTemplate, encounterHandler.buildLabResultEncounter(orderEncounter));
            log.info("TaskProcessor: savedResultEncounter id {}", savedResultEncounter.getIdPart());
            saveObservationAndDiagnosticReport(producerTemplate, serviceRequest, analyses, savedResultEncounter);

            log.info("TaskProcessor: Completed saving results for service request {}", serviceRequest.getId());
        }
    }

    private void saveObservationAndDiagnosticReport(
            ProducerTemplate producerTemplate,
            ServiceRequest serviceRequest,
            Analyses[] analyses,
            Encounter savedResultEncounter)
            throws JsonProcessingException {
        String subjectID = serviceRequest.getSubject().getReference().split("/")[1];
        ArrayList<String> observationUuids = new ArrayList<>();
        for (Analyses analysis : analyses) {
            log.info("TaskProcessor: analysis {} and analyses {}", analysis, analyses);
            AnalysesDetails resultAnalyses =
                    analysesHandler.getAnalysesByAnalysesApiUrl(producerTemplate, analysis.getAnalysesApiUrl());
            String analysesDescription = resultAnalyses.getDescription();
            String conceptUuid = analysesDescription.substring(
                    analysesDescription.lastIndexOf("(") + 1, analysesDescription.lastIndexOf(")"));

            Observation savedObservation = observationHandler.getObservationByCodeSubjectEncounterAndDate(
                    producerTemplate,
                    conceptUuid,
                    subjectID,
                    savedResultEncounter.getIdPart(),
                    resultAnalyses.getResultCaptureDate());
            log.info("TaskProcessor: Fetched Observation {}", savedObservation);
            if (savedObservation == null || savedObservation.getId().isEmpty()) {
                // Create result Observation
                savedObservation = observationHandler.sendObservation(
                        producerTemplate,
                        observationHandler.buildResultObservation(
                                savedResultEncounter,
                                conceptUuid,
                                resultAnalyses.getResult(),
                                resultAnalyses.getResultCaptureDate()));
                log.info("TaskProcessor: Saved Observation {}", savedObservation);
            }
            observationUuids.add(savedObservation.getIdPart());
        }
        DiagnosticReport savedDiagnosticReport = diagnosticReportHandler.sendDiagnosticReport(
                producerTemplate,
                diagnosticReportHandler.buildDiagnosticReport(
                        observationUuids, serviceRequest, savedResultEncounter.getIdPart()));
        log.info(
                "TaskProcessor: Saved DiagnosticReport {} for serviceRequest {}",
                savedDiagnosticReport.getIdPart(),
                serviceRequest.getIdPart());
    }
}
