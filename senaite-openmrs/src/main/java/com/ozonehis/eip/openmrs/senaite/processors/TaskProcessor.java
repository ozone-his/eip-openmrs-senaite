/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openmrs.senaite.processors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.ozonehis.eip.openmrs.senaite.Constants;
import com.ozonehis.eip.openmrs.senaite.handlers.bahmni.BahmniResultsHandler;
import com.ozonehis.eip.openmrs.senaite.handlers.openmrs.DiagnosticReportHandler;
import com.ozonehis.eip.openmrs.senaite.handlers.openmrs.EncounterHandler;
import com.ozonehis.eip.openmrs.senaite.handlers.openmrs.ObservationHandler;
import com.ozonehis.eip.openmrs.senaite.handlers.openmrs.ServiceRequestHandler;
import com.ozonehis.eip.openmrs.senaite.handlers.openmrs.TaskHandler;
import com.ozonehis.eip.openmrs.senaite.handlers.senaite.AnalysesHandler;
import com.ozonehis.eip.openmrs.senaite.handlers.senaite.AnalysisRequestHandler;
import com.ozonehis.eip.openmrs.senaite.model.analyses.AnalysesDTO;
import com.ozonehis.eip.openmrs.senaite.model.analysisRequest.AnalysisRequestDTO;
import com.ozonehis.eip.openmrs.senaite.model.analysisRequest.response.Analyses;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.ProducerTemplate;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.hl7.fhir.r4.model.Task;
import org.openmrs.eip.EIPException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Setter
@Getter
@NoArgsConstructor
@Component
public class TaskProcessor implements Processor {

    @Value("${results.encounterType.uuid}")
    private String resultEncounterTypeUUID;

    @Value("${run.with.bahmni.emr}")
    private String runWithBahmniEmr;

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

    @Autowired
    private BahmniResultsHandler bahmniResultsHandler;

    @Override
    public void process(Exchange exchange) {
        String taskId = "unavailable";
        try (ProducerTemplate producerTemplate = exchange.getContext().createProducerTemplate()) {
            Bundle bundle = exchange.getMessage().getBody(Bundle.class);
            if (bundle == null) {
                throw new IllegalArgumentException("Cannot process polled Tasks: expected a FHIR Bundle body");
            }
            log.debug(
                    "Processing {} entries from OpenMRS Task polling (exchange ID: {})",
                    bundle.getEntry().size(),
                    exchange.getExchangeId());
            List<Bundle.BundleEntryComponent> entries = bundle.getEntry();
            for (Bundle.BundleEntryComponent entry : entries) {
                taskId = "unavailable";
                Task task = null;
                Resource resource = entry.getResource();
                if (resource instanceof Task) {
                    task = (Task) resource;
                    taskId = task.getIdPart();
                }

                if (!taskHandler.doesTaskExists(task)) {
                    continue;
                }
                if (task.getStatus() == Task.TaskStatus.COMPLETED || task.getStatus() == Task.TaskStatus.CANCELLED) {
                    log.debug("Skipping OpenMRS Task {}: status {} is terminal", task.getIdPart(), task.getStatus());
                    continue;
                }
                if (task.getBasedOn() == null || task.getBasedOn().isEmpty()) {
                    log.warn("Skipping OpenMRS Task {}: no basedOn ServiceRequest reference is present", taskId);
                    continue;
                }
                String taskBasedOnReference = task.getBasedOn().get(0).getReference();
                if (taskBasedOnReference.contains(Constants.SERVICE_REQUEST_PREFIX)) {
                    taskBasedOnReference = taskBasedOnReference.split("/")[1];
                }
                ServiceRequest serviceRequest = serviceRequestHandler.getServiceRequestByID(taskBasedOnReference);
                if (serviceRequest.getStatus() == ServiceRequest.ServiceRequestStatus.REVOKED) {
                    taskHandler.updateTask(taskHandler.markTaskRejected(task), task.getIdPart());
                    log.info("Rejected OpenMRS Task {}: ServiceRequest {} is revoked", taskId, taskBasedOnReference);
                } else {
                    String serviceRequestSubjectID =
                            serviceRequest.getSubject().getReference().split("/")[1];
                    AnalysisRequestDTO analysisRequestDTO =
                            analysisRequestHandler.getAnalysisRequestByClientIDAndClientSampleID(
                                    producerTemplate, serviceRequestSubjectID, taskBasedOnReference);
                    if (analysisRequestHandler.doesAnalysisRequestExists(analysisRequestDTO)) {
                        Analyses[] analyses = analysisRequestDTO.getAnalyses();
                        String analysisRequestTaskStatus =
                                getTaskStatusCorrespondingToAnalysisRequestStatus(analysisRequestDTO);
                        if (analysisRequestTaskStatus != null
                                && analysisRequestTaskStatus.equalsIgnoreCase("completed")) {
                            createResultsInOpenMRS(
                                    producerTemplate, serviceRequest, analyses, analysisRequestDTO.getDatePublished());
                        } else {
                            log.debug(
                                    "No published results to import for OpenMRS Task {}: "
                                            + "SENAITE analysis request {} has review state '{}'",
                                    task.getIdPart(),
                                    analysisRequestDTO.getUid(),
                                    analysisRequestDTO.getReviewState());
                        }
                        if (analysisRequestTaskStatus != null
                                && !analysisRequestTaskStatus.equalsIgnoreCase(
                                        task.getStatus().toString())) {
                            Task updatedTask = taskHandler.updateTask(
                                    taskHandler.updateTaskStatus(task, analysisRequestTaskStatus), task.getIdPart());
                            log.info(
                                    "Updated OpenMRS Task {} to status {}",
                                    updatedTask.getIdPart(),
                                    updatedTask.getStatus());
                        }
                    } else {
                        log.debug(
                                "Skipping OpenMRS Task {}: no SENAITE analysis request found for ServiceRequest {}",
                                taskId,
                                taskBasedOnReference);
                    }
                }
            }
        } catch (Exception e) {
            throw new EIPException(
                    String.format(
                            "Failed to synchronize polled OpenMRS Tasks with SENAITE (Task ID: %s, exchange ID: %s)",
                            taskId, exchange.getExchangeId()),
                    e);
        }
    }

    private String getTaskStatusCorrespondingToAnalysisRequestStatus(AnalysisRequestDTO analysisRequestDTO) {
        String analysisRequestStatus = analysisRequestDTO.getReviewState();
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
            ProducerTemplate producerTemplate, ServiceRequest serviceRequest, Analyses[] analyses, String datePublished)
            throws JsonProcessingException {
        String subjectID = serviceRequest.getSubject().getReference().split("/")[1];
        Encounter resultEncounter = encounterHandler.getEncounterByTypeAndSubjectAndStartDate(
                resultEncounterTypeUUID,
                subjectID,
                serviceRequest.hasOccurrencePeriod()
                        ? serviceRequest.getOccurrencePeriod().getStart()
                        : null);
        if (hasSameStartDate(resultEncounter, serviceRequest)) {
            // Result Encounter exists
            log.debug(
                    "Reusing lab results Encounter {} for ServiceRequest {}",
                    resultEncounter.getIdPart(),
                    serviceRequest.getIdPart());
            saveObservationAndDiagnosticReport(
                    producerTemplate, serviceRequest, analyses, resultEncounter, datePublished);
        } else {
            // Result Encounter does not exist, create a new one
            log.debug("Creating a lab results Encounter for ServiceRequest {}", serviceRequest.getIdPart());
            String encounterID = serviceRequest.getEncounter().getReference().split("/")[1];
            // Fetch the order encounter using the encounter ID from the service request
            log.debug("Fetching order Encounter {} for ServiceRequest {}", encounterID, serviceRequest.getIdPart());
            Encounter orderEncounter = encounterHandler.getEncounterByEncounterID(encounterID);
            Encounter savedResultEncounter =
                    encounterHandler.sendEncounter(encounterHandler.buildLabResultEncounter(orderEncounter));
            saveObservationAndDiagnosticReport(
                    producerTemplate, serviceRequest, analyses, savedResultEncounter, datePublished);
        }
    }

    /**
     * Checks if the start date of the encounter matches the start date of the service request.
     *
     * @param encounter      The encounter to check.
     * @param serviceRequest The service request to check against.
     * @return true if both dates match, false otherwise.
     */
    private boolean hasSameStartDate(Encounter encounter, ServiceRequest serviceRequest) {
        if (encounter != null && serviceRequest.hasOccurrencePeriod() && encounter.hasPeriod()) {
            return encounter.getPeriod().getStart().getTime()
                    == serviceRequest.getOccurrencePeriod().getStart().getTime();
        }
        return false;
    }

    private void saveObservationAndDiagnosticReport(
            ProducerTemplate producerTemplate,
            ServiceRequest serviceRequest,
            Analyses[] analyses,
            Encounter savedResultEncounter,
            String datePublished)
            throws JsonProcessingException {
        String subjectID = serviceRequest.getSubject().getReference().split("/")[1];
        ArrayList<String> observationUuids = new ArrayList<>();

        ArrayList<AnalysesDTO> analysesDTOs = new ArrayList<>();
        for (Analyses analysis : analyses) {
            AnalysesDTO resultAnalysesDTO =
                    analysesHandler.getAnalysesByAnalysesApiUrl(producerTemplate, analysis.getAnalysesApiUrl());
            analysesDTOs.add(resultAnalysesDTO);
        }

        if (Boolean.parseBoolean(runWithBahmniEmr)) {
            Observation savedObservation = observationHandler.getObservationByCodeSubjectEncounterAndDate(
                    bahmniResultsHandler.getServiceRequestCodingIdentifier(serviceRequest),
                    subjectID,
                    savedResultEncounter.getIdPart(),
                    datePublished);
            if (!observationHandler.doesObservationExists(savedObservation)) {
                // Create Bahmni result Observation
                savedObservation = bahmniResultsHandler.buildAndSendBahmniResultObservation(
                        producerTemplate, savedResultEncounter, serviceRequest, analysesDTOs, datePublished);
            }
            observationUuids.add(savedObservation.getIdPart());
        } else {
            for (AnalysesDTO resultAnalysesDTO : analysesDTOs) {

                String analysesDescription = resultAnalysesDTO.getDescription();
                String conceptUuid = analysesDescription.substring(
                        analysesDescription.lastIndexOf("(") + 1, analysesDescription.lastIndexOf(")"));

                Observation savedObservation = observationHandler.getObservationByCodeSubjectEncounterDateAndValue(
                        conceptUuid,
                        subjectID,
                        savedResultEncounter.getIdPart(),
                        resultAnalysesDTO.getResultCaptureDate(),
                        resultAnalysesDTO.getResult());
                if (!observationHandler.doesObservationExists(savedObservation)) {
                    // Create result Observation
                    savedObservation = observationHandler.sendObservation(observationHandler.buildResultObservation(
                            savedResultEncounter,
                            conceptUuid,
                            resultAnalysesDTO.getResult(),
                            resultAnalysesDTO.getResultCaptureDate()));
                }
                observationUuids.add(savedObservation.getIdPart());
            }

            diagnosticReportHandler.sendDiagnosticReport(diagnosticReportHandler.buildDiagnosticReport(
                    observationUuids, serviceRequest, savedResultEncounter.getIdPart()));
        }
    }
}
