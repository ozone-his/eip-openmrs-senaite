/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openmrs.senaite.model.contact;

import com.ozonehis.eip.openmrs.senaite.model.SenaiteResponseWrapper;
import com.ozonehis.eip.openmrs.senaite.model.contact.response.ContactItem;

public class ContactMapper {

    public static ContactDTO map(SenaiteResponseWrapper<ContactItem> contactResponse) {
        ContactDTO contactDTO = new ContactDTO();
        if (contactResponse != null && !contactResponse.getItems().isEmpty()) {
            ContactItem contactItem = contactResponse.getItems().get(0);

            contactDTO.setFirstName(contactItem.getFirstName());
            contactDTO.setSurname(contactItem.getSurname());
            contactDTO.setPortalType(contactItem.getPortalType());
            contactDTO.setParentPath(contactItem.getParentPath());
            contactDTO.setUid(contactItem.getUid());
            return contactDTO;
        }
        return null;
    }
}
