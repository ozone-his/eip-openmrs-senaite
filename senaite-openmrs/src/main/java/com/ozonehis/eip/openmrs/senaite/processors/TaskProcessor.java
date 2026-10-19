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
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
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

    // Maximum number of open Tasks checked against SENAITE per polling cycle, 0 or less means no limit.
    @Value("${task.update.max.per.cycle:100}")
    private int maxTasksPerCycle;

    // Polling cycle in which each open Task was last checked, so that a capped cycle starts with the Tasks that have
    // waited the longest.
    private final Map<String, Long> lastCheckedCycleByTaskId = new HashMap<>();

    private long pollCycle;

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
            for (Task task : selectTasksToCheck(bundle.getEntry())) {
                if (task.getBasedOn() == null || task.getBasedOn().isEmpty()) {
                    continue;
                }
                String taskBasedOnReference = task.getBasedOn().get(0).getReference();
                if (taskBasedOnReference.contains(Constants.SERVICE_REQUEST_PREFIX)) {
                    taskBasedOnReference = taskBasedOnReference.split("/")[1];
                }
                ServiceRequest serviceRequest = serviceRequestHandler.getServiceRequestByID(taskBasedOnReference);
                if (serviceRequest.getStatus() == ServiceRequest.ServiceRequestStatus.REVOKED) {
                    taskHandler.updateTask(taskHandler.markTaskRejected(task), task.getIdPart());
                } else {
                    String serviceRequestSubjectID =
                            serviceRequest.getSubject().getReference().split("/")[1];
                    // Only the review state is needed to decide what to do, the full sample is fetched once published
                    AnalysisRequestDTO analysisRequestDTO =
                            analysisRequestHandler.getAnalysisRequestStatusByClientIDAndClientSampleID(
                                    producerTemplate, serviceRequestSubjectID, taskBasedOnReference);
                    if (analysisRequestHandler.doesAnalysisRequestExists(analysisRequestDTO)) {
                        String analysisRequestTaskStatus =
                                getTaskStatusCorrespondingToAnalysisRequestStatus(analysisRequestDTO);
                        if (analysisRequestTaskStatus != null
                                && analysisRequestTaskStatus.equalsIgnoreCase("completed")) {
                            AnalysisRequestDTO publishedAnalysisRequestDTO =
                                    analysisRequestHandler.getAnalysisRequestByClientIDAndClientSampleID(
                                            producerTemplate, serviceRequestSubjectID, taskBasedOnReference);
                            if (!analysisRequestHandler.doesAnalysisRequestExists(publishedAnalysisRequestDTO)) {
                                log.warn(
                                        "TaskProcessor: Published AnalysisRequest for task {} could not be fetched",
                                        task.getIdPart());
                                continue;
                            }
                            createResultsInOpenMRS(
                                    producerTemplate,
                                    serviceRequest,
                                    publishedAnalysisRequestDTO.getUid(),
                                    publishedAnalysisRequestDTO.getAnalyses(),
                                    analysisRequestDTO.getDatePublished());
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

    /**
     * Returns the open Tasks to check against SENAITE in this cycle. When there are more than
     * {@code maxTasksPerCycle}, the ones checked least recently go first, so every open Task is checked within a few
     * cycles and a Task that keeps failing does not hold the others back.
     */
    private List<Task> selectTasksToCheck(List<Bundle.BundleEntryComponent> entries) {
        List<Task> openTasks = new ArrayList<>();
        for (Bundle.BundleEntryComponent entry : entries) {
            Task task = null;
            Resource resource = entry.getResource();
            if (resource instanceof Task) {
                task = (Task) resource;
            }

            if (!taskHandler.doesTaskExists(task)) {
                continue;
            }
            if (task.getStatus() == Task.TaskStatus.COMPLETED || task.getStatus() == Task.TaskStatus.CANCELLED) {
                log.debug(
                        "TaskProcessor: Skipping duplicate or terminal task {} with status {}",
                        task.getIdPart(),
                        task.getStatus());
                continue;
            }
            openTasks.add(task);
        }

        pollCycle++;
        Set<String> openTaskIds = openTasks.stream().map(Task::getIdPart).collect(Collectors.toSet());
        lastCheckedCycleByTaskId.keySet().retainAll(openTaskIds);

        List<Task> tasksToCheck = openTasks;
        if (maxTasksPerCycle > 0 && openTasks.size() > maxTasksPerCycle) {
            tasksToCheck = openTasks.stream()
                    .sorted(Comparator.comparingLong(
                            task -> lastCheckedCycleByTaskId.getOrDefault(task.getIdPart(), 0L)))
                    .limit(maxTasksPerCycle)
                    .toList();
            log.info(
                    "TaskProcessor: Checking {} of {} open Tasks this cycle (task.update.max.per.cycle={})",
                    tasksToCheck.size(),
                    openTasks.size(),
                    maxTasksPerCycle);
        }
        tasksToCheck.forEach(task -> lastCheckedCycleByTaskId.put(task.getIdPart(), pollCycle));
        return tasksToCheck;
    }

    private String getTaskStatusCorrespondingToAnalysisRequestStatus(AnalysisRequestDTO analysisRequestDTO) {
        String analysisRequestStatus = analysisRequestDTO.getReviewState();
        if (analysisRequestStatus == null) {
            return null;
        }
        return switch (analysisRequestStatus.toLowerCase()) {
            case "sample_due" -> "requested";
            case "sample_received" -> "accepted";
            case "published" -> "completed";
            case "cancelled", "rejected" -> "rejected";
            default -> null;
        };
    }

    private void createResultsInOpenMRS(
            ProducerTemplate producerTemplate,
            ServiceRequest serviceRequest,
            String analysisRequestUid,
            Analyses[] analyses,
            String datePublished)
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
            log.debug("TaskProcessor: LabResults Encounter exists with ID {}", resultEncounter.getIdPart());
            saveObservationAndDiagnosticReport(
                    producerTemplate, serviceRequest, analysisRequestUid, analyses, resultEncounter, datePublished);
        } else {
            // Result Encounter does not exist, create a new one
            log.debug("TaskProcessor: LabResults Encounter does not exist, creating a new one");
            String encounterID = serviceRequest.getEncounter().getReference().split("/")[1];
            // Fetch the order encounter using the encounter ID from the service request
            log.debug("TaskProcessor: Fetching order encounter with ID {}", encounterID);
            Encounter orderEncounter = encounterHandler.getEncounterByEncounterID(encounterID);
            Encounter savedResultEncounter =
                    encounterHandler.sendEncounter(encounterHandler.buildLabResultEncounter(orderEncounter));
            saveObservationAndDiagnosticReport(
                    producerTemplate,
                    serviceRequest,
                    analysisRequestUid,
                    analyses,
                    savedResultEncounter,
                    datePublished);
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
            String analysisRequestUid,
            Analyses[] analyses,
            Encounter savedResultEncounter,
            String datePublished)
            throws JsonProcessingException {
        String subjectID = serviceRequest.getSubject().getReference().split("/")[1];
        ArrayList<String> observationUuids = new ArrayList<>();

        // One request for all analyses of the sample, restricted to the ones the sample lists, with a per-analysis
        // fallback for any the batch did not return
        Map<String, AnalysesDTO> analysesByUid =
                analysesHandler.getAnalysesByAnalysisRequestUID(producerTemplate, analysisRequestUid);
        ArrayList<AnalysesDTO> analysesDTOs = new ArrayList<>();
        for (Analyses analysis : analyses) {
            AnalysesDTO resultAnalysesDTO = analysesByUid.get(analysis.getAnalysesUid());
            if (resultAnalysesDTO == null) {
                resultAnalysesDTO =
                        analysesHandler.getAnalysesByAnalysesApiUrl(producerTemplate, analysis.getAnalysesApiUrl());
            }
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
                            resultAnalysesDTO.getResultCaptureDate(),
                            resultAnalysesDTO.getRemarks()));
                }
                observationUuids.add(savedObservation.getIdPart());
            }

            diagnosticReportHandler.sendDiagnosticReport(diagnosticReportHandler.buildDiagnosticReport(
                    observationUuids, serviceRequest, savedResultEncounter.getIdPart()));
        }
    }
}
