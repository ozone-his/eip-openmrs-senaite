/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openmrs.senaite.processors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
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
import com.ozonehis.eip.openmrs.senaite.model.analyses.AnalysesDTO;
import com.ozonehis.eip.openmrs.senaite.model.analysisRequest.AnalysisRequestDTO;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.apache.camel.Exchange;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.hl7.fhir.r4.model.Task;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

class TaskProcessorTest extends BaseProcessorTest {

    private static final String TASK_ID = "zzaea498-e046-43c6-bf9c-dbbc7d39f40c";
    private static final String SERVICE_REQUEST_ID = "bbaea498-e046-43c6-bf9c-dbbc7d39f38c";
    private static final String ANALYSIS_UID = "06d1e902317047d1b90b27df5122ab78";

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
        when(serviceRequestHandler.getServiceRequestByID(SERVICE_REQUEST_ID)).thenReturn(serviceRequest);
        when(taskHandler.markTaskRejected(task)).thenReturn(rejectTask);
        when(taskHandler.updateTask(rejectTask, task.getIdPart())).thenReturn(rejectTask);

        Exchange exchange = createExchange(bundle, "c"); // NOTE: EventType is not required

        // Act
        taskProcessor.process(exchange);

        // Verify
        verify(taskHandler, times(1)).doesTaskExists(any());
        verify(serviceRequestHandler, times(1)).getServiceRequestByID(SERVICE_REQUEST_ID);
        verify(taskHandler, times(1)).updateTask(any(), any());
        verify(taskHandler, times(1)).markTaskRejected(any());
    }

    @Test
    void shouldSkipCompletedTaskWithoutReprocessingResults() throws JsonProcessingException {
        Task task = getTask();
        task.setStatus(Task.TaskStatus.COMPLETED);

        Bundle bundle = new Bundle();
        List<Bundle.BundleEntryComponent> entries = new ArrayList<>();
        entries.add(new Bundle.BundleEntryComponent().setResource(task));
        bundle.setEntry(entries);

        when(taskHandler.doesTaskExists(task)).thenReturn(true);

        Exchange exchange = createExchange(bundle, "c");

        taskProcessor.process(exchange);

        verify(taskHandler, times(1)).doesTaskExists(any());
        verify(serviceRequestHandler, times(0)).getServiceRequestByID(any());
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
        when(serviceRequestHandler.getServiceRequestByID(SERVICE_REQUEST_ID)).thenReturn(serviceRequest);
        when(analysisRequestHandler.getAnalysisRequestStatusByClientIDAndClientSampleID(any(), any(), any()))
                .thenReturn(null);

        Exchange exchange = createExchange(bundle, "c"); // NOTE: EventType is not required

        // Act
        taskProcessor.process(exchange);

        // Verify
        verify(taskHandler, times(1)).doesTaskExists(any());
        verify(serviceRequestHandler, times(1)).getServiceRequestByID(SERVICE_REQUEST_ID);
        verify(analysisRequestHandler, times(1))
                .getAnalysisRequestStatusByClientIDAndClientSampleID(any(), any(), any());
        verify(analysisRequestHandler, never()).getAnalysisRequestByClientIDAndClientSampleID(any(), any(), any());
    }

    @Test
    void shouldNotUpdateAnalysisRequestWhenAnalysisRequestStatusIsNotCompleted() throws JsonProcessingException {
        // Setup
        Task task = getTask();

        Bundle bundle = new Bundle();
        List<Bundle.BundleEntryComponent> entries = new ArrayList<>();
        entries.add(new Bundle.BundleEntryComponent().setResource(task));
        bundle.setEntry(entries);

        task.setStatus(Task.TaskStatus.REQUESTED);
        ServiceRequest serviceRequest = buildServiceRequest();
        AnalysisRequestDTO analysisRequestDTO = getAnalysisRequestDTO();
        analysisRequestDTO.setReviewState("sample_due");

        when(taskHandler.doesTaskExists(task)).thenReturn(true);
        when(serviceRequestHandler.getServiceRequestByID(SERVICE_REQUEST_ID)).thenReturn(serviceRequest);
        when(analysisRequestHandler.getAnalysisRequestStatusByClientIDAndClientSampleID(any(), any(), any()))
                .thenReturn(analysisRequestDTO);
        when(analysisRequestHandler.doesAnalysisRequestExists(any())).thenReturn(true);

        Exchange exchange = createExchange(bundle, "c"); // NOTE: EventType is not required

        // Act
        taskProcessor.process(exchange);

        // Verify
        verify(taskHandler, times(1)).doesTaskExists(any());
        verify(serviceRequestHandler, times(1)).getServiceRequestByID(SERVICE_REQUEST_ID);
        verify(analysisRequestHandler, times(1))
                .getAnalysisRequestStatusByClientIDAndClientSampleID(any(), any(), any());
        verify(analysisRequestHandler, never()).getAnalysisRequestByClientIDAndClientSampleID(any(), any(), any());
        verify(taskHandler, never()).updateTask(any(), any());
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
        when(serviceRequestHandler.getServiceRequestByID(SERVICE_REQUEST_ID)).thenReturn(serviceRequest);
        when(analysisRequestHandler.getAnalysisRequestStatusByClientIDAndClientSampleID(any(), any(), any()))
                .thenReturn(analysisRequestDTO);
        when(analysisRequestHandler.getAnalysisRequestByClientIDAndClientSampleID(any(), any(), any()))
                .thenReturn(analysisRequestDTO);
        when(analysisRequestHandler.doesAnalysisRequestExists(any())).thenReturn(true);
        when(encounterHandler.getEncounterByTypeAndSubjectAndStartDate(any(), any(), any()))
                .thenReturn(savedEncounter);
        when(encounterHandler.getEncounterByEncounterID(any())).thenReturn(null);
        when(encounterHandler.buildLabResultEncounter(any())).thenReturn(savedEncounter);
        when(encounterHandler.sendEncounter(any())).thenReturn(savedEncounter);
        var resultAnalyses = getAnalysesDTO();
        resultAnalyses.setRemarks("Sample slightly hemolysed; result verified.");
        when(analysesHandler.getAnalysesByAnalysisRequestUID(any(), eq(analysisRequestDTO.getUid())))
                .thenReturn(Map.of(ANALYSIS_UID, resultAnalyses));
        when(observationHandler.getObservationByCodeSubjectEncounterDateAndValue(any(), any(), any(), any(), any()))
                .thenReturn(buildObservation());
        when(observationHandler.buildResultObservation(any(), any(), any(), any(), any()))
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
        verify(serviceRequestHandler, times(1)).getServiceRequestByID(SERVICE_REQUEST_ID);
        verify(analysisRequestHandler, times(1))
                .getAnalysisRequestStatusByClientIDAndClientSampleID(any(), any(), any());
        verify(analysisRequestHandler, times(1)).getAnalysisRequestByClientIDAndClientSampleID(any(), any(), any());
        verify(analysisRequestHandler, times(2)).doesAnalysisRequestExists(any());
        verify(encounterHandler, times(1)).getEncounterByTypeAndSubjectAndStartDate(any(), any(), any());
        verify(encounterHandler, times(1)).getEncounterByEncounterID(any());
        verify(encounterHandler, times(1)).buildLabResultEncounter(any());
        verify(encounterHandler, times(1)).sendEncounter(any());
        verify(analysesHandler, times(1)).getAnalysesByAnalysisRequestUID(any(), any());
        verify(analysesHandler, never()).getAnalysesByAnalysesApiUrl(any(), any());
        verify(observationHandler, times(1))
                .getObservationByCodeSubjectEncounterDateAndValue(any(), any(), any(), any(), any());
        verify(observationHandler, times(1))
                .buildResultObservation(any(), any(), any(), any(), eq(resultAnalyses.getRemarks()));
        verify(observationHandler, times(1)).sendObservation(any());
        verify(diagnosticReportHandler, times(1)).buildDiagnosticReport(any(), any(), any());
        verify(diagnosticReportHandler, times(1)).sendDiagnosticReport(any());
        verify(taskHandler, times(1)).updateTaskStatus(any(), any());
        verify(taskHandler, times(1)).updateTask(any(), any());
    }

    @Test
    void shouldFetchAnalysesMissingFromBatchIndividually() throws JsonProcessingException {
        Task task = getTask();
        Bundle bundle = bundleOf(task);
        AnalysisRequestDTO analysisRequestDTO = getAnalysisRequestDTO();
        analysisRequestDTO.setReviewState("published");
        Encounter savedEncounter = buildEncounter();
        AnalysesDTO resultAnalyses = getAnalysesDTO();

        when(taskHandler.doesTaskExists(task)).thenReturn(true);
        when(serviceRequestHandler.getServiceRequestByID(SERVICE_REQUEST_ID)).thenReturn(buildServiceRequest());
        when(analysisRequestHandler.getAnalysisRequestStatusByClientIDAndClientSampleID(any(), any(), any()))
                .thenReturn(analysisRequestDTO);
        when(analysisRequestHandler.getAnalysisRequestByClientIDAndClientSampleID(any(), any(), any()))
                .thenReturn(analysisRequestDTO);
        when(analysisRequestHandler.doesAnalysisRequestExists(any())).thenReturn(true);
        when(encounterHandler.buildLabResultEncounter(any())).thenReturn(savedEncounter);
        when(encounterHandler.sendEncounter(any())).thenReturn(savedEncounter);
        when(analysesHandler.getAnalysesByAnalysisRequestUID(any(), any())).thenReturn(Map.of());
        when(observationHandler.doesObservationExists(any())).thenReturn(true);
        when(analysesHandler.getAnalysesByAnalysesApiUrl(any(), any())).thenReturn(resultAnalyses);
        when(observationHandler.getObservationByCodeSubjectEncounterDateAndValue(any(), any(), any(), any(), any()))
                .thenReturn(buildObservation());
        when(diagnosticReportHandler.buildDiagnosticReport(any(), any(), any())).thenReturn(buildDiagnosticReport());
        when(taskHandler.updateTaskStatus(any(), any())).thenReturn(task);
        when(taskHandler.updateTask(any(), any())).thenReturn(task);

        taskProcessor.process(createExchange(bundle, "c"));

        verify(analysesHandler, times(1))
                .getAnalysesByAnalysesApiUrl(any(), eq(analysisRequestDTO.getAnalyses()[0].getAnalysesApiUrl()));
        verify(diagnosticReportHandler, times(1)).sendDiagnosticReport(any());
    }

    @Test
    void shouldMarkTaskRejectedWhenSampleIsRejectedInSenaite() throws JsonProcessingException {
        Task task = getTask();
        AnalysisRequestDTO analysisRequestDTO = getAnalysisRequestDTO();
        analysisRequestDTO.setReviewState("rejected");

        when(taskHandler.doesTaskExists(task)).thenReturn(true);
        when(serviceRequestHandler.getServiceRequestByID(SERVICE_REQUEST_ID)).thenReturn(buildServiceRequest());
        when(analysisRequestHandler.getAnalysisRequestStatusByClientIDAndClientSampleID(any(), any(), any()))
                .thenReturn(analysisRequestDTO);
        when(analysisRequestHandler.doesAnalysisRequestExists(any())).thenReturn(true);
        when(taskHandler.updateTaskStatus(any(), any())).thenReturn(task);
        when(taskHandler.updateTask(any(), any())).thenReturn(task);

        taskProcessor.process(createExchange(bundleOf(task), "c"));

        verify(taskHandler, times(1)).updateTaskStatus(task, "rejected");
        verify(taskHandler, times(1)).updateTask(any(), any());
        verify(analysisRequestHandler, never()).getAnalysisRequestByClientIDAndClientSampleID(any(), any(), any());
    }

    @Test
    void shouldCheckAtMostMaxTasksPerCycleStartingWithLeastRecentlyChecked() {
        Task first = buildOpenTask("task-1", "sr-1");
        Task second = buildOpenTask("task-2", "sr-2");
        Task third = buildOpenTask("task-3", "sr-3");
        Bundle bundle = bundleOf(first, second, third);
        taskProcessor.setMaxTasksPerCycle(2);

        ServiceRequest revoked = buildServiceRequest();
        revoked.setStatus(ServiceRequest.ServiceRequestStatus.REVOKED);
        when(taskHandler.doesTaskExists(any())).thenReturn(true);
        when(serviceRequestHandler.getServiceRequestByID(any())).thenReturn(revoked);

        taskProcessor.process(createExchange(bundle, "c"));
        taskProcessor.process(createExchange(bundle, "c"));
        taskProcessor.process(createExchange(bundle, "c"));

        ArgumentCaptor<String> checked = ArgumentCaptor.forClass(String.class);
        verify(serviceRequestHandler, times(6)).getServiceRequestByID(checked.capture());
        // Cycle 1: never checked, in server order. Cycle 2: sr-3 has never been checked, then sr-1.
        // Cycle 3: sr-2 has waited longest (cycle 1), then sr-1 and sr-3 (cycle 2) in server order.
        assertEquals(List.of("sr-1", "sr-2", "sr-3", "sr-1", "sr-2", "sr-1"), checked.getAllValues());
    }

    @Test
    void shouldCheckAllOpenTasksWhenMaxTasksPerCycleIsNotSet() {
        Bundle bundle = bundleOf(buildOpenTask("task-1", "sr-1"), buildOpenTask("task-2", "sr-2"));
        ServiceRequest revoked = buildServiceRequest();
        revoked.setStatus(ServiceRequest.ServiceRequestStatus.REVOKED);
        when(taskHandler.doesTaskExists(any())).thenReturn(true);
        when(serviceRequestHandler.getServiceRequestByID(any())).thenReturn(revoked);

        taskProcessor.process(createExchange(bundle, "c"));

        verify(serviceRequestHandler, times(2)).getServiceRequestByID(any());
    }

    private Task buildOpenTask(String taskId, String serviceRequestId) {
        Task task = getTask();
        task.setId(taskId);
        task.setBasedOn(Collections.singletonList(new Reference("ServiceRequest/" + serviceRequestId)));
        return task;
    }

    private Bundle bundleOf(Task... tasks) {
        Bundle bundle = new Bundle();
        for (Task task : tasks) {
            bundle.addEntry(new Bundle.BundleEntryComponent().setResource(task));
        }
        return bundle;
    }
}
