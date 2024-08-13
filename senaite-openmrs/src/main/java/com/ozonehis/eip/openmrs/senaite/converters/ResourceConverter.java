/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openmrs.senaite.converters;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.MethodOutcome;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ozonehis.eip.openmrs.senaite.model.SenaiteResource;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Converter;
import org.apache.camel.Exchange;
import org.apache.camel.converter.stream.InputStreamCache;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.hl7.fhir.r4.model.DomainResource;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.hl7.fhir.r4.model.Task;
import org.springframework.stereotype.Component;

@Slf4j
@Converter
@Component
public class ResourceConverter {

    @Converter
    public static InputStream convertResourceToInputStream(DomainResource resource) {
        FhirContext ctx = FhirContext.forR4();
        String json = ctx.newJsonParser().encodeResourceToString(resource);
        return new ByteArrayInputStream(json.getBytes());
    }

    @Converter
    public static IBaseResource convertMethodOutcomeToIBaseResource(MethodOutcome outcome) {
        if (outcome.getResource() != null) {
            return (IBaseResource) outcome.getResource();
        } else {
            log.warn("The MethodOutcome does not contain a valid IBaseResource. Returning null.");
            return null;
        }
    }

    @Converter
    public static Encounter toEncounter(InputStreamCache inputStreamCache, Exchange exchange) throws Exception {
        FhirContext ctx = FhirContext.forR4();
        String body = exchange.getContext().getTypeConverter().mandatoryConvertTo(String.class, inputStreamCache);
        return (Encounter) ctx.newJsonParser().parseResource(Encounter.class, body);
    }

    @Converter
    public static Task toTask(InputStreamCache inputStreamCache, Exchange exchange) throws Exception {
        FhirContext ctx = FhirContext.forR4();
        String body = exchange.getContext().getTypeConverter().mandatoryConvertTo(String.class, inputStreamCache);
        return (Task) ctx.newJsonParser().parseResource(Task.class, body);
    }

    @Converter
    public static ServiceRequest toServiceRequest(InputStreamCache inputStreamCache, Exchange exchange)
            throws Exception {
        FhirContext ctx = FhirContext.forR4();
        String body = exchange.getContext().getTypeConverter().mandatoryConvertTo(String.class, inputStreamCache);
        return (ServiceRequest) ctx.newJsonParser().parseResource(ServiceRequest.class, body);
    }

    @Converter
    public static Observation toObservation(InputStreamCache inputStreamCache, Exchange exchange) throws Exception {
        FhirContext ctx = FhirContext.forR4();
        String body = exchange.getContext().getTypeConverter().mandatoryConvertTo(String.class, inputStreamCache);
        return (Observation) ctx.newJsonParser().parseResource(Observation.class, body);
    }

    @Converter
    public static DiagnosticReport toDiagnosticReport(InputStreamCache inputStreamCache, Exchange exchange)
            throws Exception {
        FhirContext ctx = FhirContext.forR4();
        String body = exchange.getContext().getTypeConverter().mandatoryConvertTo(String.class, inputStreamCache);
        return (DiagnosticReport) ctx.newJsonParser().parseResource(DiagnosticReport.class, body);
    }

    @Converter
    public static Bundle toBundle(InputStreamCache inputStreamCache, Exchange exchange) throws Exception {
        FhirContext ctx = FhirContext.forR4();
        String body = exchange.getContext().getTypeConverter().mandatoryConvertTo(String.class, inputStreamCache);
        return (Bundle) ctx.newJsonParser().parseResource(Bundle.class, body);
    }

    @Converter
    public static InputStream convertSenaiteResourceToInputStream(SenaiteResource senaiteResource) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        String json = objectMapper.writeValueAsString(senaiteResource);
        return new ByteArrayInputStream(json.getBytes());
    }
}
