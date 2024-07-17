/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openmrs.senaite.converters;

import ca.uhn.fhir.context.FhirContext;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Converter;
import org.hl7.fhir.r4.model.Task;
import org.springframework.stereotype.Component;

@Slf4j
@Converter
@Component
public class TaskConverter {

    @Converter
    public static InputStream convertTaskToInputStream(Task task) {
        FhirContext ctx = FhirContext.forR4();
        String json = ctx.newJsonParser().encodeResourceToString(task);
        return new ByteArrayInputStream(json.getBytes());
    }
}