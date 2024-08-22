/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openmrs.senaite.model.client;

import com.ozonehis.eip.openmrs.senaite.model.client.response.ClientItem;
import com.ozonehis.eip.openmrs.senaite.model.client.response.ClientResponse;

public class ClientMapper {

    public static ClientDAO map(ClientResponse clientResponse) {
        ClientDAO clientDAO = new ClientDAO();
        if (clientResponse != null && !clientResponse.getClientItems().isEmpty()) {
            ClientItem clientItem = clientResponse.getClientItems().get(0);

            clientDAO.setClientID(clientItem.getGetClientID());
            clientDAO.setPortalType(clientItem.getPortalType());
            clientDAO.setTitle(clientItem.getTitle());
            clientDAO.setUid(clientItem.getUid());
            clientDAO.setParentPath(clientItem.getParentPath());
            clientDAO.setPath(clientItem.getPath());
            return clientDAO;
        }

        return null;
    }
}
