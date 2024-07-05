/*
 * Copyright © 2024, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openmrs.senaite.mapper.senaite;

import com.ozonehis.eip.openmrs.senaite.model.Analyses;
import com.ozonehis.eip.openmrs.senaite.model.AnalysisRequest;
import com.ozonehis.eip.openmrs.senaite.model.AnalysisRequestTemplate;
import com.ozonehis.eip.openmrs.senaite.model.client.Client;
import java.util.List;
import lombok.Setter;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.springframework.stereotype.Component;

@Setter
@Component
public class AnalysisRequestMapper {

    public AnalysisRequest toSenaite(
            Client client, AnalysisRequestTemplate analysisRequestTemplate, ServiceRequest serviceRequest) {
        if (serviceRequest == null) {
            return null;
        }
        serviceRequest.getCode().getCodingFirstRep().getCode();

        AnalysisRequest analysisRequest = new AnalysisRequest();
        analysisRequest.setContact(client.getClientItems().get(0).getUid());
        analysisRequest.setSampleType(
                analysisRequestTemplate.getItems().get(0).getSampleType().getUid());
        analysisRequest.setDateSampled(serviceRequest.getOccurrencePeriod().getStart());
        analysisRequest.setTemplate(analysisRequestTemplate.getItems().get(0).getUid());
        analysisRequest.setProfiles(
                analysisRequestTemplate.getItems().get(0).getAnalysisProfile().getUid());
        analysisRequest.setAnalyses(
                getAnalysesUids(analysisRequestTemplate.getItems().get(0).getAnalyses()));
        analysisRequest.setClientSampleID(serviceRequest.getId()); // TODO: Check

        return analysisRequest;
    }

    private String[] getAnalysesUids(List<Analyses> analysesList) {
        String[] uids = new String[analysesList.size()];
        for (int i = 0; i < analysesList.size(); i++) {
            uids[i] = analysesList.get(i).getServiceUid();
        }
        return uids;
    }
}
