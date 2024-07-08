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
        log.info("sendClient response {}", response);
        ObjectMapper objectMapper = new ObjectMapper();
        ClientResponse savedClientResponse = objectMapper.readValue(response, ClientResponse.class);
        log.info("sendClient {}", savedClientResponse);
        return savedClientResponse.clientResponseToClient(savedClientResponse);
    }

    public Client getClient(ProducerTemplate producerTemplate, Map<String, Object> headers)
            throws JsonProcessingException {
        String response =
                producerTemplate.requestBodyAndHeaders("direct:senaite-get-client-route", null, headers, String.class);
        log.info("getClient response {}", response);
        ObjectMapper objectMapper = new ObjectMapper();
        ClientResponse clientResponse = objectMapper.readValue(response, ClientResponse.class);
        log.info("getClient {}", clientResponse);
        return clientResponse.clientResponseToClient(clientResponse);
    }
}
