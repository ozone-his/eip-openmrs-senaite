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
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.IRead;
import ca.uhn.fhir.rest.gclient.IReadExecutable;
import ca.uhn.fhir.rest.gclient.IReadTyped;
import java.util.UUID;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

class ServiceRequestHandlerTest {

    @Mock
    private IGenericClient openmrsFhirClient;

    @Mock
    private IRead iRead;

    @Mock
    private IReadTyped<ServiceRequest> iReadTyped;

    @Mock
    private IReadExecutable<ServiceRequest> iReadExecutable;

    @InjectMocks
    private ServiceRequestHandler serviceRequestHandler;

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
    void shouldReturnServiceRequestGivenServiceRequestID() {
        // Setup
        String serviceRequestID = UUID.randomUUID().toString();
        ServiceRequest serviceRequest = new ServiceRequest();
        serviceRequest.setId(serviceRequestID);

        // Mock behavior
        when(openmrsFhirClient.read()).thenReturn(iRead);
        when(iRead.resource(ServiceRequest.class)).thenReturn(iReadTyped);
        when(iReadTyped.withId(serviceRequestID)).thenReturn(iReadExecutable);
        when(iReadExecutable.execute()).thenReturn(serviceRequest);

        // Act
        ServiceRequest result = serviceRequestHandler.getServiceRequestByID(serviceRequestID);

        // Verify
        assertNotNull(result);
        assertEquals(serviceRequestID, result.getId());
    }
}
