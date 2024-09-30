/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openmrs.senaite.handlers.openmrs;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.ICreate;
import ca.uhn.fhir.rest.gclient.ICreateTyped;
import java.util.UUID;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

class DiagnosticReportHandlerTest {

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
    void sendDiagnosticReport() {
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
    }

    @Test
    void buildDiagnosticReport() {}
}
