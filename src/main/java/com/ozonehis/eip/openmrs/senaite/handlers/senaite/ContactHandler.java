package com.ozonehis.eip.openmrs.senaite.handlers.senaite;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ozonehis.eip.openmrs.senaite.model.contact.Contact;
import com.ozonehis.eip.openmrs.senaite.model.contact.ContactResponse;
import java.util.Map;
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

    public Contact getContact(ProducerTemplate producerTemplate, Map<String, Object> headers)
            throws JsonProcessingException {
        String response =
                producerTemplate.requestBodyAndHeaders("direct:senaite-get-contact-route", null, headers, String.class);
        log.error("getContact response {}", response);
        ObjectMapper objectMapper = new ObjectMapper();
        ContactResponse contactResponse = objectMapper.readValue(response, ContactResponse.class);
        log.error("getContact {}", contactResponse);
        return contactResponse.contactResponseToContact(contactResponse);
    }
}
