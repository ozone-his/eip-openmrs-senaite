package com.ozonehis.eip.openmrs.senaite.handlers.senaite;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ozonehis.eip.openmrs.senaite.model.client.Client;
import com.ozonehis.eip.openmrs.senaite.model.client.ClientResponse;
import java.util.Map;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.ProducerTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Setter
@Component
public class ClientHandler {

    public Client sendClient(ProducerTemplate producerTemplate, Client client) throws JsonProcessingException {
        String response = producerTemplate.requestBody("direct:senaite-create-client-route", client, String.class);
        log.error("sendClient response {}", response);
        ObjectMapper objectMapper = new ObjectMapper();
        Client savedClient = objectMapper.readValue(response, Client.class);
        log.error("sendClient {}", savedClient);
        return savedClient;
    }

    public Client getClient(ProducerTemplate producerTemplate, Map<String, Object> headers)
            throws JsonProcessingException {
        String response =
                producerTemplate.requestBodyAndHeaders("direct:senaite-get-client-route", null, headers, String.class);
        log.error("getClient response {}", response);
        ObjectMapper objectMapper = new ObjectMapper();
        ClientResponse clientResponse = objectMapper.readValue(response, ClientResponse.class);
        log.error("getClient {}", clientResponse);
        return clientResponse.clientResponseToClient(clientResponse);
    }
}
