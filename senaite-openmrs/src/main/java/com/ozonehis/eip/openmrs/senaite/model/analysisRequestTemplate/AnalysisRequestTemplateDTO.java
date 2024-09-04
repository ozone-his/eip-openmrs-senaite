/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openmrs.senaite.model.analysisRequestTemplate;

import com.ozonehis.eip.openmrs.senaite.model.analysisRequestTemplate.response.Analyses;
import com.ozonehis.eip.openmrs.senaite.model.analysisRequestTemplate.response.AnalysisProfile;
import com.ozonehis.eip.openmrs.senaite.model.analysisRequestTemplate.response.SampleType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AnalysisRequestTemplateDTO {

    private String uid;

    private String path;

    private AnalysisProfile analysisProfile;

    private Analyses[] analyses;

    private SampleType sampleType;
}
