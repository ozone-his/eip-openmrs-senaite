/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openmrs.senaite.mapper.senaite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.MockitoAnnotations.openMocks;

import com.ozonehis.eip.openmrs.senaite.model.analysisRequest.request.AnalysisRequest;
import com.ozonehis.eip.openmrs.senaite.model.analysisRequestTemplate.AnalysisRequestTemplateDTO;
import com.ozonehis.eip.openmrs.senaite.model.analysisRequestTemplate.response.Analyses;
import com.ozonehis.eip.openmrs.senaite.model.analysisRequestTemplate.response.AnalysisProfile;
import com.ozonehis.eip.openmrs.senaite.model.analysisRequestTemplate.response.SampleType;
import com.ozonehis.eip.openmrs.senaite.model.contact.ContactDTO;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;

class AnalysisRequestMapperTest {

    @InjectMocks
    private AnalysisRequestMapper analysisRequestMapper;

    private static AutoCloseable mocksCloser;

    @BeforeEach
    void setUp() {
        mocksCloser = openMocks(this);
    }

    @AfterAll
    static void close() throws Exception {
        mocksCloser.close();
    }

    @Test
    void shouldReturnAnalysisRequestGivenContactAnalysisRequestTemplateAndServiceRequest() {
        // Setup
        ContactDTO contactDTO = getContactDTO();

        AnalysisRequestTemplateDTO analysisRequestTemplateDTO = getAnalysisRequestTemplate();

        ServiceRequest serviceRequest = getServiceRequest();

        // Act
        AnalysisRequest result =
                analysisRequestMapper.toSenaite(contactDTO, analysisRequestTemplateDTO, serviceRequest);

        // Verify
        assertEquals(contactDTO.getUid(), result.getContact());
        assertEquals("2024-10-09T10:11:12Z", result.getDateSampled());
        assertEquals(serviceRequest.getIdPart(), result.getClientSampleID());
        assertEquals("sample_due", result.getReviewState());
        assertEquals(analysisRequestTemplateDTO.getUid(), result.getTemplate());
        assertEquals(analysisRequestTemplateDTO.getSampleType().getUid(), result.getSampleType());
        assertEquals(analysisRequestTemplateDTO.getAnalysisProfile().getUid(), result.getProfiles());
        assertEquals(
                Arrays.stream(analysisRequestTemplateDTO.getAnalyses())
                        .map(Analyses::getServiceUid)
                        .toArray(String[]::new)[0],
                result.getAnalyses()[0]);
    }

    @Test
    void shouldReturnAnalysisRequestGivenContactAndServiceRequestWhenAnalysisRequestTemplateIsNull() {
        // Setup
        ContactDTO contactDTO = getContactDTO();

        AnalysisRequestTemplateDTO analysisRequestTemplateDTO = getAnalysisRequestTemplate();

        ServiceRequest serviceRequest = getServiceRequest();

        // Act
        AnalysisRequest result =
                analysisRequestMapper.toSenaite(contactDTO, analysisRequestTemplateDTO, serviceRequest);

        // Verify
        assertEquals(contactDTO.getUid(), result.getContact());
        assertEquals("2024-10-09T10:11:12Z", result.getDateSampled());
        assertEquals(serviceRequest.getIdPart(), result.getClientSampleID());
        assertEquals("sample_due", result.getReviewState());
    }

    @Test
    void shouldReturnNullWhenServiceRequestIsNull() {
        // Setup
        ContactDTO contactDTO = getContactDTO();

        AnalysisRequestTemplateDTO analysisRequestTemplateDTO = getAnalysisRequestTemplate();

        // Act
        AnalysisRequest result = analysisRequestMapper.toSenaite(contactDTO, analysisRequestTemplateDTO, null);

        // Verify
        assertNull(result);
    }

    private ServiceRequest getServiceRequest() {
        ServiceRequest serviceRequest = new ServiceRequest();
        serviceRequest.setStatus(ServiceRequest.ServiceRequestStatus.ACTIVE);
        serviceRequest.setIntent(ServiceRequest.ServiceRequestIntent.ORDER);
        serviceRequest.setSubject(new Reference("Patient/iiaea498-e046-09c6-bf9c-dbbc7d39f54c"));
        serviceRequest.setCode(
                new CodeableConcept().setCoding(Collections.singletonList(new Coding().setCode("123ABC"))));
        serviceRequest.setOccurrence(new Period().setStart(new Date(1728468672000L)));
        serviceRequest.setEncounter(new Reference("Encounter/1234"));
        return serviceRequest;
    }

    private ContactDTO getContactDTO() {
        ContactDTO contactDTO = new ContactDTO();
        contactDTO.setPortalType("Contact");
        contactDTO.setParentPath("/senaite/clients/client-1");
        contactDTO.setUid("1baac45668fc49cbbd5c4fd35d804b72");
        contactDTO.setTitle("Super User");
        contactDTO.setFirstName("Super");
        contactDTO.setSurname("User");
        return contactDTO;
    }

    private AnalysisRequestTemplateDTO getAnalysisRequestTemplate() {
        AnalysisRequestTemplateDTO analysisRequestTemplateDTO = new AnalysisRequestTemplateDTO();
        analysisRequestTemplateDTO.setUid("7a09878065314fe29f8fc994fd2c8447");
        analysisRequestTemplateDTO.setPath("/senaite/bika_setup/bika_artemplates/artemplate-8");
        analysisRequestTemplateDTO.setAnalysisProfile(null);
        analysisRequestTemplateDTO.setAnalyses(
                new com.ozonehis.eip.openmrs.senaite.model.analysisRequestTemplate.response.Analyses[] {
                    new com.ozonehis.eip.openmrs.senaite.model.analysisRequestTemplate.response.Analyses(
                            "8c057c866cf4439ca2c78e2bfc89f6a0", "part-1")
                });
        analysisRequestTemplateDTO.setSampleType(new SampleType(
                "http://senaite:8080/senaite/bika_setup/bika_sampletypes/sampletype-2",
                "db97756c89f143408a18e0d152d0d337",
                "http://senaite:8080/senaite/@@API/senaite/v1/sampletype/db97756c89f143408a18e0d152d0d337"));
        analysisRequestTemplateDTO.setAnalysisProfile(new AnalysisProfile(
                "http://senaite:8080/senaite/profile-1",
                "ac97756c89f143408a18e0d152d0d354",
                "http://senaite:8080/senaite/@@API/senaite/v1/profiles/ac97756c89f143408a18e0d152d0d354"));

        return analysisRequestTemplateDTO;
    }
}
