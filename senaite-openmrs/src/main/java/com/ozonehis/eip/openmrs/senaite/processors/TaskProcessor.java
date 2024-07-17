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
import com.ozonehis.eip.openmrs.senaite.Constants;
import com.ozonehis.eip.openmrs.senaite.handlers.openmrs.EncounterHandler;
import com.ozonehis.eip.openmrs.senaite.handlers.openmrs.ObservationHandler;
import com.ozonehis.eip.openmrs.senaite.handlers.openmrs.ServiceRequestHandler;
import com.ozonehis.eip.openmrs.senaite.handlers.openmrs.TaskHandler;
import com.ozonehis.eip.openmrs.senaite.handlers.senaite.AnalysesHandler;
import com.ozonehis.eip.openmrs.senaite.handlers.senaite.AnalysisRequestHandler;
import com.ozonehis.eip.openmrs.senaite.model.analysisRequest.Analyses;
import com.ozonehis.eip.openmrs.senaite.model.analysisRequest.AnalysisRequestResponse;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
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
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Quantity;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.Task;
import org.hl7.fhir.r4.model.Type;
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
                if (serviceRequest.getStatus() == ServiceRequest.ServiceRequestStatus.REVOKED) {
                    Task rejectedTask = markTaskRejected(producerTemplate, headers, task);
                    log.info("TaskProcessor: Rejected Task {}", rejectedTask);
                } else {
                    AnalysisRequestResponse analysisRequest =
                            fetchAnalysisRequestByClientIDAndSampleID(producerTemplate, headers, task, serviceRequest);
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
                            Task updatedTask =
                                    updateTaskStatus(producerTemplate, headers, task, analysisRequestTaskStatus);
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
        Map<String, Object> headers = new HashMap<>();
        Encounter encounter = fetchLabResultTypeEncounterByServiceRequestID(producerTemplate, headers, serviceRequest);
        if (encounter != null
                && encounter.getPeriod().getStart().getTime()
                        == serviceRequest.getOccurrencePeriod().getStart().getTime()) {
            // Result Encounter exists
            log.info(
                    "TaskProcessor: Result Encounter {} exists for serviceRequest id {}",
                    encounter.getId(),
                    serviceRequest.getId());
            return;
        } else {
            Encounter savedResultEncounter = createLabResultEncounter(producerTemplate, headers, serviceRequest);
            log.info("TaskProcessor: savedResultEncounter id {}", savedResultEncounter.getIdPart());

            headers.put(Constants.HEADER_ANALYSES_GET_ENDPOINT, analyses[0].getAnalysesUrlApiUrl());
            com.ozonehis.eip.openmrs.senaite.model.analyses.Analyses resultAnalyses =
                    analysesHandler.getAnalyses(producerTemplate, headers);
            String analysesDescription = resultAnalyses.getDescription();
            String conceptUuid = analysesDescription.substring(
                    analysesDescription.lastIndexOf("(") + 1, analysesDescription.lastIndexOf(")"));

            Observation savedObservation = fetchObservationByConceptUuidPatientEncounterAndDate(
                    producerTemplate, headers, serviceRequest, savedResultEncounter, resultAnalyses, conceptUuid);
            log.info("TaskProcessor: Fetched Observation {}", savedObservation);
            if (savedObservation == null || savedObservation.getId().isEmpty()) {
                // Create result Observation
                savedObservation = createResultObservationBySenaiteAnalysesResult(
                        producerTemplate, savedResultEncounter, resultAnalyses, conceptUuid);
                log.info("TaskProcessor: Saved Observation {}", savedObservation);
            }
            log.info("TaskProcessor: Completed saving results for service request {}", serviceRequest.getId());
        }
    }

    private Observation createResultObservationBySenaiteAnalysesResult(
            ProducerTemplate producerTemplate,
            Encounter savedResultEncounter,
            com.ozonehis.eip.openmrs.senaite.model.analyses.Analyses resultAnalyses,
            String conceptUuid) {
        Observation observation = new Observation();
        observation.setStatus(Observation.ObservationStatus.FINAL);
        observation.setCode(new CodeableConcept(new Coding().setCode(conceptUuid)));
        observation.setSubject(savedResultEncounter.getSubject());
        observation.setEffective(
                new DateTimeType().setValue(Date.from(Instant.parse(resultAnalyses.getResultCaptureDate()))));
        observation.setValue(getObservationValueBySenaiteResult(resultAnalyses.getResult()));
        observation.setEncounter(new Reference("Encounter/" + savedResultEncounter.getIdPart()));
        return observationHandler.sendObservation(producerTemplate, observation);
    }

    private Type getObservationValueBySenaiteResult(String senaiteResult) {
        if (senaiteResult.matches("-?\\d+(\\.\\d+)?")) {
            // If result is a number
            log.info("Result match number {}", senaiteResult);
            return new Quantity().setValue(Double.parseDouble(senaiteResult));
        } else if (senaiteResult.matches(
                        "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")
                || senaiteResult.matches("^[A-F0-9]{36,38}$")) {
            // If result is a UUID
            log.info("Result match uuid {}", senaiteResult);
            return new CodeableConcept().setCoding(Collections.singletonList(new Coding().setCode(senaiteResult)));
        } else {
            // Handle ordinary string values
            log.info("Result match string {}", senaiteResult);
            return new StringType(senaiteResult);
        }
    }

    private Observation fetchObservationByConceptUuidPatientEncounterAndDate(
            ProducerTemplate producerTemplate,
            Map<String, Object> headers,
            ServiceRequest serviceRequest,
            Encounter savedResultEncounter,
            com.ozonehis.eip.openmrs.senaite.model.analyses.Analyses resultAnalyses,
            String conceptUuid) {
        headers.put(Constants.HEADER_OBSERVATION_CODE, conceptUuid);
        headers.put(
                Constants.HEADER_OBSERVATION_SUBJECT,
                serviceRequest.getSubject().getReference().split("/")[1]);
        headers.put(Constants.HEADER_OBSERVATION_ENCOUNTER, savedResultEncounter.getIdPart());
        headers.put(Constants.HEADER_OBSERVATION_DATE, resultAnalyses.getResultCaptureDate());
        return observationHandler.getObservation(producerTemplate, headers);
    }

    private Encounter createLabResultEncounter(
            ProducerTemplate producerTemplate, Map<String, Object> headers, ServiceRequest serviceRequest) {
        headers.put(
                Constants.HEADER_ENCOUNTER_ID,
                serviceRequest.getEncounter().getReference().split("/")[1]);
        Encounter orderEncounter = encounterHandler.getEncounterById(producerTemplate, headers);
        Encounter resultEncounter = new Encounter();
        resultEncounter.setLocation(orderEncounter.getLocation());
        Coding coding = new Coding();
        coding.setCode("3596fafb-6f6f-4396-8c87-6e63a0f1bd71");
        coding.setSystem("http://fhir.openmrs.org/code-system/encounter-type");
        coding.setDisplay("Lab Results");
        resultEncounter.setType(
                (Collections.singletonList(new CodeableConcept().setCoding(Collections.singletonList(coding)))));
        resultEncounter.setPeriod(orderEncounter.getPeriod());
        resultEncounter.setSubject(orderEncounter.getSubject());
        resultEncounter.setPartOf(orderEncounter.getPartOf());
        resultEncounter.setParticipant(orderEncounter.getParticipant());
        return encounterHandler.sendEncounter(producerTemplate, resultEncounter);
    }

    private Encounter fetchLabResultTypeEncounterByServiceRequestID(
            ProducerTemplate producerTemplate, Map<String, Object> headers, ServiceRequest serviceRequest) {
        headers.put(
                Constants.HEADER_ENCOUNTER_TYPE_ID, "3596fafb-6f6f-4396-8c87-6e63a0f1bd71"); // TODO: Fetch from config
        headers.put(
                Constants.HEADER_PATIENT_ID,
                serviceRequest.getSubject().getReference().split("/")[1]);
        return encounterHandler.getEncounter(producerTemplate, headers);
    }

    private Task markTaskRejected(ProducerTemplate producerTemplate, Map<String, Object> headers, Task task) {
        log.info("TaskProcessor: ServiceRequest is voided or deleted {}", task);
        Task rejectTask = new Task();
        rejectTask.setId(task.getId());
        rejectTask.setStatus(Task.TaskStatus.REJECTED);
        rejectTask.setIntent(Task.TaskIntent.ORDER);
        headers.put(Constants.HEADER_TASK_ID, task.getIdPart());
        return taskHandler.updateTask(producerTemplate, rejectTask, headers);
    }

    private AnalysisRequestResponse fetchAnalysisRequestByClientIDAndSampleID(
            ProducerTemplate producerTemplate, Map<String, Object> headers, Task task, ServiceRequest serviceRequest)
            throws JsonProcessingException {
        headers.put(Constants.HEADER_CLIENT_SAMPLE_ID, task.getBasedOn().get(0).getReference());
        headers.put(
                Constants.HEADER_CLIENT_ID,
                serviceRequest.getSubject().getReference().split("/")[1]);
        return analysisRequestHandler.getAnalysisRequestResponse(producerTemplate, headers);
    }

    private Task updateTaskStatus(
            ProducerTemplate producerTemplate,
            Map<String, Object> headers,
            Task task,
            String analysisRequestTaskStatus) {
        Task updateTask = new Task();
        updateTask.setId(task.getIdPart());
        updateTask.setIntent(Task.TaskIntent.ORDER);
        updateTask.setStatus(Task.TaskStatus.fromCode(analysisRequestTaskStatus));
        headers.put(Constants.HEADER_TASK_ID, task.getIdPart());
        log.info(
                "TaskProcessor: Updating Task with id {} from status {} to status {} analysisRequest {}",
                task.getIdPart(),
                task.getStatus().toString(),
                Task.TaskStatus.fromCode(analysisRequestTaskStatus),
                analysisRequestTaskStatus);
        return taskHandler.updateTask(producerTemplate, updateTask, headers);
    }
}
