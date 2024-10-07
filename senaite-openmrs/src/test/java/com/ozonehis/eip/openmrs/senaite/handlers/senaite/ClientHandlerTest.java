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
    void sendClient() throws JsonProcessingException {
        // Setup
        String responseBody = new Utils().readJSON("senaite/response/create-client.json");

        Client client = new Client();
        client.setPortalType("Client");
        client.setTitle("Siddharth Vaish (100000Y)");
        client.setClientID("467aca2c-d069-40fc-90f4-17216fab0454");
        client.setParentPath("/senaite/clients");

        // Mock
        when(producerTemplate.requestBody(eq("direct:senaite-create-client-route"), eq(client), eq(String.class)))
                .thenReturn(responseBody);

        TypeReference<SenaiteResponseWrapper<ClientItem>> typeReference = new TypeReference<>() {};
        SenaiteResponseWrapper<ClientItem> responseWrapper = objectMapper.readValue(responseBody, typeReference);

        ClientDTO clientDTO = new ClientDTO();
        clientDTO.setPortalType("Client");
        clientDTO.setTitle("Siddharth Vaish (100000Y)");
        clientDTO.setParentPath("/senaite/clients");
        clientDTO.setUid("7f4aebaf3d4a4a2f8e8ebb5881d4ce73");
        clientDTO.setPath("/senaite/clients/client-1");

        when(ClientMapper.map(responseWrapper)).thenReturn(clientDTO);

        // Act
        ClientDTO result = clientHandler.sendClient(producerTemplate, client);

        // Verify
        assertEquals(clientDTO, result);
    }

    @Test
    void getClientByPatientID() throws JsonProcessingException {
        // Setup
        String responseBody = new Utils().readJSON("senaite/response/get-client.json");
        Map<String, Object> headers = new HashMap<>();
        headers.put(Constants.HEADER_CLIENT_ID, "467aca2c-d069-40fc-90f4-17216fab0454");

        // Mock
        when(producerTemplate.requestBodyAndHeaders(
                        eq("direct:senaite-get-client-route"), isNull(), eq(headers), eq(String.class)))
                .thenReturn(responseBody);

        TypeReference<SenaiteResponseWrapper<ClientItem>> typeReference = new TypeReference<>() {};
        SenaiteResponseWrapper<ClientItem> responseWrapper = objectMapper.readValue(responseBody, typeReference);

        ClientDTO clientDTO = new ClientDTO();
        clientDTO.setPortalType("Client");
        clientDTO.setTitle("Siddharth Vaish (100000Y)");
        clientDTO.setParentPath("/senaite/clients");
        clientDTO.setUid("7f4aebaf3d4a4a2f8e8ebb5881d4ce73");
        clientDTO.setPath("/senaite/clients/client-1");
        clientDTO.setClientID("467aca2c-d069-40fc-90f4-17216fab0454");

        when(ClientMapper.map(responseWrapper)).thenReturn(clientDTO);

        // Act
        ClientDTO result = clientHandler.getClientByPatientID(producerTemplate, "467aca2c-d069-40fc-90f4-17216fab0454");

        // Verify
        assertEquals(clientDTO, result);
    }
}
