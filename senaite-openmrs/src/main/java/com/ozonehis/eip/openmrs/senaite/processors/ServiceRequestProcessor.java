/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openmrs.senaite.processors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.ozonehis.eip.openmrs.senaite.handlers.openmrs.ServiceRequestHandler;
import com.ozonehis.eip.openmrs.senaite.handlers.openmrs.TaskHandler;
import com.ozonehis.eip.openmrs.senaite.handlers.senaite.AnalysisRequestHandler;
import com.ozonehis.eip.openmrs.senaite.handlers.senaite.AnalysisRequestTemplateHandler;
import com.ozonehis.eip.openmrs.senaite.handlers.senaite.ClientHandler;
import com.ozonehis.eip.openmrs.senaite.handlers.senaite.ContactHandler;
import com.ozonehis.eip.openmrs.senaite.mapper.fhir.TaskMapper;
import com.ozonehis.eip.openmrs.senaite.mapper.senaite.AnalysisRequestMapper;
import com.ozonehis.eip.openmrs.senaite.mapper.senaite.ClientMapper;
import com.ozonehis.eip.openmrs.senaite.mapper.senaite.ContactMapper;
import com.ozonehis.eip.openmrs.senaite.model.analysisRequest.AnalysisRequestDAO;
import com.ozonehis.eip.openmrs.senaite.model.analysisRequest.request.AnalysisRequest;
import com.ozonehis.eip.openmrs.senaite.model.analysisRequest.request.CancelAnalysisRequest;
import com.ozonehis.eip.openmrs.senaite.model.analysisRequestTemplate.AnalysisRequestTemplateDAO;
import com.ozonehis.eip.openmrs.senaite.model.client.ClientDAO;
import com.ozonehis.eip.openmrs.senaite.model.client.request.Client;
import com.ozonehis.eip.openmrs.senaite.model.contact.ContactDAO;
import com.ozonehis.eip.openmrs.senaite.model.contact.request.Contact;
import java.util.List;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.ProducerTemplate;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.hl7.fhir.r4.model.Task;
import org.openmrs.eip.EIPException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Setter
@Component
public class ServiceRequestProcessor implements Processor {

    @Autowired
    private ClientMapper clientMapper;

    @Autowired
    private ContactMapper contactMapper;

    @Autowired
    private AnalysisRequestMapper analysisRequestMapper;

    @Autowired
    private ClientHandler clientHandler;

    @Autowired
    private AnalysisRequestHandler analysisRequestHandler;

    @Autowired
    private ContactHandler contactHandler;

    @Autowired
    private AnalysisRequestTemplateHandler analysisRequestTemplateHandler;

    @Autowired
    private TaskHandler taskHandler;

    @Autowired
    private TaskMapper taskMapper;

    @Autowired
    private ServiceRequestHandler serviceRequestHandler;

    @Override
    public void process(Exchange exchange) {
        try (ProducerTemplate producerTemplate = exchange.getContext().createProducerTemplate()) {
            Bundle bundle = exchange.getMessage().getBody(Bundle.class);
            List<Bundle.BundleEntryComponent> entries = bundle.getEntry();

            Patient patient = null;
            Encounter encounter = null;
            ServiceRequest serviceRequest = null;
            for (Bundle.BundleEntryComponent entry : entries) {
                Resource resource = entry.getResource();
                if (resource instanceof Patient) {
                    patient = (Patient) resource;
                } else if (resource instanceof Encounter) {
                    encounter = (Encounter) resource;
                } else if (resource instanceof ServiceRequest) {
                    serviceRequest = (ServiceRequest) resource;
                }
            }

            if (patient == null || encounter == null || serviceRequest == null) {
                throw new EIPException("Invalid Bundle. Bundle must contain Patient, Encounter and ServiceRequest");
            } else {
                log.debug("Processing ServiceRequest for Patient with UUID {}", patient.getIdPart());
                String eventType = exchange.getMessage()
                        .getHeader(org.openmrs.eip.fhir.Constants.HEADER_FHIR_EVENT_TYPE, String.class);
                if (eventType == null) {
                    throw new IllegalArgumentException("Event type not found in the exchange headers.");
                }
                String serviceRequestUuid = serviceRequest.getIdPart();
                if ("c".equals(eventType) || "u".equals(eventType)) {
                    if (serviceRequest.getStatus().equals(ServiceRequest.ServiceRequestStatus.ACTIVE)
                            && serviceRequest.getIntent().equals(ServiceRequest.ServiceRequestIntent.ORDER)) {

                        Client client = clientMapper.toSenaite(patient);
                        ClientDAO savedClientDAO =
                                clientHandler.getClientByPatientID(producerTemplate, patient.getIdPart());
                        if (!clientHandler.doesClientExists(savedClientDAO)) {
                            savedClientDAO = clientHandler.sendClient(producerTemplate, client);
                        }
                        ContactDAO savedContactDAO =
                                contactHandler.getContactByClientPath(producerTemplate, savedClientDAO.getPath());
                        if (!contactHandler.doesContactExists(savedContactDAO)) {
                            Contact contact = contactMapper.toSenaite(serviceRequest, savedClientDAO);
                            savedContactDAO = contactHandler.sendContact(producerTemplate, contact);
                        }
                        AnalysisRequestDAO savedAnalysisRequestDAO =
                                analysisRequestHandler.getAnalysisRequestByClientIDAndClientSampleID(
                                        producerTemplate, savedClientDAO.getClientID(), serviceRequestUuid);
                        if (!analysisRequestHandler.doesAnalysisRequestExists(savedAnalysisRequestDAO)) {
                            String serviceRequestCodeID =
                                    serviceRequest.getCode().getCoding().get(0).getCode();
                            AnalysisRequestTemplateDAO analysisRequestTemplateDAO =
                                    analysisRequestTemplateHandler.getAnalysisRequestTemplateByServiceRequestCode(
                                            producerTemplate, serviceRequestCodeID);
                            if (analysisRequestTemplateDAO == null) {
                                log.error("No ARTemplate found in SENAITE code {}", serviceRequestCodeID);
                                // TODO: Should we throw an error if ARTemplate with serviceRequest code does not exists
                                return;
                            }
                            AnalysisRequest analysisRequest = analysisRequestMapper.toSenaite(
                                    savedContactDAO, analysisRequestTemplateDAO, serviceRequest);
                            savedAnalysisRequestDAO = analysisRequestHandler.sendAnalysisRequest(
                                    producerTemplate, analysisRequest, savedClientDAO.getUid());
                        }
                        Task savedTask = taskHandler.getTaskByServiceRequestID(producerTemplate, serviceRequestUuid);
                        if (!taskHandler.doesTaskExists(savedTask)) {
                            Task task = taskMapper.toFhir(savedAnalysisRequestDAO);
                            task.setStatus(Task.TaskStatus.REQUESTED);
                            taskHandler.sendTask(producerTemplate, task);
                        }

                    } else {
                        // Executed when MODIFY option is selected in OpenMRS
                        cancelAnalysisRequest(producerTemplate, serviceRequestUuid);
                    }
                } else if ("d".equals(eventType)) {
                    // Executed when DISCONTINUE option is selected in OpenMRS
                    cancelAnalysisRequest(producerTemplate, serviceRequestUuid);
                } else {
                    throw new IllegalArgumentException("Unsupported event type: " + eventType);
                }
            }
        } catch (Exception e) {
            throw new EIPException(String.format("Error processing ServiceRequest %s", e.getMessage()));
        }
    }

    private AnalysisRequestDAO cancelAnalysisRequest(ProducerTemplate producerTemplate, String serviceRequestUuid)
            throws JsonProcessingException {
        AnalysisRequestDAO analysisRequestDAO =
                analysisRequestHandler.getAnalysisRequestByClientSampleID(producerTemplate, serviceRequestUuid);
        if (analysisRequestDAO.getReviewState().equalsIgnoreCase("sample_due")) {
            CancelAnalysisRequest cancelAnalysisRequest = new CancelAnalysisRequest();
            cancelAnalysisRequest.setTransition("cancel");
            cancelAnalysisRequest.setClient(analysisRequestDAO.getClient());
            return analysisRequestHandler.cancelAnalysisRequest(
                    producerTemplate, cancelAnalysisRequest, analysisRequestDAO.getUid());
        } else {
            log.debug(
                    "ServiceRequestProcessor: AnalysisRequest {} is already cancelled for ServiceRequest id {}",
                    analysisRequestDAO,
                    serviceRequestUuid);
        }
        return null;
    }
}
