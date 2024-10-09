/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openmrs.senaite.mapper.senaite;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.MockitoAnnotations.openMocks;

import com.ozonehis.eip.openmrs.senaite.model.client.request.Client;
import java.util.Collections;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;

class ClientMapperTest {

    private static final String PATIENT_ID = "ioaea498-e146-98c6-bf1c-dccc7d39f30d";

    private static final String IDENTIFIER = "10IDH12H";

    @InjectMocks
    private ClientMapper clientMapper;

    private static AutoCloseable mocksCloser;

    @BeforeEach
    void setUp() {
        mocksCloser = openMocks(this);
    }

    @AfterAll
    static void close() throws Exception {
        mocksCloser.close();
    }

    @Test
    void shouldReturnClientWhenPatientIsNotNull() {
        // Setup
        Patient patient = new Patient();
        patient.setId(PATIENT_ID);
        patient.setActive(true);
        patient.setName(Collections.singletonList(
                new HumanName().setFamily("Doe").addGiven("John").setText("John Doe")));
        patient.setIdentifier(Collections.singletonList(
                new Identifier().setUse(Identifier.IdentifierUse.OFFICIAL).setValue(IDENTIFIER)));

        // Act
        Client result = clientMapper.toSenaite(patient);

        // Verify
        assertEquals(patient.getIdPart(), result.getClientID());
        assertEquals("Client", result.getPortalType());
        assertEquals("John Doe (" + IDENTIFIER + ")", result.getTitle());
    }

    @Test
    void shouldReturnNullWhenPatientIsNull() {
        // Setup
        Patient patient = null;

        // Act
        Client result = clientMapper.toSenaite(patient);

        // Verify
        assertNull(result);
    }
}
