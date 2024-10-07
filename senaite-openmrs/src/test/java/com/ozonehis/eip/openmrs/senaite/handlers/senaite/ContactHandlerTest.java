/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openmrs.senaite.handlers.senaite;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ozonehis.eip.openmrs.senaite.Constants;
import com.ozonehis.eip.openmrs.senaite.model.SenaiteResponseWrapper;
import com.ozonehis.eip.openmrs.senaite.model.contact.ContactDTO;
import com.ozonehis.eip.openmrs.senaite.model.contact.ContactMapper;
import com.ozonehis.eip.openmrs.senaite.model.contact.request.Contact;
import com.ozonehis.eip.openmrs.senaite.model.contact.response.ContactItem;
import java.util.HashMap;
import java.util.Map;
import org.apache.camel.ProducerTemplate;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

class ContactHandlerTest {
    @Mock
    private ProducerTemplate producerTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private ContactHandler contactHandler;

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
    void sendContact() throws JsonProcessingException {
        // Setup
        String responseBody = new Utils().readJSON("senaite/response/create-contact.json");

        Contact contact = new Contact();
        contact.setPortalType("Contact");
        contact.setParentPath("/senaite/clients/client-1");
        contact.setFirstName("Super");
        contact.setSurname("User");

        // Mock
        when(producerTemplate.requestBody(eq("direct:senaite-create-contact-route"), eq(contact), eq(String.class)))
                .thenReturn(responseBody);

        TypeReference<SenaiteResponseWrapper<ContactItem>> typeReference = new TypeReference<>() {};
        SenaiteResponseWrapper<ContactItem> responseWrapper = objectMapper.readValue(responseBody, typeReference);

        ContactDTO contactDTO = new ContactDTO();
        contactDTO.setPortalType("Contact");
        contactDTO.setParentPath("/senaite/clients/client-1");
        contactDTO.setFirstName("Super");
        contactDTO.setSurname("User");
        contactDTO.setTitle("Super User");
        contactDTO.setUid("1baac45668fc49cbbd5c4fd35d804b72");

        when(ContactMapper.map(responseWrapper)).thenReturn(contactDTO);

        // Act
        ContactDTO result = contactHandler.sendContact(producerTemplate, contact);

        // Verify
        assertEquals(contactDTO, result);
    }

    @Test
    void getContactByClientPath() throws JsonProcessingException {
        // Setup
        String responseBody = new Utils().readJSON("senaite/response/get-contact.json");
        Map<String, Object> headers = new HashMap<>();
        headers.put(Constants.HEADER_PATH, "/senaite/clients/client-1");

        // Mock
        when(producerTemplate.requestBodyAndHeaders(
                        eq("direct:senaite-get-contact-route"), isNull(), eq(headers), eq(String.class)))
                .thenReturn(responseBody);

        TypeReference<SenaiteResponseWrapper<ContactItem>> typeReference = new TypeReference<>() {};
        SenaiteResponseWrapper<ContactItem> responseWrapper = objectMapper.readValue(responseBody, typeReference);

        ContactDTO contactDTO = new ContactDTO();
        contactDTO.setPortalType("Contact");
        contactDTO.setParentPath("/senaite/clients/client-1");
        contactDTO.setUid("1baac45668fc49cbbd5c4fd35d804b72");
        contactDTO.setTitle("Super User");

        when(ContactMapper.map(responseWrapper)).thenReturn(contactDTO);

        // Act
        ContactDTO result = contactHandler.getContactByClientPath(producerTemplate, "/senaite/clients/client-1");

        // Verify
        assertEquals(contactDTO, result);
    }
}
