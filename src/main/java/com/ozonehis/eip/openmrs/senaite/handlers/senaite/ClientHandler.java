package com.ozonehis.eip.openmrs.senaite.handlers.senaite;

import com.ozonehis.eip.openmrs.senaite.model.Client;
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

    public void sendClient(ProducerTemplate producerTemplate, String endpointUri, Client client) {
        Map<String, Object> clientHeaders = new HashMap<>();
        producerTemplate.sendBodyAndHeaders(endpointUri, client, clientHeaders);
    }
}
