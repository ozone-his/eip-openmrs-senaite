/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openmrs.senaite.mapper.senaite;

import com.ozonehis.eip.openmrs.senaite.model.analysisRequest.AnalysisRequest;
import com.ozonehis.eip.openmrs.senaite.model.analysisRequestTemplate.Analyses;
import com.ozonehis.eip.openmrs.senaite.model.analysisRequestTemplate.AnalysisRequestTemplate;
import com.ozonehis.eip.openmrs.senaite.model.client.Client;
import com.ozonehis.eip.openmrs.senaite.model.contact.Contact;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.TimeZone;
import lombok.Setter;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.springframework.stereotype.Component;

@Setter
@Component
public class AnalysisRequestMapper {

    public AnalysisRequest toSenaite(
            Client client,
            Contact contact,
            AnalysisRequestTemplate analysisRequestTemplate,
            ServiceRequest serviceRequest) {
        if (serviceRequest == null) {
            return null;
        }
        serviceRequest.getCode().getCodingFirstRep().getCode();

        AnalysisRequest analysisRequest = new AnalysisRequest();

        analysisRequest.setContact(contact.getUid());
        // TODO: Fix
        //        analysisRequest.setDateSampled(
        //                convertDateFormat(serviceRequest.getOccurrencePeriod().getStart()));
        analysisRequest.setClientSampleID(serviceRequest.getIdPart());
        analysisRequest.setReviewState("sample_due");

        if (analysisRequestTemplate.getAnalysisRequestTemplateItems() != null
                && !analysisRequestTemplate.getAnalysisRequestTemplateItems().isEmpty()) {

            analysisRequest.setTemplate(analysisRequestTemplate
                    .getAnalysisRequestTemplateItems()
                    .get(0)
                    .getUid());

            if (analysisRequestTemplate.getAnalysisRequestTemplateItems().get(0).getSampleType() != null) {
                analysisRequest.setSampleType(analysisRequestTemplate
                        .getAnalysisRequestTemplateItems()
                        .get(0)
                        .getSampleType()
                        .getUid());
            }
            if (analysisRequestTemplate.getAnalysisRequestTemplateItems().get(0).getAnalysisProfile() != null) {
                analysisRequest.setProfiles(analysisRequestTemplate
                        .getAnalysisRequestTemplateItems()
                        .get(0)
                        .getAnalysisProfile()
                        .getUid());
            }
            if (analysisRequestTemplate.getAnalysisRequestTemplateItems().get(0).getAnalyses() != null) {
                analysisRequest.setAnalyses(getAnalysesUids(analysisRequestTemplate
                        .getAnalysisRequestTemplateItems()
                        .get(0)
                        .getAnalyses()));
            }
        }

        return analysisRequest;
    }

    private String[] getAnalysesUids(Analyses[] analysesList) {
        String[] uids = new String[analysesList.length];
        for (int i = 0; i < analysesList.length; i++) {
            uids[i] = analysesList[i].getServiceUid();
        }
        return uids;
    }

    public Date convertDateFormat(Date openmrsDate) {
        String inputDateStr = openmrsDate.toString(); // "Tue Oct 24 01:51:17 UTC 2023";
        DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:ss z yyyy");

        ZonedDateTime inputDateTime = ZonedDateTime.parse(inputDateStr, inputFormatter);

        DateTimeFormatter outputFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
        String isoFormattedDate = inputDateTime.format(outputFormatter);

        SimpleDateFormat isoDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
        isoDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

        try {
            return isoDateFormat.parse(isoFormattedDate);
        } catch (ParseException e) {
            return null;
        }
    }
}
