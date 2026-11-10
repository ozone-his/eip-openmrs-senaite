/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openmrs.senaite.processors;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.ozonehis.eip.openmrs.senaite.handlers.bahmni.BahmniResultsHandler;
import com.ozonehis.eip.openmrs.senaite.handlers.openmrs.DiagnosticReportHandler;
import com.ozonehis.eip.openmrs.senaite.handlers.openmrs.EncounterHandler;
import com.ozonehis.eip.openmrs.senaite.handlers.openmrs.ObservationHandler;
import com.ozonehis.eip.openmrs.senaite.handlers.openmrs.ServiceRequestHandler;
import com.ozonehis.eip.openmrs.senaite.handlers.openmrs.TaskHandler;
import com.ozonehis.eip.openmrs.senaite.handlers.senaite.AnalysesHandler;
import com.ozonehis.eip.openmrs.senaite.handlers.senaite.AnalysisRequestHandler;
import com.ozonehis.eip.openmrs.senaite.model.analysisRequest.AnalysisRequestDTO;
import com.ozonehis.eip.openmrs.senaite.model.analysisRequest.response.Analyses;
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
    private static final String SERVICE_REQUEST_ID = "bbaea498-e046-43c6-bf9c-dbbc7d39f38c";

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

    @Mock
    private BahmniResultsHandler bahmniResultsHandler;

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
        when(analysisRequestHandler.getAnalysisRequestByClientIDAndClientSampleID(any(), any(), any()))
                .thenReturn(null);

        Exchange exchange = createExchange(bundle, "c"); // NOTE: EventType is not required

        // Act
        taskProcessor.process(exchange);

        // Verify
        verify(taskHandler, times(1)).doesTaskExists(any());
        verify(serviceRequestHandler, times(1)).getServiceRequestByID(SERVICE_REQUEST_ID);
        verify(analysisRequestHandler, times(1)).getAnalysisRequestByClientIDAndClientSampleID(any(), any(), any());
    }

    @Test
    void shouldNotUpdateAnalysisRequestWhenAnalysisRequestStatusIsNotCompleted() throws JsonProcessingException {
        assertResultsNotImported("sample_due");
    }

    @Test
    void shouldNotImportVerifiedResultsByDefault() throws JsonProcessingException {
        assertResultsNotImported("verified");
    }

    @Test
    void shouldNotImportPublishedResultsWhenConfiguredForVerified() throws JsonProcessingException {
        taskProcessor.setAnalysisRequestCompletedStatus("verified");
        assertResultsNotImported("published");
    }

    @Test
    void shouldNotImportResultsForUnknownStatus() throws JsonProcessingException {
        assertResultsNotImported("custom_ready");
    }

    @Test
    void shouldNotImportResultsWhenStatusIsMissing() throws JsonProcessingException {
        assertResultsNotImported(null);
    }

    private void assertResultsNotImported(String reviewState) throws JsonProcessingException {
        Task task = getTask();
        task.setStatus(Task.TaskStatus.REQUESTED);

        Bundle bundle = new Bundle();
        List<Bundle.BundleEntryComponent> entries = new ArrayList<>();
        entries.add(new Bundle.BundleEntryComponent().setResource(task));
        bundle.setEntry(entries);

        ServiceRequest serviceRequest = buildServiceRequest();
        AnalysisRequestDTO analysisRequestDTO = getAnalysisRequestDTO();
        analysisRequestDTO.setReviewState(reviewState);

        when(taskHandler.doesTaskExists(task)).thenReturn(true);
        when(serviceRequestHandler.getServiceRequestByID(SERVICE_REQUEST_ID)).thenReturn(serviceRequest);
        when(analysisRequestHandler.getAnalysisRequestByClientIDAndClientSampleID(any(), any(), any()))
                .thenReturn(analysisRequestDTO);
        when(analysisRequestHandler.doesAnalysisRequestExists(analysisRequestDTO))
                .thenReturn(true);

        Exchange exchange = createExchange(bundle, "c"); // NOTE: EventType is not required

        // Act
        taskProcessor.process(exchange);

        // Verify
        verify(taskHandler, times(1)).doesTaskExists(any());
        verify(serviceRequestHandler, times(1)).getServiceRequestByID(SERVICE_REQUEST_ID);
        verify(analysisRequestHandler, times(1)).getAnalysisRequestByClientIDAndClientSampleID(any(), any(), any());
        verifyNoInteractions(
                encounterHandler, analysesHandler, observationHandler, diagnosticReportHandler, bahmniResultsHandler);
        verify(taskHandler, never()).updateTaskStatus(any(), any());
        verify(taskHandler, never()).updateTask(any(), any());
    }

    @Test
    void shouldCreateResultsInOpenMRSWhenAnalysisRequestStatusIsCompleted() throws JsonProcessingException {
        assertResultsImported("published", false, "2024-10-01T08:00:00+00:00");
    }

    @Test
    void shouldImportVerifiedResultsInOpenMRSWhenConfigured() throws JsonProcessingException {
        taskProcessor.setAnalysisRequestCompletedStatus("verified");
        assertResultsImported("verified", false, null);
    }

    @Test
    void shouldImportVerifiedResultsInBahmniWithoutPublicationDate() throws JsonProcessingException {
        taskProcessor.setAnalysisRequestCompletedStatus("VERIFIED");
        assertResultsImported("verified", true, null);
    }

    @Test
    void shouldUseResultCaptureDateInBahmniWhenPublicationDateIsBlank() throws JsonProcessingException {
        taskProcessor.setAnalysisRequestCompletedStatus("verified");
        assertResultsImported("verified", true, " ");
    }

    @Test
    void shouldPreservePublicationDateInBahmni() throws JsonProcessingException {
        assertResultsImported("published", true, "2024-10-01T08:00:00+00:00");
    }

    @Test
    void shouldAllowCompletionStatusToOverrideSampleReceivedMapping() throws JsonProcessingException {
        taskProcessor.setAnalysisRequestCompletedStatus("sample_received");
        assertResultsImported("sample_received", false, null);
    }

    @Test
    void shouldImportResultsForCustomCompletionStatus() throws JsonProcessingException {
        taskProcessor.setAnalysisRequestCompletedStatus("custom_ready");
        assertResultsImported("custom_ready", true, null);
    }

    private void assertResultsImported(String reviewState, boolean bahmni, String datePublished)
            throws JsonProcessingException {
        taskProcessor.setRunWithBahmniEmr(Boolean.toString(bahmni));
        // Setup
        Task task = getTask();

        Bundle bundle = new Bundle();
        List<Bundle.BundleEntryComponent> entries = new ArrayList<>();
        entries.add(new Bundle.BundleEntryComponent().setResource(task));
        bundle.setEntry(entries);

        ServiceRequest serviceRequest = buildServiceRequest();
        AnalysisRequestDTO analysisRequestDTO = getAnalysisRequestDTO();
        analysisRequestDTO.setReviewState(reviewState);
        analysisRequestDTO.setDatePublished(datePublished);

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
        when(analysesHandler.getAnalysesByAnalysesApiUrl(any(), any())).thenReturn(resultAnalyses);
        if (bahmni) {
            Analyses analysis = analysisRequestDTO.getAnalyses()[0];
            analysisRequestDTO.setAnalyses(new Analyses[] {analysis, analysis});
            var earlierAnalysis = getAnalysesDTO();
            earlierAnalysis.setResultCaptureDate("2024-09-30T12:30:00+02:00");
            when(analysesHandler.getAnalysesByAnalysesApiUrl(any(), any())).thenReturn(earlierAnalysis, resultAnalyses);
            when(bahmniResultsHandler.getServiceRequestCodingIdentifier(serviceRequest))
                    .thenReturn("panel-concept");
            when(bahmniResultsHandler.buildAndSendBahmniResultObservation(any(), any(), any(), any(), any()))
                    .thenReturn(buildObservation());
        }
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
        verify(analysisRequestHandler, times(1)).getAnalysisRequestByClientIDAndClientSampleID(any(), any(), any());
        verify(analysisRequestHandler, times(1)).doesAnalysisRequestExists(any());
        verify(encounterHandler, times(1)).getEncounterByTypeAndSubjectAndStartDate(any(), any(), any());
        verify(encounterHandler, times(1)).getEncounterByEncounterID(any());
        verify(encounterHandler, times(1)).buildLabResultEncounter(any());
        verify(encounterHandler, times(1)).sendEncounter(any());
        var ordered = inOrder(observationHandler, diagnosticReportHandler, bahmniResultsHandler, taskHandler);
        if (bahmni) {
            String resultDate = datePublished == null || datePublished.isBlank()
                    ? resultAnalyses.getResultCaptureDate()
                    : datePublished;
            verify(analysesHandler, times(2)).getAnalysesByAnalysesApiUrl(any(), any());
            verify(observationHandler)
                    .getObservationByCodeSubjectEncounterAndDate(eq("panel-concept"), any(), any(), eq(resultDate));
            ordered.verify(bahmniResultsHandler)
                    .buildAndSendBahmniResultObservation(
                            any(), eq(savedEncounter), eq(serviceRequest), any(), eq(resultDate));
            verifyNoInteractions(diagnosticReportHandler);
        } else {
            verify(analysesHandler, times(1)).getAnalysesByAnalysesApiUrl(any(), any());
            verify(observationHandler, times(1))
                    .getObservationByCodeSubjectEncounterDateAndValue(any(), any(), any(), any(), any());
            verify(observationHandler, times(1))
                    .buildResultObservation(any(), any(), any(), any(), eq(resultAnalyses.getRemarks()));
            verify(observationHandler, times(1)).sendObservation(any());
            verify(diagnosticReportHandler, times(1)).buildDiagnosticReport(any(), any(), any());
            ordered.verify(diagnosticReportHandler).sendDiagnosticReport(any());
            verifyNoInteractions(bahmniResultsHandler);
        }
        ordered.verify(taskHandler).updateTaskStatus(task, "completed");
        ordered.verify(taskHandler).updateTask(updatedTask, task.getIdPart());
    }
}
