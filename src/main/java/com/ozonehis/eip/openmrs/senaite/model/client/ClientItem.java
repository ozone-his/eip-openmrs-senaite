package com.ozonehis.eip.openmrs.senaite.model.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ClientItem {
    @JsonProperty("portal_type")
    private String portalType; // Client

    @JsonProperty("title")
    private String title; // patient-name-unique

    @JsonProperty("getClientID")
    private String getClientID; // patient-id

    @JsonProperty("parent_path")
    private String parentPath; // /senaite/clients

    @JsonProperty("uid")
    private String uid;

    @JsonProperty("path")
    private String path;
}
