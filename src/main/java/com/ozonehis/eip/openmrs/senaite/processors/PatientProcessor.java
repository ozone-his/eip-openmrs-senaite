/*
 * Copyright © 2024, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openmrs.senaite.processors;

import static org.openmrs.eip.fhir.Constants.HEADER_FHIR_EVENT_TYPE;

import com.ozonehis.eip.openmrs.senaite.handlers.senaite.ClientHandler;
import com.ozonehis.eip.openmrs.senaite.mapper.senaite.ClientMapper;
import com.ozonehis.eip.openmrs.senaite.model.Client;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.CamelExecutionException;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.ProducerTemplate;
import org.hl7.fhir.r4.model.Patient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Setter
@Getter
@Component
public class PatientProcessor implements Processor {

    @Autowired
    private ClientHandler clientHandler;

    @Autowired
    private ClientMapper clientMapper;

    @Override
    public void process(Exchange exchange) {
        try (ProducerTemplate producerTemplate = exchange.getContext().createProducerTemplate()) {
            Message message = exchange.getMessage();
            Patient patient = message.getBody(Patient.class);

            if (patient == null) {
                return;
            }
            log.info("PatientProcessor: Patient {}", patient);

            Map<String, Object> headers = new HashMap<>();
            Client savedClient = clientHandler.getClient(producerTemplate, "");
            log.info("PatientProcessor: savedClient {}", savedClient);
            Client client = clientMapper.toSenaite(patient);
            log.info("PatientProcessor: client {}", client);
            if (savedClient != null && !savedClient.getClientID().isEmpty()) {
                headers.put(HEADER_FHIR_EVENT_TYPE, "u");
            } else {
                headers.put(HEADER_FHIR_EVENT_TYPE, "c");
            }
            exchange.getMessage().setHeaders(headers);
            exchange.getMessage().setBody(client);

        } catch (Exception e) {
            throw new CamelExecutionException("Error processing Patient", exchange, e);
        }
    }
}
