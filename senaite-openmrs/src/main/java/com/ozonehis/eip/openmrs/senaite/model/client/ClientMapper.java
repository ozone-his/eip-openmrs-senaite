/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openmrs.senaite.model.client;

import com.ozonehis.eip.openmrs.senaite.model.SenaiteResponseWrapper;
import com.ozonehis.eip.openmrs.senaite.model.client.response.ClientItem;

public class ClientMapper {

    public static ClientDTO map(SenaiteResponseWrapper<ClientItem> clientResponse) {
        ClientDTO clientDTO = new ClientDTO();
        if (clientResponse != null && !clientResponse.getItems().isEmpty()) {
            ClientItem clientItem = clientResponse.getItems().get(0);

            clientDTO.setClientID(clientItem.getGetClientID());
            clientDTO.setPortalType(clientItem.getPortalType());
            clientDTO.setTitle(clientItem.getTitle());
            clientDTO.setUid(clientItem.getUid());
            clientDTO.setParentPath(clientItem.getParentPath());
            clientDTO.setPath(clientItem.getPath());
            return clientDTO;
        }

        return null;
    }
}
