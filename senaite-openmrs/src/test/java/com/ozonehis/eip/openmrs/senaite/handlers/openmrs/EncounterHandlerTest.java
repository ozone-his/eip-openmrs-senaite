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
import ca.uhn.fhir.rest.gclient.IRead;
import ca.uhn.fhir.rest.gclient.IReadExecutable;
import ca.uhn.fhir.rest.gclient.IReadTyped;
import ca.uhn.fhir.rest.gclient.IUntypedQuery;
import java.util.Collections;
import java.util.UUID;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Encounter;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

class EncounterHandlerTest {

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
    void getEncounterByTypeAndSubject() {
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
        when(iQuery.returnBundle(Bundle.class)).thenReturn(iQuery);
        when(iQuery.execute()).thenReturn(bundle);

        // Act
        Encounter result = encounterHandler.getEncounterByTypeAndSubject("type-id", "subject-id");

        // Verify
        assertNotNull(result);
        assertEquals(encounterID, result.getId());
    }

    @Test
    void getEncounterByEncounterID() {
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
    void sendEncounter() {
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
    }

    @Test
    void buildLabResultEncounter() {}
}
