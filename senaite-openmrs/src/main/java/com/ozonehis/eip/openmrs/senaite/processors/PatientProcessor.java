/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openmrs.senaite.processors;

import static org.openmrs.eip.fhir.Constants.HEADER_FHIR_EVENT_TYPE;

import com.ozonehis.eip.openmrs.senaite.handlers.senaite.ClientHandler;
import com.ozonehis.eip.openmrs.senaite.mapper.senaite.ClientMapper;
import com.ozonehis.eip.openmrs.senaite.model.client.ClientDTO;
import com.ozonehis.eip.openmrs.senaite.model.client.request.Client;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.ProducerTemplate;
import org.hl7.fhir.r4.model.Patient;
import org.openmrs.eip.EIPException;
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

            Map<String, Object> headers = new HashMap<>();
            ClientDTO savedClientDTO = clientHandler.getClientByPatientID(producerTemplate, patient.getIdPart());
            Client client = clientMapper.toSenaite(patient);
            if (savedClientDTO != null && !savedClientDTO.getUid().isEmpty()) {
                savedClientDTO.setTitle(client.getTitle());
                headers.put(HEADER_FHIR_EVENT_TYPE, "u");
                exchange.getMessage().setBody(savedClientDTO);
            } else {
                headers.put(HEADER_FHIR_EVENT_TYPE, "c");
                exchange.getMessage().setBody(client);
            }
            exchange.getMessage().setHeaders(headers);

        } catch (Exception e) {
            throw new EIPException(String.format("Error processing Patient %s", e.getMessage()));
        }
    }
}
