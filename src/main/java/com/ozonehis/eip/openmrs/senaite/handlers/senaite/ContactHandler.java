package com.ozonehis.eip.openmrs.senaite.handlers.senaite;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ozonehis.eip.openmrs.senaite.model.Contact;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.ProducerTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Setter
@Component
public class ContactHandler {

    public Contact sendContact(ProducerTemplate producerTemplate, Contact contact) throws JsonProcessingException {
        String response = producerTemplate.requestBody("direct:senaite-create-contact-route", contact, String.class);
        log.error("sendContact response {}", response);
        ObjectMapper objectMapper = new ObjectMapper();
        Contact savedContact = objectMapper.readValue(response, Contact.class);
        log.error("sendContact {}", response);
        return savedContact;
    }

    public Contact getContact(ProducerTemplate producerTemplate, String queryParams) throws JsonProcessingException {
        String response = producerTemplate.requestBody("direct:senaite-get-contact-route", null, String.class);
        log.error("getContact response {}", response);
        ObjectMapper objectMapper = new ObjectMapper();
        Contact contact = objectMapper.readValue(response, Contact.class);
        log.error("getContact {}", response);
        return contact;
    }
}
