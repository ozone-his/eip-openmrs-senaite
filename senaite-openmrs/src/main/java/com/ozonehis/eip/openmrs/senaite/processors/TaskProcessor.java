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
import java.util.Collections;
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
import org.apache.commons.lang3.RandomStringUtils;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.Reference;
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
                            createResultsInOpenMRS(producerTemplate, serviceRequest, analyses);
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
                            log.info(
                                    "TaskProcessor: Nothing to update for task {} with status {}",
                                    task,
                                    task.getStatus());
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
        headers.put(
                Constants.HEADER_ENCOUNTER_TYPE_ID, "3596fafb-6f6f-4396-8c87-6e63a0f1bd71"); // TODO: Fetch from config
        headers.put(
                Constants.HEADER_PATIENT_ID,
                serviceRequest.getSubject().getReference().split("/")[1]);
        Encounter encounter = encounterHandler.getEncounter(producerTemplate, headers);
        if (encounter != null
                && encounter.getPeriod().getStart().getTime()
                        == serviceRequest.getOccurrencePeriod().getStart().getTime()) {
            // Result Encounter exists
            log.info(
                    "TaskProcessor: Result Encounter {} exists for serviceRequest id {}",
                    encounter.getId(),
                    serviceRequest.getId());
        } else {
            Encounter resultEncounter = new Encounter();
            resultEncounter.setId(RandomStringUtils.random(14, true, true));
            //            resultEncounter.setLocation(encounter.getLocation()); TODO: Set patient location
            Coding coding = new Coding();
            coding.setCode("3596fafb-6f6f-4396-8c87-6e63a0f1bd71");
            coding.setSystem("http://fhir.openmrs.org/code-system/encounter-type");
            coding.setDisplay("Lab Results");
            resultEncounter.setType(
                    (Collections.singletonList(new CodeableConcept().setCoding(Collections.singletonList(coding)))));
            resultEncounter.setPeriod(
                    new Period().setStart(serviceRequest.getOccurrencePeriod().getStart()));
            resultEncounter.setSubject(serviceRequest.getSubject());
            // TODO: "visit":"${exchangeProperty.service-request-visit-uuid}"
            resultEncounter.setParticipant(Collections.singletonList(
                    new Encounter.EncounterParticipantComponent().setIndividual(serviceRequest.getRequester())));
            Encounter savedResultEncounter = encounterHandler.sendEncounter(producerTemplate, resultEncounter);
            log.info("TaskProcessor: savedResultEncounter id {}", savedResultEncounter.getId());

            headers.put(Constants.HEADER_ANALYSES_GET_ENDPOINT, analyses[0].getAnalysesUrlApiUrl());
            com.ozonehis.eip.openmrs.senaite.model.analyses.Analyses resultAnalyses =
                    analysesHandler.getAnalyses(producerTemplate, headers);
            String analysesDescription = resultAnalyses.getDescription();
            String conceptUuid = analysesDescription.substring(
                    analysesDescription.lastIndexOf("(") + 1, analysesDescription.lastIndexOf(")"));

            headers.put(Constants.HEADER_OBSERVATION_CODE, conceptUuid);
            headers.put(
                    Constants.HEADER_OBSERVATION_SUBJECT,
                    serviceRequest.getSubject().getReference().split("/")[1]);
            headers.put(Constants.HEADER_OBSERVATION_ENCOUNTER, savedResultEncounter.getId());
            headers.put(Constants.HEADER_OBSERVATION_DATE, resultAnalyses.getResultCaptureDate());
            Observation savedObservation = observationHandler.getObservation(producerTemplate, headers);
            if (savedObservation == null || savedObservation.getId().isEmpty()) {
                // Create result Observation
                Observation observation = new Observation();
                observation.setEncounter(new Reference("Encounter/" + savedResultEncounter.getId()));
                savedObservation = observationHandler.sendObservation(producerTemplate, observation);
                log.info("TaskProcessor: Saved Observation {}", savedObservation);
            }
            log.info("TaskProcessor: Completed saving results for service request {}", serviceRequest.getId());
        }
    }
}
