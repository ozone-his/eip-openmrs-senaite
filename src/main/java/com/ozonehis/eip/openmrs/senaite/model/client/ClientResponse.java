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
    private ArrayList<ClientItem> clientItems; // items[0].uid AND items[0].path

    public Client clientResponseToClient(ClientResponse clientResponse) {
        Client client = new Client();
        if (clientResponse != null && !clientResponse.getClientItems().isEmpty()) {
            client.setClientID(clientResponse.getClientItems().get(0).getGetClientID());
            client.setPortalType(clientResponse.getClientItems().get(0).getPortalType());
            client.setTitle(clientResponse.getClientItems().get(0).getTitle());
            client.setUid(clientResponse.getClientItems().get(0).getUid());
            client.setClientItems(clientResponse.getClientItems());
            return client;
        }

        return null;
    }
}
