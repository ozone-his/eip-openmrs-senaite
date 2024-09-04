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
import com.ozonehis.eip.openmrs.senaite.model.contact.ContactDAO;
import com.ozonehis.eip.openmrs.senaite.model.contact.ContactMapper;
import com.ozonehis.eip.openmrs.senaite.model.contact.request.Contact;
import com.ozonehis.eip.openmrs.senaite.model.contact.response.ContactResponse;
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

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public ContactDAO sendContact(ProducerTemplate producerTemplate, Contact contact) throws JsonProcessingException {
        String response = producerTemplate.requestBody("direct:senaite-create-contact-route", contact, String.class);
        ContactResponse savedContactResponse = objectMapper.readValue(response, ContactResponse.class);
        return ContactMapper.map(savedContactResponse);
    }

    public ContactDAO getContactByClientPath(ProducerTemplate producerTemplate, String clientPath)
            throws JsonProcessingException {
        Map<String, Object> headers = new HashMap<>();
        headers.put(Constants.HEADER_PATH, clientPath);
        String response =
                producerTemplate.requestBodyAndHeaders("direct:senaite-get-contact-route", null, headers, String.class);
        ContactResponse contactResponse = objectMapper.readValue(response, ContactResponse.class);
        return ContactMapper.map(contactResponse);
    }

    public boolean doesContactExists(ContactDAO contactDAO) {
        return contactDAO != null
                && contactDAO.getUid() != null
                && !contactDAO.getUid().isEmpty();
    }
}
