/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openmrs.senaite.model.analysisRequest;

import com.ozonehis.eip.openmrs.senaite.model.analysisRequest.response.Analyses;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AnalysisRequestDTO {

    private String contact; // Contact UUID

    private String sampleType; // SampleType UUID

    private String dateSampled;

    private String datePublished;

    private String template; // Template UUID

    private String profiles; // Profiles UUID

    private String[] analysesUids; // Array of Analyses UUID

    private String clientSampleID;

    private String client; // SENAITE Client UUID, it's not OpenMRS Patient ID

    private String uid; // SENAITE AnalysisRequest UUID

    private String reviewState;

    private Analyses[] analyses;
}
