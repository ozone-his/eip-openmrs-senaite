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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.ICreate;
import ca.uhn.fhir.rest.gclient.ICreateTyped;
import ca.uhn.fhir.rest.gclient.ICriterion;
import ca.uhn.fhir.rest.gclient.IQuery;
import ca.uhn.fhir.rest.gclient.IRead;
import ca.uhn.fhir.rest.gclient.IReadExecutable;
import ca.uhn.fhir.rest.gclient.IReadTyped;
import ca.uhn.fhir.rest.gclient.IUntypedQuery;
import java.util.Collections;
import java.util.Date;
import java.util.UUID;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.Reference;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

class EncounterHandlerTest {

    private static final String TYPE_ID = "type-id-1";

    private static final String SUBJECT_ID = "subject-id-1";

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

    @Mock
    private IRead iRead;

    @Mock
    private IReadTyped<Encounter> iReadTyped;

    @Mock
    private IReadExecutable<Encounter> iReadExecutable;

    @InjectMocks
    private EncounterHandler encounterHandler;

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
    @SuppressWarnings("unchecked")
    void shouldReturnEncounterGivenTypeAndSubject() {
        // Setup
        String encounterID = UUID.randomUUID().toString();
        Encounter encounter = new Encounter();
        encounter.setId(encounterID);

        Bundle bundle = new Bundle();
        Bundle.BundleEntryComponent bundleEntryComponent = new Bundle.BundleEntryComponent();
        bundleEntryComponent.setResource(encounter);
        bundle.setEntry(Collections.singletonList(bundleEntryComponent));

        // Mock behavior
        when(openmrsFhirClient.search()).thenReturn(iUntypedQuery);
        when(iUntypedQuery.forResource(Encounter.class)).thenReturn(iQuery);
        when(iQuery.where(any(ICriterion.class))).thenReturn(iQuery);
        when(iQuery.and(any(ICriterion.class))).thenReturn(iQuery);
        when(iQuery.count(1)).thenReturn(iQuery);
        when(iQuery.returnBundle(Bundle.class)).thenReturn(iQuery);
        when(iQuery.execute()).thenReturn(bundle);

        // Act
        Encounter result = encounterHandler.getEncounterByTypeAndSubject(TYPE_ID, SUBJECT_ID);

        // Verify
        assertNotNull(result);
        assertEquals(encounterID, result.getId());
    }

    @Test
    void shouldReturnEncounterGivenEncounterID() {
        // Setup
        String encounterID = UUID.randomUUID().toString();
        Encounter encounter = new Encounter();
        encounter.setId(encounterID);

        // Mock behavior
        when(openmrsFhirClient.read()).thenReturn(iRead);
        when(iRead.resource(Encounter.class)).thenReturn(iReadTyped);
        when(iReadTyped.withId(encounterID)).thenReturn(iReadExecutable);
        when(iReadExecutable.execute()).thenReturn(encounter);

        // Act
        Encounter result = encounterHandler.getEncounterByEncounterID(encounterID);

        // Verify
        assertNotNull(result);
        assertEquals(encounterID, result.getId());
    }

    @Test
    void shouldSaveEncounter() {
        // Setup
        String encounterID = UUID.randomUUID().toString();
        Encounter encounter = new Encounter();
        encounter.setId(encounterID);

        MethodOutcome methodOutcome = new MethodOutcome();
        methodOutcome.setResource(encounter);
        methodOutcome.setCreated(true);

        // Mock behavior
        when(openmrsFhirClient.create()).thenReturn(iCreate);
        when(iCreate.resource(encounter)).thenReturn(iCreateTyped);
        when(iCreateTyped.encodedJson()).thenReturn(iCreateTyped);
        when(iCreateTyped.execute()).thenReturn(methodOutcome);

        // Act
        encounterHandler.sendEncounter(encounter);

        // Verify
        verify(openmrsFhirClient, times(1)).create();
        verify(iCreate, times(1)).resource(encounter);
        verify(iCreateTyped, times(1)).encodedJson();
        verify(iCreateTyped, times(1)).execute();
    }

    @Test
    void shouldReturnLabResultEncounterGivenEncounter() {
        // Setup
        Encounter orderEncounter = new Encounter();
        orderEncounter.setLocation(Collections.singletonList(
                new Encounter.EncounterLocationComponent().setLocation(new Reference().setReference("Location/123"))));
        orderEncounter.setPeriod(new Period().setStart(new Date()).setEnd(new Date()));
        orderEncounter.setSubject(new Reference().setReference("Patient/123").setType("Patient"));
        orderEncounter.setPartOf(new Reference().setReference("Encounter/345"));
        orderEncounter.setParticipant(Collections.singletonList(
                new Encounter.EncounterParticipantComponent().setIndividual(new Reference("Practitioner/456"))));

        // Act
        Encounter resultEncounter = encounterHandler.buildLabResultEncounter(orderEncounter);

        // Verify
        assertNotNull(resultEncounter);
        assertEquals(orderEncounter.getLocation(), resultEncounter.getLocation());

        assertNotNull(resultEncounter.getType());
        assertEquals(1, resultEncounter.getType().size());
        Coding coding = resultEncounter.getType().get(0).getCodingFirstRep();
        assertEquals("http://fhir.openmrs.org/code-system/encounter-type", coding.getSystem());
        assertEquals("Lab Results", coding.getDisplay());

        assertEquals(orderEncounter.getPeriod(), resultEncounter.getPeriod());
        assertEquals(orderEncounter.getSubject(), resultEncounter.getSubject());
        assertEquals(orderEncounter.getPartOf(), resultEncounter.getPartOf());
        assertEquals(orderEncounter.getParticipant(), resultEncounter.getParticipant());
    }
}
