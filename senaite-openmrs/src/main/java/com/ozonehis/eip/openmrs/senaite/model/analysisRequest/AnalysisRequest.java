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

    @JsonProperty("Contact")
    private String contact; // Contact UUID

    @JsonProperty("SampleType")
    private String sampleType; // SampleType UUID

    @JsonProperty("DateSampled")
    private Date dateSampled;

    @JsonProperty("Template")
    private String template; // Template UUID

    @JsonProperty("Profiles")
    private String profiles; // Profiles UUID

    @JsonProperty("Analyses")
    private String[] analyses; // Array of Analyses UUID

    @JsonProperty("ClientSampleID")
    private String clientSampleID;

    @JsonProperty("Client")
    private String client; // SENAITE Client UUID, it's not OpenMRS Patient ID

    @JsonProperty("uid")
    private String uid; // SENAITE AnalysisRequest UUID

    @JsonProperty("review_state")
    private String reviewState;
}
