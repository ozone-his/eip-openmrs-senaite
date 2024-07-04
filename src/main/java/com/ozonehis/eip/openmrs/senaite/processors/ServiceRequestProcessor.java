/*
 * Copyright © 2024, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openmrs.senaite.processors;

import com.ozonehis.eip.openmrs.openmrs.handlers.openmrs.TaskHandler;
import com.ozonehis.eip.openmrs.senaite.handlers.senaite.AnalysisRequestHandler;
import com.ozonehis.eip.openmrs.senaite.handlers.senaite.AnalysisRequestTemplateHandler;
import com.ozonehis.eip.openmrs.senaite.handlers.senaite.ClientHandler;
import com.ozonehis.eip.openmrs.senaite.handlers.senaite.ContactHandler;
import com.ozonehis.eip.openmrs.senaite.mapper.fhir.TaskMapper;
import com.ozonehis.eip.openmrs.senaite.mapper.senaite.AnalysisRequestMapper;
import com.ozonehis.eip.openmrs.senaite.mapper.senaite.ClientMapper;
import com.ozonehis.eip.openmrs.senaite.mapper.senaite.ContactMapper;
import com.ozonehis.eip.openmrs.senaite.model.AnalysisRequest;
import com.ozonehis.eip.openmrs.senaite.model.AnalysisRequestTemplate;
import com.ozonehis.eip.openmrs.senaite.model.Client;
import com.ozonehis.eip.openmrs.senaite.model.Contact;
import java.util.List;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.CamelExecutionException;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.ProducerTemplate;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.hl7.fhir.r4.model.Task;
import org.openmrs.eip.fhir.Constants;
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
            Practitioner practitioner = null;
            for (Bundle.BundleEntryComponent entry : entries) {
                Resource resource = entry.getResource();
                if (resource instanceof Patient) {
                    patient = (Patient) resource;
                } else if (resource instanceof Encounter) {
                    encounter = (Encounter) resource;
                } else if (resource instanceof ServiceRequest) {
                    serviceRequest = (ServiceRequest) resource;
                } else if (resource instanceof Practitioner) {
                    practitioner = (Practitioner) resource;
                }
            }

            if (patient == null || encounter == null || serviceRequest == null || practitioner == null) {
                throw new CamelExecutionException(
                        "Invalid Bundle. Bundle must contain Patient, Encounter, ServiceRequest and Practitioner",
                        exchange);
            } else {
                log.debug("Processing ServiceRequest for Patient with UUID {}", patient.getIdPart());
                String eventType = exchange.getMessage().getHeader(Constants.HEADER_FHIR_EVENT_TYPE, String.class);
                if (eventType == null) {
                    throw new IllegalArgumentException("Event type not found in the exchange headers.");
                }
                String serviceRequestUuid = serviceRequest.getIdPart(); // TODO: check it should be {body.identifier}
                if ("c".equals(eventType) || "u".equals(eventType)) {
                    if (serviceRequest.getStatus().equals(ServiceRequest.ServiceRequestStatus.ACTIVE)
                            && serviceRequest.getIntent().equals(ServiceRequest.ServiceRequestIntent.ORDER)) {
                        // If revision/renewed order, first cancel their previous one on SENAITE
                        Client client = clientMapper.toSenaite(patient);

                        Client savedClient = clientHandler.getClient(producerTemplate, "");

                        if (savedClient == null) {
                            savedClient = clientHandler.sendClient(producerTemplate, client);
                        }
                        Contact savedContact = contactHandler.getContact(producerTemplate, "");
                        if (savedContact == null) {
                            Contact contact = contactMapper.toSenaite(savedClient);
                            savedContact = contactHandler.sendContact(producerTemplate, contact);
                        }
                        AnalysisRequest savedAnalysisRequest =
                                analysisRequestHandler.getAnalysisRequest(producerTemplate, "");
                        if (savedAnalysisRequest == null) {
                            AnalysisRequestTemplate analysisRequestTemplate =
                                    analysisRequestTemplateHandler.getAnalysisRequestTemplate(producerTemplate, "");
                            AnalysisRequest analysisRequest = analysisRequestMapper.toSenaite(
                                    savedClient, analysisRequestTemplate, serviceRequest);
                            savedAnalysisRequest =
                                    analysisRequestHandler.sendAnalysisRequest(producerTemplate, analysisRequest);
                        }
                        Task savedTask = taskHandler.getTask(producerTemplate, "");
                        if (savedTask == null) {
                            Task task = taskMapper.toFhir(savedAnalysisRequest);
                            task.setStatus(Task.TaskStatus.REQUESTED);
                            savedTask = taskHandler.sendTask(producerTemplate, task);
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
