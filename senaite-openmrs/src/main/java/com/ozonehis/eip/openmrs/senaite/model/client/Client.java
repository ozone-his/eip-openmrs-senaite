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
import com.ozonehis.eip.openmrs.senaite.model.SenaiteResource;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Client implements SenaiteResource {

    //    <simple>{"portal_type":"Client","title":"${exchangeProperty.patient-name-unique}",
    //    "ClientID":"${exchangeProperty.patient-id}","parent_path":"/senaite/clients"}</simple>

    @JsonProperty("portal_type")
    private String portalType; // Client

    @JsonProperty("title")
    private String title; // patient-name-unique

    @JsonProperty("ClientID")
    private String clientID; // patient-id

    @JsonProperty("parent_path")
    private String parentPath; // /senaite/clients

    @JsonProperty("uid")
    private String uid;

    @JsonProperty("path")
    private String path; // /senaite/clients
}
