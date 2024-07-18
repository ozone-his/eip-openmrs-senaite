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
import java.util.List;
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
                ServiceRequest serviceRequest = serviceRequestHandler.getServiceRequestByID(
                        producerTemplate, task.getBasedOn().get(0).getReference());
                if (serviceRequest.getStatus() == ServiceRequest.ServiceRequestStatus.REVOKED) {
                    Task rejectedTask = markTaskRejected(producerTemplate, task);
                    log.info("TaskProcessor: Rejected Task {}", rejectedTask);
                } else {
                    AnalysisRequestResponse analysisRequest =
                            fetchAnalysisRequestByClientIDAndSampleID(producerTemplate, task, serviceRequest);
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
                            Task updatedTask = updateTaskStatus(producerTemplate, task, analysisRequestTaskStatus);
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
        Encounter encounter = fetchLabResultTypeEncounterByServiceRequestID(producerTemplate, serviceRequest);
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
            Encounter savedResultEncounter = createLabResultEncounter(producerTemplate, serviceRequest);
            log.info("TaskProcessor: savedResultEncounter id {}", savedResultEncounter.getIdPart());
            com.ozonehis.eip.openmrs.senaite.model.analyses.Analyses resultAnalyses =
                    analysesHandler.getAnalysesByAnalysesApiUrl(producerTemplate, analyses[0].getAnalysesApiUrl());
            String analysesDescription = resultAnalyses.getDescription();
            String conceptUuid = analysesDescription.substring(
                    analysesDescription.lastIndexOf("(") + 1, analysesDescription.lastIndexOf(")"));

            Observation savedObservation = fetchObservationByConceptUuidPatientEncounterAndDate(
                    producerTemplate, serviceRequest, savedResultEncounter, resultAnalyses, conceptUuid);
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
            ServiceRequest serviceRequest,
            Encounter savedResultEncounter,
            com.ozonehis.eip.openmrs.senaite.model.analyses.Analyses resultAnalyses,
            String conceptUuid) {
        return observationHandler.getObservationByCodeSubjectEncounterAndDate(
                producerTemplate,
                conceptUuid,
                serviceRequest.getSubject().getReference().split("/")[1],
                savedResultEncounter.getIdPart(),
                resultAnalyses.getResultCaptureDate());
    }

    private Encounter createLabResultEncounter(ProducerTemplate producerTemplate, ServiceRequest serviceRequest) {
        Encounter orderEncounter = encounterHandler.getEncounterByEncounterID(
                producerTemplate, serviceRequest.getEncounter().getReference().split("/")[1]);
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
            ProducerTemplate producerTemplate, ServiceRequest serviceRequest) {
        // TODO: Fetch typeID from config
        return encounterHandler.getEncounterByTypeAndSubject(
                producerTemplate,
                "3596fafb-6f6f-4396-8c87-6e63a0f1bd71",
                serviceRequest.getSubject().getReference().split("/")[1]);
    }

    private Task markTaskRejected(ProducerTemplate producerTemplate, Task task) {
        log.info("TaskProcessor: ServiceRequest is voided or deleted {}", task);
        Task rejectTask = new Task();
        rejectTask.setId(task.getId());
        rejectTask.setStatus(Task.TaskStatus.REJECTED);
        rejectTask.setIntent(Task.TaskIntent.ORDER);
        return taskHandler.updateTask(producerTemplate, rejectTask, task.getIdPart());
    }

    private AnalysisRequestResponse fetchAnalysisRequestByClientIDAndSampleID(
            ProducerTemplate producerTemplate, Task task, ServiceRequest serviceRequest)
            throws JsonProcessingException {
        return analysisRequestHandler.getAnalysisRequestResponseByClientIDAndClientSampleID(
                producerTemplate,
                serviceRequest.getSubject().getReference().split("/")[1],
                task.getBasedOn().get(0).getReference());
    }

    private Task updateTaskStatus(ProducerTemplate producerTemplate, Task task, String analysisRequestTaskStatus) {
        Task updateTask = new Task();
        updateTask.setId(task.getIdPart());
        updateTask.setIntent(Task.TaskIntent.ORDER);
        updateTask.setStatus(Task.TaskStatus.fromCode(analysisRequestTaskStatus));
        log.info(
                "TaskProcessor: Updating Task with id {} from status {} to status {} analysisRequest {}",
                task.getIdPart(),
                task.getStatus().toString(),
                Task.TaskStatus.fromCode(analysisRequestTaskStatus),
                analysisRequestTaskStatus);
        return taskHandler.updateTask(producerTemplate, updateTask, task.getIdPart());
    }
}
