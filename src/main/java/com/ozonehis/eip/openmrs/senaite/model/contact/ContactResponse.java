/*
 * Copyright © 2024, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openmrs.senaite.model.contact;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ozonehis.eip.openmrs.senaite.model.SenaiteResource;
import java.util.ArrayList;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ContactResponse implements SenaiteResource {

    //  <simple>{"portal_type": "Contact","parent_path": "${exchangeProperty.client-storage-path}"
    //  ,"Firstname": "${exchangeProperty.requester-given-name}","Surname":
    // "${exchangeProperty.requester-family-name}"}</simple>

    @JsonProperty("items")
    private ArrayList<ContactItem> contactItems; // items[0].uid

    public Contact contactResponseToContact(ContactResponse contactResponse) {
        Contact contact = new Contact();
        if (contactResponse != null && !contactResponse.getContactItems().isEmpty()) {
            contact.setFirstName(contactResponse.getContactItems().get(0).getFirstName());
            contact.setSurname(contactResponse.getContactItems().get(0).getSurname());
            contact.setPortalType(contactResponse.getContactItems().get(0).getPortalType());
            contact.setParentPath(contactResponse.getContactItems().get(0).getParentPath());
            contact.setUid(contactResponse.getContactItems().get(0).getUid());
            return contact;
        }

        return null;
    }
}
