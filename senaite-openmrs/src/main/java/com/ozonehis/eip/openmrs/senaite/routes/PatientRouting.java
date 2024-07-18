/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openmrs.senaite.routes;

import static com.ozonehis.eip.openmrs.senaite.Constants.HEADER_ENABLE_PATIENT_SYNC;
import static org.openmrs.eip.fhir.Constants.HEADER_FHIR_EVENT_TYPE;

import com.ozonehis.eip.openmrs.senaite.converters.FhirResourceConverter;
import com.ozonehis.eip.openmrs.senaite.converters.SenaiteResourceConverter;
import com.ozonehis.eip.openmrs.senaite.processors.PatientProcessor;
import lombok.Setter;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Predicate;
import org.apache.camel.builder.RouteBuilder;
import org.hl7.fhir.r4.model.Patient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Setter
@Component
public class PatientRouting extends RouteBuilder {

    @Autowired
    private PatientProcessor patientProcessor;

    @Autowired
    private SenaiteResourceConverter senaiteResourceConverter;

    @Autowired
    private FhirResourceConverter fhirResourceConverter;

    @Value("${openmrs.senaite.enable.patient.sync}")
    private boolean isPatientSyncEnabled;

    Predicate isPatientSyncEnabled() {
        return exchange -> isPatientSyncEnabled
                || exchange.getMessage().getHeader(HEADER_ENABLE_PATIENT_SYNC, false, Boolean.class)
                || "u".equals(exchange.getMessage().getHeader(HEADER_FHIR_EVENT_TYPE, String.class));
    }

    @Override
    public void configure() {
        getContext().getTypeConverterRegistry().addTypeConverters(senaiteResourceConverter);
        getContext().getTypeConverterRegistry().addTypeConverters(fhirResourceConverter);
        // spotless:off
        from("direct:patient-to-client-router")
                .routeId("patient-to-client-router")
                .filter(exchange -> exchange.getMessage().getBody() instanceof Patient)
                .filter(isPatientSyncEnabled())
                .log(LoggingLevel.INFO, "Processing Patient")
                .process(patientProcessor)
                .choice()
                .when(header(HEADER_FHIR_EVENT_TYPE).isEqualTo("c"))
                .toD("direct:senaite-create-client-route")
                .endChoice()
                .when(header(HEADER_FHIR_EVENT_TYPE).isEqualTo("u"))
                .toD("direct:senaite-update-client-route")
                .endChoice()
                .end();

        from("direct:fhir-patient")
                .routeId("fhir-patient-to-client-router")
                .to("direct:patient-to-client-router")
                .end();
        // spotless:on
    }
}
