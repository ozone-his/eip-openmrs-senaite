/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openmrs.senaite.handlers.senaite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ozonehis.eip.openmrs.senaite.Constants;
import com.ozonehis.eip.openmrs.senaite.model.SenaiteResponseWrapper;
import com.ozonehis.eip.openmrs.senaite.model.client.ClientDTO;
import com.ozonehis.eip.openmrs.senaite.model.client.ClientMapper;
import com.ozonehis.eip.openmrs.senaite.model.client.request.Client;
import com.ozonehis.eip.openmrs.senaite.model.client.response.ClientItem;
import java.util.HashMap;
import java.util.Map;
import org.apache.camel.ProducerTemplate;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

class ClientHandlerTest {

    private static final String CLIENT_ID = "467aca2c-d069-40fc-90f4-17216fab0454";

    private static final String TITLE = "Siddharth Vaish (100000Y)";

    private static final String UID = "7f4aebaf3d4a4a2f8e8ebb5881d4ce73";

    private static final String PORTAL_TYPE = "Client";

    private static final String PARENT_PATH = "/senaite/clients";

    private static final String PATH = "/senaite/clients/client-1";

    @Mock
    private ProducerTemplate producerTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private ClientHandler clientHandler;

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
    void shouldSaveAndReturnSavedClient() throws JsonProcessingException {
        // Setup
        String responseBody = new Utils().readJSON("senaite/response/create-client.json");

        Client client = new Client();
        client.setPortalType(PORTAL_TYPE);
        client.setTitle(TITLE);
        client.setClientID(CLIENT_ID);
        client.setParentPath(PARENT_PATH);

        // Mock
        when(producerTemplate.requestBody(eq("direct:senaite-create-client-route"), eq(client), eq(String.class)))
                .thenReturn(responseBody);

        TypeReference<SenaiteResponseWrapper<ClientItem>> typeReference = new TypeReference<>() {};
        SenaiteResponseWrapper<ClientItem> responseWrapper = objectMapper.readValue(responseBody, typeReference);

        ClientDTO clientDTO = new ClientDTO();
        clientDTO.setPortalType(PORTAL_TYPE);
        clientDTO.setTitle(TITLE);
        clientDTO.setParentPath(PARENT_PATH);
        clientDTO.setUid(UID);
        clientDTO.setPath(PATH);

        when(ClientMapper.map(responseWrapper)).thenReturn(clientDTO);

        // Act
        ClientDTO result = clientHandler.sendClient(producerTemplate, client);

        // Verify
        assertEquals(clientDTO, result);
    }

    @Test
    void shouldReturnClientGivenPatientID() throws JsonProcessingException {
        // Setup
        String responseBody = new Utils().readJSON("senaite/response/get-client.json");
        Map<String, Object> headers = new HashMap<>();
        headers.put(Constants.HEADER_CLIENT_ID, CLIENT_ID);

        // Mock
        when(producerTemplate.requestBodyAndHeaders(
                        eq("direct:senaite-get-client-route"), isNull(), eq(headers), eq(String.class)))
                .thenReturn(responseBody);

        TypeReference<SenaiteResponseWrapper<ClientItem>> typeReference = new TypeReference<>() {};
        SenaiteResponseWrapper<ClientItem> responseWrapper = objectMapper.readValue(responseBody, typeReference);

        ClientDTO clientDTO = new ClientDTO();
        clientDTO.setPortalType(PORTAL_TYPE);
        clientDTO.setTitle(TITLE);
        clientDTO.setParentPath(PARENT_PATH);
        clientDTO.setUid(UID);
        clientDTO.setPath(PATH);
        clientDTO.setClientID(CLIENT_ID);

        when(ClientMapper.map(responseWrapper)).thenReturn(clientDTO);

        // Act
        ClientDTO result = clientHandler.getClientByPatientID(producerTemplate, CLIENT_ID);

        // Verify
        assertEquals(clientDTO, result);
    }
}
