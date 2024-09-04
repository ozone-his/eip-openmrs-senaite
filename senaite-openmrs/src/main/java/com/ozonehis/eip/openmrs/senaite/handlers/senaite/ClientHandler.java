/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openmrs.senaite.handlers.senaite;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ozonehis.eip.openmrs.senaite.Constants;
import com.ozonehis.eip.openmrs.senaite.model.client.ClientDAO;
import com.ozonehis.eip.openmrs.senaite.model.client.ClientMapper;
import com.ozonehis.eip.openmrs.senaite.model.client.request.Client;
import com.ozonehis.eip.openmrs.senaite.model.client.response.ClientResponse;
import java.util.HashMap;
import java.util.Map;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.ProducerTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Setter
@Component
public class ClientHandler {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public ClientDAO sendClient(ProducerTemplate producerTemplate, Client client) throws JsonProcessingException {
        String response = producerTemplate.requestBody("direct:senaite-create-client-route", client, String.class);
        ClientResponse savedClientResponse = objectMapper.readValue(response, ClientResponse.class);
        return ClientMapper.map(savedClientResponse);
    }

    public ClientDAO getClientByPatientID(ProducerTemplate producerTemplate, String patientID)
            throws JsonProcessingException {
        Map<String, Object> headers = new HashMap<>();
        headers.put(Constants.HEADER_CLIENT_ID, patientID);
        String response =
                producerTemplate.requestBodyAndHeaders("direct:senaite-get-client-route", null, headers, String.class);
        ClientResponse clientResponse = objectMapper.readValue(response, ClientResponse.class);
        return ClientMapper.map(clientResponse);
    }

    public boolean doesClientExists(ClientDAO clientDAO) {
        return clientDAO != null
                && clientDAO.getUid() != null
                && !clientDAO.getUid().isEmpty();
    }
}
