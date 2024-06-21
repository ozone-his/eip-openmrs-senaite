/*
 * Copyright © 2024, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openmrs.senaite.mapper.senaite;

import com.ozonehis.eip.openmrs.senaite.model.Client;
import com.ozonehis.eip.openmrs.senaite.model.Contact;
import lombok.Setter;
import org.springframework.stereotype.Component;

@Setter
@Component
public class ContactMapper {

    public Contact toSenaite(Client client) {
        if (client == null) {
            return null;
        }
        Contact contact = new Contact();
        contact.setPortalType("Contact");
        if (!client.getItems().isEmpty()) {
            contact.setParentPath((String) client.getItems().get(0).get("path"));
        }

        String[] nameSplit = client.getTitle().split(" ");
        contact.setFirstName(nameSplit[0]);
        contact.setSurname(nameSplit[1]);

        return contact;
    }
}
