/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openmrs.senaite.model.contact;

import com.ozonehis.eip.openmrs.senaite.model.contact.response.ContactItem;
import com.ozonehis.eip.openmrs.senaite.model.contact.response.ContactResponse;

public class ContactMapper {

    public static ContactDAO map(ContactResponse contactResponse) {
        ContactDAO contactDAO = new ContactDAO();
        if (contactResponse != null && !contactResponse.getContactItems().isEmpty()) {
            ContactItem contactItem = contactResponse.getContactItems().get(0);

            contactDAO.setFirstName(contactItem.getFirstName());
            contactDAO.setSurname(contactItem.getSurname());
            contactDAO.setPortalType(contactItem.getPortalType());
            contactDAO.setParentPath(contactItem.getParentPath());
            contactDAO.setUid(contactItem.getUid());
            return contactDAO;
        }
        return null;
    }
}
