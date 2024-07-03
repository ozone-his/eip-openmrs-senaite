package com.ozonehis.eip.openmrs.senaite.handlers.senaite;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ozonehis.eip.openmrs.senaite.model.Client;
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
        log.error("sendClient {}", response);
        return savedClient;
    }

    public Client getClient(ProducerTemplate producerTemplate, String queryParams) throws JsonProcessingException {
        String response = producerTemplate.requestBody("direct:senaite-get-client-route", null, String.class);
        log.error("getClient response {}", response);
        ObjectMapper objectMapper = new ObjectMapper();
        Client client = objectMapper.readValue(response, Client.class);
        log.error("getClient {}", response);
        return client;
    }
}
