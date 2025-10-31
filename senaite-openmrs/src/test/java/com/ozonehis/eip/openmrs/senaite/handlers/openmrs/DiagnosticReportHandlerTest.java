/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openmrs.senaite.handlers.openmrs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.ICreate;
import ca.uhn.fhir.rest.gclient.ICreateTyped;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

class DiagnosticReportHandlerTest {

    private static final String OBSERVATION_ID_1 = "obs-uuid-1";

    private static final String OBSERVATION_ID_2 = "obs-uuid-2";

    private static final String LAB_RESULT_ENCOUNTER_ID = "encounter-123";

    @Mock
    private IGenericClient openmrsFhirClient;

    @Mock
    private ICreate iCreate;

    @Mock
    private ICreateTyped iCreateTyped;

    @InjectMocks
    private DiagnosticReportHandler diagnosticReportHandler;

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
    void shouldSaveDiagnosticReport() {
        // Setup
        String diagnosticReportID = UUID.randomUUID().toString();
        DiagnosticReport diagnosticReport = new DiagnosticReport();
        diagnosticReport.setId(diagnosticReportID);

        MethodOutcome methodOutcome = new MethodOutcome();
        methodOutcome.setResource(diagnosticReport);
        methodOutcome.setCreated(true);

        // Mock behavior
        when(openmrsFhirClient.create()).thenReturn(iCreate);
        when(iCreate.resource(diagnosticReport)).thenReturn(iCreateTyped);
        when(iCreateTyped.encodedJson()).thenReturn(iCreateTyped);
        when(iCreateTyped.execute()).thenReturn(methodOutcome);

        // Act
        diagnosticReportHandler.sendDiagnosticReport(diagnosticReport);

        // Verify
        verify(openmrsFhirClient, times(1)).create();
        verify(iCreate, times(1)).resource(diagnosticReport);
        verify(iCreateTyped, times(1)).encodedJson();
        verify(iCreateTyped, times(1)).execute();
    }

    @Test
    void shouldReturnDiagnosticReportGivenObservationUidsServiceRequestAndLabResultEncounterID() {
        // Setup
        ArrayList<String> observationUuids = new ArrayList<>();
        observationUuids.add(OBSERVATION_ID_1);
        observationUuids.add(OBSERVATION_ID_2);

        ServiceRequest serviceRequest = new ServiceRequest();
        serviceRequest.setCode(new CodeableConcept().setText("Blood Test"));
        serviceRequest.setSubject(new Reference().setReference("Patient/123").setType("Patient"));

        // Act
        DiagnosticReport report = diagnosticReportHandler.buildDiagnosticReport(
                observationUuids, serviceRequest, LAB_RESULT_ENCOUNTER_ID);

        // Verify
        assertNotNull(report);
        assertEquals(DiagnosticReport.DiagnosticReportStatus.FINAL, report.getStatus());
        assertEquals("Blood Test", report.getCode().getText());
        assertEquals("Patient/123", report.getSubject().getReference());
        assertEquals(
                "Encounter/" + LAB_RESULT_ENCOUNTER_ID, report.getEncounter().getReference());
        assertEquals("Encounter", report.getEncounter().getType());

        List<Reference> resultReferences = report.getResult();
        assertEquals(2, resultReferences.size());

        assertEquals("Observation/" + OBSERVATION_ID_1, resultReferences.get(0).getReference());
        assertEquals("Observation", resultReferences.get(0).getType());

        assertEquals("Observation/" + OBSERVATION_ID_2, resultReferences.get(1).getReference());
        assertEquals("Observation", resultReferences.get(1).getType());
    }
}
