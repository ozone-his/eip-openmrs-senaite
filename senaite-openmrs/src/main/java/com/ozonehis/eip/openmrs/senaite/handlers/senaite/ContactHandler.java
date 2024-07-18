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
import com.ozonehis.eip.openmrs.senaite.model.contact.Contact;
import com.ozonehis.eip.openmrs.senaite.model.contact.ContactResponse;
import java.util.HashMap;
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
        ContactResponse savedContactResponse = objectMapper.readValue(response, ContactResponse.class);
        log.error("sendContact {}", savedContactResponse);
        return savedContactResponse.contactResponseToContact(savedContactResponse);
    }

    public Contact getContactByClientPath(ProducerTemplate producerTemplate, String clientPath)
            throws JsonProcessingException {
        Map<String, Object> headers = new HashMap<>();
        headers.put(Constants.HEADER_PATH, clientPath);
        String response =
                producerTemplate.requestBodyAndHeaders("direct:senaite-get-contact-route", null, headers, String.class);
        log.error("getContact response {}", response);
        ObjectMapper objectMapper = new ObjectMapper();
        ContactResponse contactResponse = objectMapper.readValue(response, ContactResponse.class);
        log.error("getContact {}", contactResponse);
        return contactResponse.contactResponseToContact(contactResponse);
    }
}
