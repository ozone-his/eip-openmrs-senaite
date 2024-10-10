/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openmrs.senaite.mapper.senaite;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.MockitoAnnotations.openMocks;

import com.ozonehis.eip.openmrs.senaite.model.client.ClientDTO;
import com.ozonehis.eip.openmrs.senaite.model.contact.request.Contact;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;

class ContactMapperTest {

    private static final String PATIENT_ID = "ioaea498-e146-98c6-bf1c-dccc7d39f30d";

    private static final String IDENTIFIER = "10IDH12H";

    @InjectMocks
    private ContactMapper contactMapper;

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
    void shouldReturnContactGivenServiceRequestAndClient() {
        // Setup
        ServiceRequest serviceRequest = new ServiceRequest();
        serviceRequest.setRequester(new Reference().setDisplay("Jane Nurse"));

        ClientDTO clientDTO = new ClientDTO();
        clientDTO.setPortalType("Client");
        clientDTO.setTitle("Siddharth Vaish (100000Y)");
        clientDTO.setClientID("bbaea498-e046-43c6-bf9c-dbbc7d39f38c");
        clientDTO.setParentPath("/senaite/clients");
        clientDTO.setUid("7f4aebaf3d4a4a2f8e8ebb5881d4ce73");
        clientDTO.setPath("/senaite/clients/client-1");

        // Act
        Contact result = contactMapper.toSenaite(serviceRequest, clientDTO);

        // Verify
        assertEquals("Jane", result.getFirstName());
        assertEquals("Nurse", result.getSurname());
        assertEquals(clientDTO.getPath(), result.getParentPath());
        assertEquals("Contact", result.getPortalType());
    }

    @Test
    void shouldReturnNullWhenServiceRequestIsNull() {
        // Act
        Contact result = contactMapper.toSenaite(null, new ClientDTO());

        // Verify
        assertNull(result);
    }
}
