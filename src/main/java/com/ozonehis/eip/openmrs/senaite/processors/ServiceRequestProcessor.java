/*
 * Copyright © 2024, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openmrs.senaite.processors;

import com.ozonehis.eip.openmrs.openmrs.handlers.openmrs.TaskHandler;
import com.ozonehis.eip.openmrs.senaite.Constants;
import com.ozonehis.eip.openmrs.senaite.handlers.senaite.AnalysisRequestHandler;
import com.ozonehis.eip.openmrs.senaite.handlers.senaite.AnalysisRequestTemplateHandler;
import com.ozonehis.eip.openmrs.senaite.handlers.senaite.ClientHandler;
import com.ozonehis.eip.openmrs.senaite.handlers.senaite.ContactHandler;
import com.ozonehis.eip.openmrs.senaite.mapper.fhir.TaskMapper;
import com.ozonehis.eip.openmrs.senaite.mapper.senaite.AnalysisRequestMapper;
import com.ozonehis.eip.openmrs.senaite.mapper.senaite.ClientMapper;
import com.ozonehis.eip.openmrs.senaite.mapper.senaite.ContactMapper;
import com.ozonehis.eip.openmrs.senaite.model.analysisRequest.AnalysisRequest;
import com.ozonehis.eip.openmrs.senaite.model.analysisRequestTemplate.AnalysisRequestTemplate;
import com.ozonehis.eip.openmrs.senaite.model.client.Client;
import com.ozonehis.eip.openmrs.senaite.model.contact.Contact;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.CamelExecutionException;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.ProducerTemplate;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.hl7.fhir.r4.model.Task;
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
                throw new CamelExecutionException(
                        "Invalid Bundle. Bundle must contain Patient, Encounter and ServiceRequest", exchange);
            } else {
                log.debug("Processing ServiceRequest for Patient with UUID {}", patient.getIdPart());
                String eventType = exchange.getMessage()
                        .getHeader(org.openmrs.eip.fhir.Constants.HEADER_FHIR_EVENT_TYPE, String.class);
                if (eventType == null) {
                    throw new IllegalArgumentException("Event type not found in the exchange headers.");
                }
                String serviceRequestUuid = serviceRequest.getIdPart(); // TODO: check it should be {body.identifier}
                if ("c".equals(eventType) || "u".equals(eventType)) {
                    if (serviceRequest.getStatus().equals(ServiceRequest.ServiceRequestStatus.ACTIVE)
                            && serviceRequest.getIntent().equals(ServiceRequest.ServiceRequestIntent.ORDER)) {
                        Map<String, Object> headers = new HashMap<>();
                        Client client = clientMapper.toSenaite(patient);
                        log.info("Mapped client from patient {}", client);

                        headers.put(Constants.HEADER_CLIENT_ID, client.getClientID());
                        Client savedClient = clientHandler.getClient(producerTemplate, headers);
                        log.info("Fetched client {}", savedClient);

                        if (savedClient == null
                                || savedClient.getUid() == null
                                || savedClient.getUid().isEmpty()) {
                            savedClient = clientHandler.sendClient(producerTemplate, client);
                            log.info("Saved client {}", savedClient);
                        }
                        headers.put(Constants.HEADER_PATH, savedClient.getPath());
                        Contact savedContact = contactHandler.getContact(producerTemplate, headers);
                        log.info("Fetched contact {}", savedContact);
                        if (savedContact == null
                                || savedContact.getUid() == null
                                || savedContact.getUid().isEmpty()) {
                            Contact contact = contactMapper.toSenaite(serviceRequest, savedClient);
                            log.info("Mapped contact from savedClient {}", contact);
                            savedContact = contactHandler.sendContact(producerTemplate, contact);
                            log.info("Saved contact {}", savedContact);
                        }
                        headers.put(Constants.HEADER_CLIENT_ID, savedClient.getClientID());
                        headers.put(Constants.HEADER_CLIENT_SAMPLE_ID, serviceRequestUuid);
                        AnalysisRequest savedAnalysisRequest =
                                analysisRequestHandler.getAnalysisRequest(producerTemplate, headers);
                        log.info("Fetched analysisRequest {}", savedAnalysisRequest);
                        if (savedAnalysisRequest == null
                                || savedAnalysisRequest.getContact() == null
                                || savedAnalysisRequest.getContact().isEmpty()) {
                            headers.put(
                                    Constants.HEADER_DESCRIPTION,
                                    serviceRequest.getCode().getCoding().get(0).getCode());
                            AnalysisRequestTemplate analysisRequestTemplate =
                                    analysisRequestTemplateHandler.getAnalysisRequestTemplate(
                                            producerTemplate, headers);
                            log.info("Fetched analysisRequestTemplate {}", analysisRequestTemplate);
                            if (analysisRequestTemplate == null
                                    || analysisRequestTemplate.getAnalysisRequestTemplateItems() == null
                                    || analysisRequestTemplate
                                            .getAnalysisRequestTemplateItems()
                                            .isEmpty()) {
                                log.error(
                                        "No ARTemplate found for id {}",
                                        serviceRequest
                                                .getCode()
                                                .getCoding()
                                                .get(0)
                                                .getCode());
                                return;
                            }
                            AnalysisRequest analysisRequest = analysisRequestMapper.toSenaite(
                                    savedClient, savedContact, analysisRequestTemplate, serviceRequest);
                            log.info(
                                    "Mapped analysisRequest from savedClient, analysisRequestTemplate, serviceRequest {}",
                                    analysisRequest);
                            headers.put(Constants.HEADER_CLIENT_UID, savedClient.getUid());
                            savedAnalysisRequest = analysisRequestHandler.sendAnalysisRequest(
                                    producerTemplate, analysisRequest, headers);
                            log.info("Saved AnalysisRequest {}", savedAnalysisRequest);
                        }
                        headers.put(Constants.HEADER_SERVICE_REQUEST_ID, serviceRequestUuid);
                        Task savedTask = taskHandler.getTask(producerTemplate, headers);
                        log.info("Fetched task {}", savedTask);
                        if (savedTask == null
                                || savedTask.getDescription() == null
                                || savedTask.getDescription().isEmpty()) {
                            Task task = taskMapper.toFhir(savedAnalysisRequest);
                            log.info("Mapped task from savedAnalysisRequest {}", task);
                            task.setStatus(Task.TaskStatus.REQUESTED);
                            savedTask = taskHandler.sendTask(producerTemplate, task);
                            log.info("Saved task {}", savedTask);
                        }

                    } else {
                        // Executed when MODIFY option is selected in OpenMRS
                    }
                } else if ("d".equals(eventType)) {
                    // Executed when DISCONTINUE option is selected in OpenMRS

                } else {
                    throw new IllegalArgumentException("Unsupported event type: " + eventType);
                }
            }
        } catch (Exception e) {
            throw new CamelExecutionException("Error processing ServiceRequest", exchange, e);
        }
    }
}
