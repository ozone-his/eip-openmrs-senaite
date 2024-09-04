/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openmrs.senaite.handlers.openmrs;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import java.util.ArrayList;
import java.util.List;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Setter
@Component
public class DiagnosticReportHandler {

    @Autowired
    private IGenericClient openmrsFhirClient;

    public void sendDiagnosticReport(DiagnosticReport diagnosticReport) {
        MethodOutcome methodOutcome = openmrsFhirClient
                .create()
                .resource(diagnosticReport)
                .encodedJson()
                .execute();

        log.debug("DiagnosticReportHandler: DiagnosticReport created {}", methodOutcome.getCreated());
    }

    public DiagnosticReport buildDiagnosticReport(
            ArrayList<String> observationUuids, ServiceRequest serviceRequest, String labResultsEncounterID) {
        DiagnosticReport diagnosticReport = new DiagnosticReport();
        diagnosticReport.setStatus(DiagnosticReport.DiagnosticReportStatus.FINAL);
        diagnosticReport.setCode(serviceRequest.getCode());
        diagnosticReport.setSubject(serviceRequest.getSubject());
        diagnosticReport.setEncounter(new Reference()
                .setReference("Encounter/" + labResultsEncounterID)
                .setType("Encounter"));
        List<Reference> referenceList = new ArrayList<>();
        for (String observationUuid : observationUuids) {
            referenceList.add(new Reference()
                    .setReference("Observation/" + observationUuid)
                    .setType("Observation"));
        }
        diagnosticReport.setResult(referenceList);
        return diagnosticReport;
    }
}
