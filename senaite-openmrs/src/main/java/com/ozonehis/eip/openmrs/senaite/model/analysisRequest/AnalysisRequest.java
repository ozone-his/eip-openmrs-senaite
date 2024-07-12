/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openmrs.senaite.model.analysisRequest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ozonehis.eip.openmrs.senaite.model.SenaiteResource;
import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AnalysisRequest implements SenaiteResource {

    //   <simple>{"Contact": "${exchangeProperty.client-contact-uid}","SampleType":
    // "${exchangeProperty.sample-type-uid}",
    //   "DateSampled": "${exchangeProperty.lab-order-start-date}","Template":
    // "${exchangeProperty.sample-template-uid}",
    //   "Profiles": "${exchangeProperty.sample-analyses-profile-uid}","Analyses":
    // [${exchangeProperty.sample-analyses-uids}],
    //   "ClientSampleID": "${exchangeProperty.lab-order-uuid}"}</simple>

    @JsonProperty("Contact")
    private String contact; // client-contact-uid

    @JsonProperty("SampleType")
    private String sampleType; // sample-type-uid

    @JsonProperty("DateSampled")
    private Date dateSampled; // lab-order-start-date

    @JsonProperty("Template")
    private String template; // sample-template-uid

    @JsonProperty("Profiles")
    private String profiles; // sample-analyses-profile-uid

    @JsonProperty("Analyses")
    private String[] analyses; // [${exchangeProperty.sample-analyses-uids}]

    @JsonProperty("ClientSampleID")
    private String clientSampleID; // lab-order-uuid

    @JsonProperty("review_state")
    private String reviewState; // service-request-transitioned-status
}
