/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openmrs.senaite.model.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ClientResponse {

    @JsonProperty("items")
    private ArrayList<ClientItem> clientItems;

    public Client clientResponseToClient(ClientResponse clientResponse) {
        Client client = new Client();
        if (clientResponse != null && !clientResponse.getClientItems().isEmpty()) {
            client.setClientID(clientResponse.getClientItems().get(0).getGetClientID());
            client.setPortalType(clientResponse.getClientItems().get(0).getPortalType());
            client.setTitle(clientResponse.getClientItems().get(0).getTitle());
            client.setUid(clientResponse.getClientItems().get(0).getUid());
            client.setParentPath(clientResponse.getClientItems().get(0).getParentPath());
            client.setPath(clientResponse.getClientItems().get(0).getPath());
            return client;
        }

        return null;
    }
}
