/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openmrs.senaite.handlers.openmrs;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.ICreate;
import ca.uhn.fhir.rest.gclient.ICreateTyped;
import ca.uhn.fhir.rest.gclient.ICriterion;
import ca.uhn.fhir.rest.gclient.IQuery;
import ca.uhn.fhir.rest.gclient.IUntypedQuery;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.UUID;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Quantity;
import org.hl7.fhir.r4.model.Reference;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

class ObservationHandlerTest {

    @Mock
    private IGenericClient openmrsFhirClient;

    @Mock
    private ICreate iCreate;

    @Mock
    private ICreateTyped iCreateTyped;

    @Mock
    private IUntypedQuery iUntypedQuery;

    @Mock
    private IQuery iQuery;

    @InjectMocks
    private ObservationHandler observationHandler;

    private static AutoCloseable mocksCloser;

    @AfterAll
    public static void close() throws Exception {
        mocksCloser.close();
    }

    @BeforeEach
    public void setup() {
        mocksCloser = openMocks(this);
    }

    @Test
    void getObservationByCodeSubjectEncounterAndDate() {
        // Setup
        String observationID = UUID.randomUUID().toString();
        Observation observation = new Observation();
        observation.setId(observationID);

        Bundle bundle = new Bundle();
        Bundle.BundleEntryComponent bundleEntryComponent = new Bundle.BundleEntryComponent();
        bundleEntryComponent.setResource(observation);
        bundle.setEntry(Collections.singletonList(bundleEntryComponent));

        // Mock behavior
        when(openmrsFhirClient.search()).thenReturn(iUntypedQuery);
        when(iUntypedQuery.forResource(Observation.class)).thenReturn(iQuery);
        when(iQuery.where(any(ICriterion.class))).thenReturn(iQuery);
        when(iQuery.and(any(ICriterion.class))).thenReturn(iQuery);
        when(iQuery.and(any(ICriterion.class))).thenReturn(iQuery);
        when(iQuery.returnBundle(Bundle.class)).thenReturn(iQuery);
        when(iQuery.execute()).thenReturn(bundle);

        // Act
        Observation result = observationHandler.getObservationByCodeSubjectEncounterAndDate(
                "code-id", "subject-id", "encounter-id", "2024-01-12");

        // Verify
        assertNotNull(result);
        assertEquals(observationID, result.getId());
    }

    @Test
    void sendObservation() {
        // Setup
        String observationID = UUID.randomUUID().toString();
        Observation observation = new Observation();
        observation.setId(observationID);

        MethodOutcome methodOutcome = new MethodOutcome();
        methodOutcome.setResource(observation);
        methodOutcome.setCreated(true);

        // Mock behavior
        when(openmrsFhirClient.create()).thenReturn(iCreate);
        when(iCreate.resource(observation)).thenReturn(iCreateTyped);
        when(iCreateTyped.encodedJson()).thenReturn(iCreateTyped);
        when(iCreateTyped.execute()).thenReturn(methodOutcome);

        // Act
        observationHandler.sendObservation(observation);
    }

    @Test
    void buildResultObservation() {
        // Setup
        Encounter savedResultEncounter = new Encounter();
        savedResultEncounter.setId("encounter-123");
        savedResultEncounter.setSubject(new Reference("Patient/456"));

        String conceptUuid = "concept-uuid-123";
        String analysesResult = "23";
        String analysesResultCaptureDate = "2024-09-30T11:21:36+00:00";

        // Act
        Observation observation = observationHandler.buildResultObservation(
                savedResultEncounter, conceptUuid, analysesResult, analysesResultCaptureDate);

        // Verify
        assertNotNull(observation);
        assertEquals(Observation.ObservationStatus.FINAL, observation.getStatus());

        CodeableConcept code = observation.getCode();
        assertNotNull(code);
        Coding coding = code.getCodingFirstRep();
        assertEquals(conceptUuid, coding.getCode());
        assertEquals(savedResultEncounter.getSubject(), observation.getSubject());
        assertEquals(
                Date.from(Instant.parse(analysesResultCaptureDate)),
                ((DateTimeType) observation.getEffective()).getValue());
        assertEquals(Quantity.class, observation.getValue().getClass());
        assertEquals("Encounter/encounter-123", observation.getEncounter().getReference());
    }
}
