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
public class AnalysisRequestItem implements SenaiteResource {

    @JsonProperty("ContactUID")
    private String contactUid; // client-contact-uid

    @JsonProperty("SampleTypeUID")
    private String sampleTypeUid; // sample-type-uid

    @JsonProperty("DateSampled")
    private Date dateSampled; // lab-order-start-date

    @JsonProperty("TemplateUID")
    private String templateUid; // sample-template-uid

    @JsonProperty("ProfilesUID")
    private String[] profilesUid; // sample-analyses-profile-uid

    @JsonProperty("Analyses")
    private Analyses[] analyses; // [${exchangeProperty.sample-analyses-uids}]

    @JsonProperty("ClientSampleID")
    private String clientSampleID; // lab-order-uuid

    @JsonProperty("getClientUID")
    private String clientUID; // senaite client uid not OpenMRS patient id

    @JsonProperty("uid")
    private String uid; // AnalysisRequest uid

    @JsonProperty("review_state")
    private String reviewState; // service-request-transitioned-status
}
