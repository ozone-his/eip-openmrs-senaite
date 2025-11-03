/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openmrs.senaite.processors;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.ozonehis.eip.openmrs.senaite.handlers.openmrs.TaskHandler;
import com.ozonehis.eip.openmrs.senaite.handlers.senaite.AnalysisRequestHandler;
import com.ozonehis.eip.openmrs.senaite.handlers.senaite.AnalysisRequestTemplateHandler;
import com.ozonehis.eip.openmrs.senaite.handlers.senaite.ClientHandler;
import com.ozonehis.eip.openmrs.senaite.handlers.senaite.ContactHandler;
import com.ozonehis.eip.openmrs.senaite.mapper.fhir.TaskMapper;
import com.ozonehis.eip.openmrs.senaite.mapper.senaite.AnalysisRequestMapper;
import com.ozonehis.eip.openmrs.senaite.mapper.senaite.ClientMapper;
import com.ozonehis.eip.openmrs.senaite.mapper.senaite.ContactMapper;
import com.ozonehis.eip.openmrs.senaite.model.analysisRequest.AnalysisRequestDTO;
import com.ozonehis.eip.openmrs.senaite.model.analysisRequest.request.AnalysisRequest;
import com.ozonehis.eip.openmrs.senaite.model.analysisRequestTemplate.AnalysisRequestTemplateDTO;
import com.ozonehis.eip.openmrs.senaite.model.client.ClientDTO;
import com.ozonehis.eip.openmrs.senaite.model.client.request.Client;
import com.ozonehis.eip.openmrs.senaite.model.contact.ContactDTO;
import com.ozonehis.eip.openmrs.senaite.model.contact.request.Contact;
import java.util.ArrayList;
import java.util.List;
import org.apache.camel.Exchange;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.hl7.fhir.r4.model.Task;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.openmrs.eip.EIPException;

class ServiceRequestProcessorTest extends BaseProcessorTest {

    @Mock
    private ClientMapper clientMapper;

    @Mock
    private ContactMapper contactMapper;

    @Mock
    private AnalysisRequestMapper analysisRequestMapper;

    @Mock
    private ClientHandler clientHandler;

    @Mock
    private AnalysisRequestHandler analysisRequestHandler;

    @Mock
    private ContactHandler contactHandler;

    @Mock
    private AnalysisRequestTemplateHandler analysisRequestTemplateHandler;

    @Mock
    private TaskHandler taskHandler;

    @Mock
    private TaskMapper taskMapper;

    @InjectMocks
    private ServiceRequestProcessor serviceRequestProcessor;

    private static AutoCloseable mocksCloser;

    @BeforeEach
    void setup() {
        mocksCloser = openMocks(this);
    }

    @AfterAll
    static void close() throws Exception {
        mocksCloser.close();
    }

    @Test
    public void shouldThrowExceptionWhenPatientIsMissingFromBundle() {
        // Setup
        Bundle bundle = new Bundle();
        List<Bundle.BundleEntryComponent> entries = new ArrayList<>();
        entries.add(new Bundle.BundleEntryComponent().setResource(buildEncounter()));
        entries.add(new Bundle.BundleEntryComponent().setResource(buildServiceRequest()));
        bundle.setEntry(entries);

        Exchange exchange = createExchange(bundle, "c");

        // Verify
        assertThrows(EIPException.class, () -> serviceRequestProcessor.process(exchange));
    }

    @Test
    public void shouldThrowExceptionWhenEventTypeIsNull() {
        // Setup
        Bundle bundle = new Bundle();
        List<Bundle.BundleEntryComponent> entries = new ArrayList<>();
        entries.add(new Bundle.BundleEntryComponent().setResource(buildPatient()));
        entries.add(new Bundle.BundleEntryComponent().setResource(buildEncounter()));
        entries.add(new Bundle.BundleEntryComponent().setResource(buildServiceRequest()));
        bundle.setEntry(entries);

        Exchange exchange = createExchange(bundle, null);

        // Verify
        assertThrows(EIPException.class, () -> serviceRequestProcessor.process(exchange));
    }

    @Test
    public void shouldCreateAnalysisRequestInOpenElisAndTaskInOpenMRSWhenClientContactDoesNotExist()
            throws JsonProcessingException {
        // Setup
        Patient patient = buildPatient();
        Encounter encounter = buildEncounter();
        ServiceRequest serviceRequest = buildServiceRequest();

        Bundle bundle = new Bundle();
        List<Bundle.BundleEntryComponent> entries = new ArrayList<>();
        entries.add(new Bundle.BundleEntryComponent().setResource(patient));
        entries.add(new Bundle.BundleEntryComponent().setResource(encounter));
        entries.add(new Bundle.BundleEntryComponent().setResource(serviceRequest));
        bundle.setEntry(entries);

        Client client = getClient();
        ClientDTO clientDTO = getClientDTO();
        ContactDTO contactDTO = getContactDTO();
        Contact contact = getContact();
        AnalysisRequestDTO analysisRequestDTO = getAnalysisRequestDTO();
        AnalysisRequest analysisRequest = getAnalysisRequest();
        AnalysisRequestTemplateDTO analysisRequestTemplateDTO = getAnalysisRequestTemplateDTO();
        Task task = getTask();

        when(clientMapper.toSenaite(patient)).thenReturn(client);

        when(clientHandler.getClientByPatientID(any(), eq(patient.getIdPart()))).thenReturn(clientDTO);
        when(clientHandler.doesClientExists(clientDTO)).thenReturn(false);
        when(clientHandler.sendClient(any(), eq(client))).thenReturn(clientDTO);

        when(contactHandler.getContactByClientPath(any(), eq(clientDTO.getPath())))
                .thenReturn(contactDTO);
        when(contactHandler.doesContactExists(contactDTO)).thenReturn(false);

        when(contactMapper.toSenaite(serviceRequest, clientDTO)).thenReturn(contact);
        when(contactHandler.sendContact(any(), eq(contact))).thenReturn(contactDTO);

        when(analysisRequestHandler.getAnalysisRequestByClientIDAndClientSampleID(
                        any(), eq(clientDTO.getClientID()), eq(serviceRequest.getIdPart())))
                .thenReturn(analysisRequestDTO);
        when(analysisRequestHandler.doesAnalysisRequestExists(analysisRequestDTO))
                .thenReturn(false);

        when(analysisRequestTemplateHandler.getAnalysisRequestTemplateByServiceRequestCode(
                        any(), eq(serviceRequest.getCode().getCoding().get(0).getCode())))
                .thenReturn(analysisRequestTemplateDTO);

        when(analysisRequestMapper.toSenaite(contactDTO, analysisRequestTemplateDTO, serviceRequest))
                .thenReturn(analysisRequest);
        when(analysisRequestHandler.sendAnalysisRequest(any(), eq(analysisRequest), eq(clientDTO.getUid())))
                .thenReturn(analysisRequestDTO);

        when(taskHandler.getTaskByServiceRequestID(serviceRequest.getIdPart())).thenReturn(task);
        when(taskHandler.doesTaskExists(task)).thenReturn(false);
        when(taskMapper.toFhir(analysisRequestDTO)).thenReturn(task);
        doNothing().when(taskHandler).sendTask(task);

        Exchange exchange = createExchange(bundle, "c");

        // Act
        serviceRequestProcessor.process(exchange);

        // Verify
        verify(clientMapper, times(1)).toSenaite(any());
        verify(clientHandler, times(1)).getClientByPatientID(any(), any());
        verify(clientHandler, times(1)).doesClientExists(any());
        verify(clientHandler, times(1)).sendClient(any(), any());
        verify(contactHandler, times(1)).getContactByClientPath(any(), any());
        verify(contactHandler, times(1)).doesContactExists(any());
        verify(contactHandler, times(1)).sendContact(any(), any());
        verify(analysisRequestHandler, times(1)).getAnalysisRequestByClientIDAndClientSampleID(any(), any(), any());
        verify(analysisRequestHandler, times(1)).doesAnalysisRequestExists(any());
        verify(analysisRequestTemplateHandler, times(1)).getAnalysisRequestTemplateByServiceRequestCode(any(), any());
        verify(analysisRequestMapper, times(1)).toSenaite(any(), any(), any());
        verify(analysisRequestHandler, times(1)).sendAnalysisRequest(any(), any(), any());
        verify(taskHandler, times(1)).getTaskByServiceRequestID(any());
        verify(taskHandler, times(1)).doesTaskExists(any());
        verify(taskMapper, times(1)).toFhir(any());
        verify(taskHandler, times(1)).sendTask(any());
    }

    @Test
    public void shouldLogErrorWhenAnalysisRequestTemplateDoesNotExistsForGivenServiceRequestConcept()
            throws JsonProcessingException {
        // Setup
        Patient patient = buildPatient();
        Encounter encounter = buildEncounter();
        ServiceRequest serviceRequest = buildServiceRequest();

        Bundle bundle = new Bundle();
        List<Bundle.BundleEntryComponent> entries = new ArrayList<>();
        entries.add(new Bundle.BundleEntryComponent().setResource(patient));
        entries.add(new Bundle.BundleEntryComponent().setResource(encounter));
        entries.add(new Bundle.BundleEntryComponent().setResource(serviceRequest));
        bundle.setEntry(entries);

        Client client = getClient();
        ClientDTO clientDTO = getClientDTO();
        ContactDTO contactDTO = getContactDTO();
        Contact contact = getContact();
        AnalysisRequestDTO analysisRequestDTO = getAnalysisRequestDTO();

        when(clientMapper.toSenaite(patient)).thenReturn(client);

        when(clientHandler.getClientByPatientID(any(), eq(patient.getIdPart()))).thenReturn(clientDTO);
        when(clientHandler.doesClientExists(clientDTO)).thenReturn(false);
        when(clientHandler.sendClient(any(), eq(client))).thenReturn(clientDTO);

        when(contactHandler.getContactByClientPath(any(), eq(clientDTO.getPath())))
                .thenReturn(contactDTO);
        when(contactHandler.doesContactExists(contactDTO)).thenReturn(false);

        when(contactMapper.toSenaite(serviceRequest, clientDTO)).thenReturn(contact);
        when(contactHandler.sendContact(any(), eq(contact))).thenReturn(contactDTO);

        when(analysisRequestHandler.getAnalysisRequestByClientIDAndClientSampleID(
                        any(), eq(clientDTO.getClientID()), eq(serviceRequest.getIdPart())))
                .thenReturn(analysisRequestDTO);
        when(analysisRequestHandler.doesAnalysisRequestExists(analysisRequestDTO))
                .thenReturn(false);

        when(analysisRequestTemplateHandler.getAnalysisRequestTemplateByServiceRequestCode(
                        any(), eq(serviceRequest.getCode().getCoding().get(0).getCode())))
                .thenReturn(null);

        Exchange exchange = createExchange(bundle, "c");

        // Act
        serviceRequestProcessor.process(exchange);

        // Verify
        verify(clientMapper, times(1)).toSenaite(any());
        verify(clientHandler, times(1)).getClientByPatientID(any(), any());
        verify(clientHandler, times(1)).doesClientExists(any());
        verify(clientHandler, times(1)).sendClient(any(), any());
        verify(contactHandler, times(1)).getContactByClientPath(any(), any());
        verify(contactHandler, times(1)).doesContactExists(any());
        verify(contactHandler, times(1)).sendContact(any(), any());
        verify(analysisRequestHandler, times(1)).getAnalysisRequestByClientIDAndClientSampleID(any(), any(), any());
        verify(analysisRequestHandler, times(1)).doesAnalysisRequestExists(any());
        verify(analysisRequestTemplateHandler, times(1)).getAnalysisRequestTemplateByServiceRequestCode(any(), any());
    }

    @Test
    public void shouldCancelAnalysisRequestWhenServiceRequestEventTypeIsDelete() throws JsonProcessingException {
        // Setup
        Patient patient = buildPatient();
        Encounter encounter = buildEncounter();
        ServiceRequest serviceRequest = buildServiceRequest();

        Bundle bundle = new Bundle();
        List<Bundle.BundleEntryComponent> entries = new ArrayList<>();
        entries.add(new Bundle.BundleEntryComponent().setResource(patient));
        entries.add(new Bundle.BundleEntryComponent().setResource(encounter));
        entries.add(new Bundle.BundleEntryComponent().setResource(serviceRequest));
        bundle.setEntry(entries);

        AnalysisRequestDTO analysisRequestDTO = getAnalysisRequestDTO();
        analysisRequestDTO.setReviewState("sample_due");

        when(analysisRequestHandler.getAnalysisRequestByClientSampleID(any(), eq(serviceRequest.getIdPart())))
                .thenReturn(analysisRequestDTO);
        when(analysisRequestHandler.cancelAnalysisRequest(any(), any(), eq(analysisRequestDTO.getUid())))
                .thenReturn(analysisRequestDTO);

        Exchange exchange = createExchange(bundle, "d");

        // Act
        serviceRequestProcessor.process(exchange);

        // Verify
        verify(analysisRequestHandler, times(1)).getAnalysisRequestByClientSampleID(any(), any());
        verify(analysisRequestHandler, times(1)).cancelAnalysisRequest(any(), any(), any());
    }

    @Test
    public void shouldCancelAnalysisRequestWhenServiceRequestStatusIsNotActive() throws JsonProcessingException {
        // Setup
        Patient patient = buildPatient();
        Encounter encounter = buildEncounter();
        ServiceRequest serviceRequest = buildServiceRequest();
        serviceRequest.setStatus(ServiceRequest.ServiceRequestStatus.REVOKED);

        Bundle bundle = new Bundle();
        List<Bundle.BundleEntryComponent> entries = new ArrayList<>();
        entries.add(new Bundle.BundleEntryComponent().setResource(patient));
        entries.add(new Bundle.BundleEntryComponent().setResource(encounter));
        entries.add(new Bundle.BundleEntryComponent().setResource(serviceRequest));
        bundle.setEntry(entries);

        AnalysisRequestDTO analysisRequestDTO = getAnalysisRequestDTO();
        analysisRequestDTO.setReviewState("sample_due");

        when(analysisRequestHandler.getAnalysisRequestByClientSampleID(any(), eq(serviceRequest.getIdPart())))
                .thenReturn(analysisRequestDTO);
        when(analysisRequestHandler.cancelAnalysisRequest(any(), any(), eq(analysisRequestDTO.getUid())))
                .thenReturn(analysisRequestDTO);

        Exchange exchange = createExchange(bundle, "d");

        // Act
        serviceRequestProcessor.process(exchange);

        // Verify
        verify(analysisRequestHandler, times(1)).getAnalysisRequestByClientSampleID(any(), any());
        verify(analysisRequestHandler, times(1)).cancelAnalysisRequest(any(), any(), any());
    }

    @Test
    public void shouldDoNothingIfAnalysisRequestNotInSampleDueStatus() throws JsonProcessingException {
        // Setup
        Patient patient = buildPatient();
        Encounter encounter = buildEncounter();
        ServiceRequest serviceRequest = buildServiceRequest();
        serviceRequest.setStatus(ServiceRequest.ServiceRequestStatus.REVOKED);

        Bundle bundle = new Bundle();
        List<Bundle.BundleEntryComponent> entries = new ArrayList<>();
        entries.add(new Bundle.BundleEntryComponent().setResource(patient));
        entries.add(new Bundle.BundleEntryComponent().setResource(encounter));
        entries.add(new Bundle.BundleEntryComponent().setResource(serviceRequest));
        bundle.setEntry(entries);

        AnalysisRequestDTO analysisRequestDTO = getAnalysisRequestDTO();
        analysisRequestDTO.setReviewState("received");

        when(analysisRequestHandler.getAnalysisRequestByClientSampleID(any(), eq(serviceRequest.getIdPart())))
                .thenReturn(analysisRequestDTO);

        Exchange exchange = createExchange(bundle, "d");

        // Act
        serviceRequestProcessor.process(exchange);

        // Verify
        verify(analysisRequestHandler, times(1)).getAnalysisRequestByClientSampleID(any(), any());
        verify(analysisRequestHandler, times(0)).cancelAnalysisRequest(any(), any(), any());
    }
}
