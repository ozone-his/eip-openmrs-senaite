/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openmrs.senaite.processors;

import com.fasterxml.jackson.core.JsonProcessingException;
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
        try (ProducerTemplate producerTemplate = exchange.getContext().createProducerTemplate()) {
            Bundle bundle = exchange.getMessage().getBody(Bundle.class);
            log.debug("TaskProcessor: bundle {}", bundle.getId());
            List<Bundle.BundleEntryComponent> entries = bundle.getEntry();
            for (Bundle.BundleEntryComponent entry : entries) {
                Task task = null;
                Resource resource = entry.getResource();
                if (resource instanceof Task) {
                    task = (Task) resource;
                }

                if (!taskHandler.doesTaskExists(task)) {
                    continue;
                }
                ServiceRequest serviceRequest = serviceRequestHandler.getServiceRequestByID(
                        task.getBasedOn().get(0).getReference());
                if (serviceRequest.getStatus() == ServiceRequest.ServiceRequestStatus.REVOKED) {
                    taskHandler.updateTask(taskHandler.markTaskRejected(task), task.getIdPart());
                } else {
                    String serviceRequestSubjectID =
                            serviceRequest.getSubject().getReference().split("/")[1];
                    String taskBasedOnReference = task.getBasedOn().get(0).getReference();
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
                                    "TaskProcessor: Nothing to update for task {} with status {}",
                                    task.getIdPart(),
                                    task.getStatus());
                        }
                        if (analysisRequestTaskStatus != null
                                && !analysisRequestTaskStatus.equalsIgnoreCase(
                                        task.getStatus().toString())) {
                            Task updatedTask = taskHandler.updateTask(
                                    taskHandler.updateTaskStatus(task, analysisRequestTaskStatus), task.getIdPart());
                            log.debug(
                                    "TaskProcessor: Updated Task {} with status {}",
                                    updatedTask.getIdPart(),
                                    updatedTask.getStatus());
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new EIPException(String.format("Error processing Task %s", e.getMessage()));
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
        Encounter resultEncounter = encounterHandler.getEncounterByTypeAndSubject(
                resultEncounterTypeUUID,
                serviceRequest.getSubject().getReference().split("/")[1]);
        if (resultEncounter != null
                && resultEncounter.getPeriod().getStart().getTime()
                        == serviceRequest.getOccurrencePeriod().getStart().getTime()) {
            // Result Encounter exists
            saveObservationAndDiagnosticReport(
                    producerTemplate, serviceRequest, analyses, resultEncounter, datePublished);
        } else {
            String encounterID = serviceRequest.getEncounter().getReference().split("/")[1];
            Encounter orderEncounter = encounterHandler.getEncounterByEncounterID(encounterID);
            Encounter savedResultEncounter =
                    encounterHandler.sendEncounter(encounterHandler.buildLabResultEncounter(orderEncounter));
            saveObservationAndDiagnosticReport(
                    producerTemplate, serviceRequest, analyses, savedResultEncounter, datePublished);
        }
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

                Observation savedObservation = observationHandler.getObservationByCodeSubjectEncounterAndDate(
                        conceptUuid,
                        subjectID,
                        savedResultEncounter.getIdPart(),
                        resultAnalysesDTO.getResultCaptureDate());
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
        }

        diagnosticReportHandler.sendDiagnosticReport(diagnosticReportHandler.buildDiagnosticReport(
                observationUuids, serviceRequest, savedResultEncounter.getIdPart()));
    }
}
