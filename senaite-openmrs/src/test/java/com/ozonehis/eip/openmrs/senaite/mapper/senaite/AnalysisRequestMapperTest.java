/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openmrs.senaite.mapper.senaite;

import static org.junit.jupiter.api.Assertions.*;

import com.ozonehis.eip.openmrs.senaite.model.analysisRequestTemplate.AnalysisRequestTemplateDTO;
import com.ozonehis.eip.openmrs.senaite.model.analysisRequestTemplate.response.SampleType;
import com.ozonehis.eip.openmrs.senaite.model.contact.ContactDTO;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AnalysisRequestMapperTest {

    // TODO
    @Test
    void toSenaite() {
        // Setup
        ContactDTO contactDTO = new ContactDTO();
        contactDTO.setUid(UUID.randomUUID().toString());
        contactDTO.setFirstName("John");
        contactDTO.setSurname("Doe");

        SampleType sampleType = new SampleType();
        sampleType.setUid(UUID.randomUUID().toString());
        sampleType.setUid("");

        AnalysisRequestTemplateDTO analysisRequestTemplateDTO = new AnalysisRequestTemplateDTO();
        analysisRequestTemplateDTO.setUid(UUID.randomUUID().toString());
        analysisRequestTemplateDTO.setSampleType(sampleType);
    }
}
