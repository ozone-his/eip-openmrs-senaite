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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
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
import java.util.List;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.hl7.fhir.r4.model.Task;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

class TaskProcessorTest {

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
    void process() throws JsonProcessingException {
        // Setup
        Task task = new Task();
        task.setId(TASK_ID);
        task.setStatus(Task.TaskStatus.RECEIVED);

        Bundle bundle = new Bundle();
        List<Bundle.BundleEntryComponent> entries = new ArrayList<>();
        entries.add(new Bundle.BundleEntryComponent().setResource(task));
        bundle.setEntry(entries);

        when(taskHandler.doesTaskExists(task)).thenReturn(true);

        ServiceRequest serviceRequest = new ServiceRequest();

        when(serviceRequestHandler.getServiceRequestByID(
                        task.getBasedOn().get(0).getReference()))
                .thenReturn(serviceRequest);
        //        doNothing().when(taskHandler).updateTask(any(),any());
        //        doNothing().when(taskHandler).markTaskRejected(task);

        String serviceRequestSubjectID =
                serviceRequest.getSubject().getReference().split("/")[1];
        String taskBasedOnReference = task.getBasedOn().get(0).getReference();

        AnalysisRequestDTO analysisRequestDTO = new AnalysisRequestDTO();
        when(analysisRequestHandler.getAnalysisRequestByClientIDAndClientSampleID(
                        any(), eq(serviceRequestSubjectID), eq(taskBasedOnReference)))
                .thenReturn(analysisRequestDTO);
        when(analysisRequestHandler.doesAnalysisRequestExists(analysisRequestDTO))
                .thenReturn(true);

        Encounter encounter = new Encounter();
        when(encounterHandler.getEncounterByTypeAndSubject(any(), eq(serviceRequestSubjectID)))
                .thenReturn(encounter);

        AnalysesDTO analysesDTO = new AnalysesDTO();
        when(analysesHandler.getAnalysesByAnalysesApiUrl(any(), any())).thenReturn(analysesDTO);

        Observation observation = new Observation();
        when(observationHandler.getObservationByCodeSubjectEncounterAndDate(any(), any(), any(), any()))
                .thenReturn(observation);
        when(observationHandler.doesObservationExists(observation)).thenReturn(false);

        when(observationHandler.sendObservation(any()));
        when(observationHandler.buildResultObservation(any(), any(), any(), any()))
                .thenReturn(observation);

        doNothing().when(diagnosticReportHandler).sendDiagnosticReport(any());

        DiagnosticReport diagnosticReport = new DiagnosticReport();
        when(diagnosticReportHandler.buildDiagnosticReport(any(), any(), any())).thenReturn(diagnosticReport);
    }
}
