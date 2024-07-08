/*
 * Copyright © 2024, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openmrs.senaite.model.analysisRequestTemplate;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ozonehis.eip.openmrs.senaite.model.SenaiteResource;
import java.util.ArrayList;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AnalysisRequestTemplate implements SenaiteResource {

    //  {{senaite.baseUrl}}/@@API/senaite/v1/search?complete=true&amp;Description=
    //  ${exchangeProperty.service-analysis-template}&amp;catalog=senaite_catalog_setup&amp;portal_type=ARTemplate

    @JsonProperty("items")
    private ArrayList<AnalysisRequestTemplateItem>
            analysisRequestTemplateItems; // .analysisRequestTemplateItems[0].SampleType.uid
}
