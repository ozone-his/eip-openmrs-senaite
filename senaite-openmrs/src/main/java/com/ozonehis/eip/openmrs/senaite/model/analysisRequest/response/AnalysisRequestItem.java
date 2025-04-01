/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openmrs.senaite.model.analysisRequest.response;

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
public class AnalysisRequestItem implements SenaiteResource {

    @JsonProperty("ContactUID")
    private String contactUid;

    @JsonProperty("SampleTypeUID")
    private String sampleTypeUid;

    @JsonProperty("DateSampled")
    private String dateSampled;

    @JsonProperty("getDatePublished")
    private String datePublished;

    @JsonProperty("TemplateUID")
    private String templateUid;

    @JsonProperty("ProfilesUID")
    private String[] profilesUid;

    @JsonProperty("Analyses")
    private Analyses[] analyses;

    @JsonProperty("ClientSampleID")
    private String clientSampleID;

    @JsonProperty("getClientUID")
    private String clientUID;

    @JsonProperty("uid")
    private String uid;

    @JsonProperty("review_state")
    private String reviewState;
}
