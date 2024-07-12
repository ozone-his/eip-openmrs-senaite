/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openmrs.senaite.model.contact;

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
public class Contact implements SenaiteResource {

    //  <simple>{"portal_type": "Contact","parent_path": "${exchangeProperty.client-storage-path}"
    //  ,"Firstname": "${exchangeProperty.requester-given-name}","Surname":
    // "${exchangeProperty.requester-family-name}"}</simple>

    @JsonProperty("portal_type")
    private String portalType; // Contact

    @JsonProperty("parent_path")
    private String parentPath; // client-storage-path

    @JsonProperty("Firstname")
    private String firstName; // requester-given-name

    @JsonProperty("Surname")
    private String surname; // requester-family-name

    @JsonProperty("uid")
    private String uid;
}
