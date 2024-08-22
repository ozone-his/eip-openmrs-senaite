/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openmrs.senaite.mapper.senaite;

import com.ozonehis.eip.openmrs.senaite.model.analysisRequest.request.AnalysisRequest;
import com.ozonehis.eip.openmrs.senaite.model.analysisRequestTemplate.AnalysisRequestTemplateDAO;
import com.ozonehis.eip.openmrs.senaite.model.analysisRequestTemplate.response.Analyses;
import com.ozonehis.eip.openmrs.senaite.model.contact.ContactDAO;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Date;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.springframework.stereotype.Component;

@Slf4j
@Setter
@Component
public class AnalysisRequestMapper {

    public AnalysisRequest toSenaite(
            ContactDAO contactDAO,
            AnalysisRequestTemplateDAO analysisRequestTemplateDAO,
            ServiceRequest serviceRequest) {
        if (serviceRequest == null) {
            return null;
        }
        serviceRequest.getCode().getCodingFirstRep().getCode();

        AnalysisRequest analysisRequest = new AnalysisRequest();

        analysisRequest.setContact(contactDAO.getUid());
        analysisRequest.setDateSampled(convertOpenmrsDateToSenaiteDate(
                serviceRequest.getOccurrencePeriod().getStart()));
        analysisRequest.setClientSampleID(serviceRequest.getIdPart());
        analysisRequest.setReviewState("sample_due");

        if (analysisRequestTemplateDAO != null) {

            analysisRequest.setTemplate(analysisRequestTemplateDAO.getUid());

            if (analysisRequestTemplateDAO.getSampleType() != null) {
                analysisRequest.setSampleType(
                        analysisRequestTemplateDAO.getSampleType().getUid());
            }
            if (analysisRequestTemplateDAO.getAnalysisProfile() != null) {
                analysisRequest.setProfiles(
                        analysisRequestTemplateDAO.getAnalysisProfile().getUid());
            }
            if (analysisRequestTemplateDAO.getAnalyses() != null) {
                analysisRequest.setAnalyses(getAnalysesUids(analysisRequestTemplateDAO.getAnalyses()));
            }
        }

        return analysisRequest;
    }

    private String[] getAnalysesUids(Analyses[] analysesList) {

        return Arrays.stream(analysesList).map(Analyses::getServiceUid).toArray(String[]::new);
    }

    public String convertOpenmrsDateToSenaiteDate(Date openmrsDate) {
        long timestampInSeconds = openmrsDate.toInstant().getEpochSecond();
        Instant instant = Instant.ofEpochSecond(timestampInSeconds);

        return DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneOffset.UTC).format(instant);
    }
}
