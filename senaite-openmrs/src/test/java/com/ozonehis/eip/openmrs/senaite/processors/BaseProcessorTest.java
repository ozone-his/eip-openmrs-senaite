/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openmrs.senaite.processors;

import static org.openmrs.eip.fhir.Constants.HEADER_FHIR_EVENT_TYPE;

import com.ozonehis.eip.openmrs.senaite.model.analyses.AnalysesDTO;
import com.ozonehis.eip.openmrs.senaite.model.analysisRequest.AnalysisRequestDTO;
import com.ozonehis.eip.openmrs.senaite.model.analysisRequest.request.AnalysisRequest;
import com.ozonehis.eip.openmrs.senaite.model.analysisRequest.response.Analyses;
import com.ozonehis.eip.openmrs.senaite.model.analysisRequestTemplate.AnalysisRequestTemplateDTO;
import com.ozonehis.eip.openmrs.senaite.model.analysisRequestTemplate.response.SampleType;
import com.ozonehis.eip.openmrs.senaite.model.client.ClientDTO;
import com.ozonehis.eip.openmrs.senaite.model.client.request.Client;
import com.ozonehis.eip.openmrs.senaite.model.contact.ContactDTO;
import com.ozonehis.eip.openmrs.senaite.model.contact.request.Contact;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.apache.camel.support.DefaultMessage;
import org.apache.camel.test.spring.junit5.CamelSpringTestSupport;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.hl7.fhir.r4.model.Task;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.StaticApplicationContext;

public abstract class BaseProcessorTest extends CamelSpringTestSupport {

    protected static final String ENCOUNTER_REFERENCE_ID = "Encounter/1234";

    protected Exchange createExchange(Resource resource, String eventType) {
        Message message = new DefaultMessage(new DefaultCamelContext());
        message.setBody(resource);
        Map<String, Object> headers = new HashMap<>();
        headers.put(HEADER_FHIR_EVENT_TYPE, eventType);
        message.setHeaders(headers);
        Exchange exchange = new DefaultExchange(new DefaultCamelContext());
        exchange.setMessage(message);
        return exchange;
    }

    @Override
    protected AbstractApplicationContext createApplicationContext() {
        return new StaticApplicationContext();
    }

    protected Client getClient() {
        Client client = new Client();
        client.setPortalType("Client");
        client.setTitle("Siddharth Vaish (100000Y)");
        client.setClientID("bbaea498-e046-43c6-bf9c-dbbc7d39f38c");
        client.setParentPath("/senaite/clients");
        client.setUid("7f4aebaf3d4a4a2f8e8ebb5881d4ce73");
        client.setPath("/senaite/clients/client-1");
        return client;
    }

    protected ClientDTO getClientDTO() {
        ClientDTO clientDTO = new ClientDTO();
        clientDTO.setPortalType("Client");
        clientDTO.setTitle("Siddharth Vaish (100000Y)");
        clientDTO.setClientID("bbaea498-e046-43c6-bf9c-dbbc7d39f38c");
        clientDTO.setParentPath("/senaite/clients");
        clientDTO.setUid("7f4aebaf3d4a4a2f8e8ebb5881d4ce73");
        clientDTO.setPath("/senaite/clients/client-1");
        return clientDTO;
    }

    protected ContactDTO getContactDTO() {
        ContactDTO contactDTO = new ContactDTO();
        contactDTO.setPortalType("Contact");
        contactDTO.setParentPath("/senaite/clients/client-1");
        contactDTO.setUid("1baac45668fc49cbbd5c4fd35d804b72");
        contactDTO.setTitle("Super User");
        contactDTO.setFirstName("Super");
        contactDTO.setSurname("User");
        return contactDTO;
    }

    protected Contact getContact() {
        Contact contact = new Contact();
        contact.setPortalType("Contact");
        contact.setParentPath("/senaite/clients/client-1");
        contact.setFirstName("Super");
        contact.setSurname("User");
        contact.setUid("1baac45668fc49cbbd5c4fd35d804b72");
        return contact;
    }

    protected AnalysisRequestDTO getAnalysisRequestDTO() {
        AnalysisRequestDTO analysisRequestDTO = new AnalysisRequestDTO();
        analysisRequestDTO.setContact("1baac45668fc49cbbd5c4fd35d804b72");
        analysisRequestDTO.setSampleType("db97756c89f143408a18e0d152d0d337");
        analysisRequestDTO.setDateSampled("2024-10-07T08:20:31+00:00");
        analysisRequestDTO.setTemplate("7a09878065314fe29f8fc994fd2c8447");
        analysisRequestDTO.setProfiles(null);
        analysisRequestDTO.setAnalysesUids(new String[] {"06d1e902317047d1b90b27df5122ab78"});
        analysisRequestDTO.setClientSampleID("bbaea498-e046-43c6-bf9c-dbbc7d39f38c");
        analysisRequestDTO.setAnalyses(null);
        analysisRequestDTO.setAnalyses(new Analyses[] {
            new Analyses(
                    "http://senaite:8080/senaite/clients/client-1/URI-0001/LAB-030",
                    "06d1e902317047d1b90b27df5122ab78",
                    "http://senaite:8080/senaite/@@API/senaite/v1/analysis/06d1e902317047d1b90b27df5122ab78")
        });
        analysisRequestDTO.setUid("99afa09663054c18b1ae7a1cd2cd7693");
        return analysisRequestDTO;
    }

    protected AnalysisRequestTemplateDTO getAnalysisRequestTemplateDTO() {
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
        return analysisRequestTemplateDTO;
    }

    protected AnalysisRequest getAnalysisRequest() {
        AnalysisRequest analysisRequest = new AnalysisRequest();
        analysisRequest.setContact("1baac45668fc49cbbd5c4fd35d804b72");
        analysisRequest.setSampleType("db97756c89f143408a18e0d152d0d337");
        analysisRequest.setDateSampled("2024-10-07T08:20:31Z");
        analysisRequest.setTemplate("7a09878065314fe29f8fc994fd2c8447");
        analysisRequest.setAnalyses(new String[] {"8c057c866cf4439ca2c78e2bfc89f6a0"});
        analysisRequest.setClientSampleID("bbaea498-e046-43c6-bf9c-dbbc7d39f38c");
        analysisRequest.setReviewState("sample_due");
        return analysisRequest;
    }

    protected Task getTask() {
        Task task = new Task();
        task.setId("zzaea498-e046-12c6-bf9c-dbbc7d39f42c");
        task.setBasedOn(
                Collections.singletonList(new Reference("ServiceRequest/bbaea498-e046-43c6-bf9c-dbbc7d39f38c")));
        task.setStatus(Task.TaskStatus.RECEIVED);
        return task;
    }

    protected Patient buildPatient() {
        Patient patient = new Patient();
        patient.setId("ioaea498-e146-98c6-bf1c-dccc7d39f30d");
        patient.setActive(true);
        patient.setName(Collections.singletonList(
                new HumanName().setFamily("Doe").addGiven("John").setText("John Doe")));
        patient.setIdentifier(Collections.singletonList(
                new Identifier().setUse(Identifier.IdentifierUse.OFFICIAL).setValue("10IDH12H")));
        return patient;
    }

    protected ServiceRequest buildServiceRequest() {
        ServiceRequest serviceRequest = new ServiceRequest();
        serviceRequest.setStatus(ServiceRequest.ServiceRequestStatus.ACTIVE);
        serviceRequest.setIntent(ServiceRequest.ServiceRequestIntent.ORDER);
        serviceRequest.setSubject(new Reference("Patient/iiaea498-e046-09c6-bf9c-dbbc7d39f54c"));
        serviceRequest.setCode(
                new CodeableConcept().setCoding(Collections.singletonList(new Coding().setCode("123ABC"))));
        serviceRequest.setOccurrence(new Period().setStart(new Date(1628468672000L)));
        serviceRequest.setEncounter(new Reference(ENCOUNTER_REFERENCE_ID));
        return serviceRequest;
    }

    protected Encounter buildEncounter() {
        Encounter encounter = new Encounter();
        encounter.setPartOf(new Reference(ENCOUNTER_REFERENCE_ID));
        encounter.setPeriod(new Period().setStart(new Date(1728468672000L)));
        return encounter;
    }

    protected Observation buildObservation() {
        Observation observation = new Observation();
        observation.setId("itaea498-e022-43c6-bf9c-dbbc7d39f67i");
        return observation;
    }

    protected DiagnosticReport buildDiagnosticReport() {
        DiagnosticReport diagnosticReport = new DiagnosticReport();
        diagnosticReport.setId("opaea498-e022-43c6-bf9c-dbbc7d39f55u");
        diagnosticReport.setEncounter(new Reference("Encounter/" + ENCOUNTER_REFERENCE_ID));
        List<Reference> referenceList = new ArrayList<>();
        for (String observationUuid : new String[] {"itaea498-e022-43c6-bf9c-dbbc7d39f67i", "obs-2"}) {
            referenceList.add(new Reference()
                    .setReference("Observation/" + observationUuid)
                    .setType("Observation"));
        }
        diagnosticReport.setResult(referenceList);
        return diagnosticReport;
    }

    protected AnalysesDTO getAnalysesDTO() {
        AnalysesDTO analysesDTO = new AnalysesDTO();
        analysesDTO.setResult("4.5");
        analysesDTO.setResultCaptureDate("2024-09-30T11:21:36+00:00");
        analysesDTO.setDescription(
                "Blood test to measure the number of red blood cells.(679AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA)");
        return analysesDTO;
    }
}
