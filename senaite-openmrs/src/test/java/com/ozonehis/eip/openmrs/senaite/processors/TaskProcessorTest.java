/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openmrs.senaite.processors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.ozonehis.eip.openmrs.senaite.handlers.openmrs.DiagnosticReportHandler;
import com.ozonehis.eip.openmrs.senaite.handlers.openmrs.EncounterHandler;
import com.ozonehis.eip.openmrs.senaite.handlers.openmrs.ObservationHandler;
import com.ozonehis.eip.openmrs.senaite.handlers.openmrs.ServiceRequestHandler;
import com.ozonehis.eip.openmrs.senaite.handlers.openmrs.TaskHandler;
import com.ozonehis.eip.openmrs.senaite.handlers.senaite.AnalysesHandler;
import com.ozonehis.eip.openmrs.senaite.handlers.senaite.AnalysisRequestHandler;
import com.ozonehis.eip.openmrs.senaite.model.analysisRequest.AnalysisRequestDTO;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.camel.Exchange;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.hl7.fhir.r4.model.Task;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

class TaskProcessorTest extends BaseProcessorTest {

    private static final String TASK_ID = "zzaea498-e046-43c6-bf9c-dbbc7d39f40c";

    @Mock
    private ServiceRequestHandler serviceRequestHandler;

    @Mock
    private TaskHandler taskHandler;

    @Mock
    private AnalysisRequestHandler analysisRequestHandler;

    @Mock
    private EncounterHandler encounterHandler;

    @Mock
    private AnalysesHandler analysesHandler;

    @Mock
    private ObservationHandler observationHandler;

    @Mock
    private DiagnosticReportHandler diagnosticReportHandler;

    @InjectMocks
    private TaskProcessor taskProcessor;

    private static AutoCloseable mocksCloser;

    @BeforeEach
    void setup() {
        mocksCloser = openMocks(this);
    }

    @AfterAll
    static void close() throws Exception {
        mocksCloser.close();
    }

    @Test
    void shouldMarkTaskAsRejectedWhenServiceRequestIsRevoked() {
        // Setup
        Task task = getTask();

        Bundle bundle = new Bundle();
        List<Bundle.BundleEntryComponent> entries = new ArrayList<>();
        entries.add(new Bundle.BundleEntryComponent().setResource(task));
        bundle.setEntry(entries);

        ServiceRequest serviceRequest = buildServiceRequest();
        serviceRequest.setStatus(ServiceRequest.ServiceRequestStatus.REVOKED);

        Task rejectTask;
        rejectTask = task;
        rejectTask.setId(task.getId());
        rejectTask.setStatus(Task.TaskStatus.REJECTED);
        rejectTask.setIntent(Task.TaskIntent.ORDER);

        when(taskHandler.doesTaskExists(task)).thenReturn(true);
        when(serviceRequestHandler.getServiceRequestByID(
                        task.getBasedOn().get(0).getReference()))
                .thenReturn(serviceRequest);
        when(taskHandler.markTaskRejected(task)).thenReturn(rejectTask);
        when(taskHandler.updateTask(rejectTask, task.getIdPart())).thenReturn(rejectTask);

        Exchange exchange = createExchange(bundle, "c"); // NOTE: EventType is not required

        // Act
        taskProcessor.process(exchange);

        // Verify
        verify(taskHandler, times(1)).doesTaskExists(any());
        verify(serviceRequestHandler, times(1)).getServiceRequestByID(any());
        verify(taskHandler, times(1)).updateTask(any(), any());
        verify(taskHandler, times(1)).markTaskRejected(any());
    }

    @Test
    void shouldNotDoAnythingWhenAnalysisRequestDoesNotExists() throws JsonProcessingException {
        // Setup
        Task task = getTask();

        Bundle bundle = new Bundle();
        List<Bundle.BundleEntryComponent> entries = new ArrayList<>();
        entries.add(new Bundle.BundleEntryComponent().setResource(task));
        bundle.setEntry(entries);

        ServiceRequest serviceRequest = buildServiceRequest();

        when(taskHandler.doesTaskExists(task)).thenReturn(true);
        when(serviceRequestHandler.getServiceRequestByID(
                        task.getBasedOn().get(0).getReference()))
                .thenReturn(serviceRequest);
        when(analysisRequestHandler.getAnalysisRequestByClientIDAndClientSampleID(any(), any(), any()))
                .thenReturn(null);

        Exchange exchange = createExchange(bundle, "c"); // NOTE: EventType is not required

        // Act
        taskProcessor.process(exchange);

        // Verify
        verify(taskHandler, times(1)).doesTaskExists(any());
        verify(serviceRequestHandler, times(1)).getServiceRequestByID(any());
        verify(analysisRequestHandler, times(1)).getAnalysisRequestByClientIDAndClientSampleID(any(), any(), any());
    }

    @Test
    void shouldNotUpdateAnalysisRequestWhenAnalysisRequestStatusIsNotCompleted() throws JsonProcessingException {
        // Setup
        Task task = getTask();

        Bundle bundle = new Bundle();
        List<Bundle.BundleEntryComponent> entries = new ArrayList<>();
        entries.add(new Bundle.BundleEntryComponent().setResource(task));
        bundle.setEntry(entries);

        ServiceRequest serviceRequest = buildServiceRequest();
        AnalysisRequestDTO analysisRequestDTO = getAnalysisRequestDTO();
        analysisRequestDTO.setReviewState("sample_due");

        when(taskHandler.doesTaskExists(task)).thenReturn(true);
        when(serviceRequestHandler.getServiceRequestByID(
                        task.getBasedOn().get(0).getReference()))
                .thenReturn(serviceRequest);
        when(analysisRequestHandler.getAnalysisRequestByClientIDAndClientSampleID(any(), any(), any()))
                .thenReturn(getAnalysisRequestDTO());

        Exchange exchange = createExchange(bundle, "c"); // NOTE: EventType is not required

        // Act
        taskProcessor.process(exchange);

        // Verify
        verify(taskHandler, times(1)).doesTaskExists(any());
        verify(serviceRequestHandler, times(1)).getServiceRequestByID(any());
        verify(analysisRequestHandler, times(1)).getAnalysisRequestByClientIDAndClientSampleID(any(), any(), any());
    }

    @Test
    void shouldCreateResultsInOpenMRSWhenAnalysisRequestStatusIsCompleted() throws JsonProcessingException {
        // Setup
        Task task = getTask();

        Bundle bundle = new Bundle();
        List<Bundle.BundleEntryComponent> entries = new ArrayList<>();
        entries.add(new Bundle.BundleEntryComponent().setResource(task));
        bundle.setEntry(entries);

        ServiceRequest serviceRequest = buildServiceRequest();
        AnalysisRequestDTO analysisRequestDTO = getAnalysisRequestDTO();
        analysisRequestDTO.setReviewState("published");

        Encounter savedEncounter = buildEncounter();
        Coding coding = new Coding();
        coding.setCode("3c6838e6-d91b-42ff-a77e-6c536a04524a");
        coding.setSystem("http://fhir.openmrs.org/code-system/encounter-type");
        coding.setDisplay("Lab Results");
        savedEncounter.setType(
                (Collections.singletonList(new CodeableConcept().setCoding(Collections.singletonList(coding)))));

        Task updatedTask = getTask();
        updatedTask.setStatus(Task.TaskStatus.COMPLETED);

        when(taskHandler.doesTaskExists(task)).thenReturn(true);
        when(serviceRequestHandler.getServiceRequestByID(
                        task.getBasedOn().get(0).getReference()))
                .thenReturn(serviceRequest);
        when(analysisRequestHandler.getAnalysisRequestByClientIDAndClientSampleID(any(), any(), any()))
                .thenReturn(analysisRequestDTO);
        when(analysisRequestHandler.doesAnalysisRequestExists(any())).thenReturn(true);
        when(encounterHandler.getEncounterByTypeAndSubject(any(), any())).thenReturn(savedEncounter);
        when(encounterHandler.getEncounterByEncounterID(any())).thenReturn(null);
        when(encounterHandler.buildLabResultEncounter(any())).thenReturn(savedEncounter);
        when(encounterHandler.sendEncounter(any())).thenReturn(savedEncounter);
        when(analysesHandler.getAnalysesByAnalysesApiUrl(any(), any())).thenReturn(getAnalysesDTO());
        when(observationHandler.getObservationByCodeSubjectEncounterAndDate(any(), any(), any(), any()))
                .thenReturn(buildObservation());
        when(observationHandler.buildResultObservation(any(), any(), any(), any()))
                .thenReturn(buildObservation());
        when(observationHandler.sendObservation(any())).thenReturn(buildObservation());
        when(diagnosticReportHandler.buildDiagnosticReport(any(), any(), any())).thenReturn(buildDiagnosticReport());
        doNothing().when(diagnosticReportHandler).sendDiagnosticReport(any());
        when(taskHandler.updateTaskStatus(any(), any())).thenReturn(updatedTask);
        when(taskHandler.updateTask(any(), any())).thenReturn(updatedTask);

        Exchange exchange = createExchange(bundle, "c"); // NOTE: EventType is not required

        // Act
        taskProcessor.process(exchange);

        // Verify
        verify(taskHandler, times(1)).doesTaskExists(any());
        verify(serviceRequestHandler, times(1)).getServiceRequestByID(any());
        verify(analysisRequestHandler, times(1)).getAnalysisRequestByClientIDAndClientSampleID(any(), any(), any());
        verify(analysisRequestHandler, times(1)).doesAnalysisRequestExists(any());
        verify(encounterHandler, times(1)).getEncounterByTypeAndSubject(any(), any());
        verify(encounterHandler, times(1)).getEncounterByEncounterID(any());
        verify(encounterHandler, times(1)).buildLabResultEncounter(any());
        verify(encounterHandler, times(1)).sendEncounter(any());
        verify(analysesHandler, times(1)).getAnalysesByAnalysesApiUrl(any(), any());
        verify(observationHandler, times(1)).getObservationByCodeSubjectEncounterAndDate(any(), any(), any(), any());
        verify(observationHandler, times(1)).buildResultObservation(any(), any(), any(), any());
        verify(observationHandler, times(1)).sendObservation(any());
        verify(diagnosticReportHandler, times(1)).buildDiagnosticReport(any(), any(), any());
        verify(diagnosticReportHandler, times(1)).sendDiagnosticReport(any());
        verify(taskHandler, times(1)).updateTaskStatus(any(), any());
        verify(taskHandler, times(1)).updateTask(any(), any());
    }
}
