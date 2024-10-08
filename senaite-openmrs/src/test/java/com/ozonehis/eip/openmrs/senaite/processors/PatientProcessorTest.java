package com.ozonehis.eip.openmrs.senaite.processors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.ozonehis.eip.openmrs.senaite.handlers.senaite.ClientHandler;
import com.ozonehis.eip.openmrs.senaite.mapper.senaite.ClientMapper;
import com.ozonehis.eip.openmrs.senaite.model.client.ClientDTO;
import com.ozonehis.eip.openmrs.senaite.model.client.request.Client;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.hl7.fhir.r4.model.Address;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collections;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static org.openmrs.eip.fhir.Constants.HEADER_FHIR_EVENT_TYPE;

class PatientProcessorTest extends BaseProcessorTest {

    private static final String ADDRESS_ID = "12377e18-a051-487b-8dd3-4cffcddb2a9c";

    private static final String PATIENT_ID = "866f25bf-d930-4886-9332-75443047e38e";

    @Mock
    private ClientHandler clientHandler;

    @Mock
    private ClientMapper clientMapper;

    @InjectMocks
    private PatientProcessor patientProcessor;

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
    void shouldProcessPatientWithUpdateEventType() throws JsonProcessingException {
        // Setup
        Patient patient = new Patient();
        patient.setId(PATIENT_ID);
        Address address = new Address();
        address.setId(ADDRESS_ID);
        address.setUse(Address.AddressUse.HOME);
        patient.setAddress(Collections.singletonList(address));

        Exchange exchange = createExchange(patient, "c");

        ClientDTO clientDTO = new ClientDTO();
        clientDTO.setUid(UUID.randomUUID().toString());
        clientDTO.setTitle("John Doe");

        Client client = new Client();
        client.setTitle("John Doe");

        when(clientHandler.getClientByPatientID(any(), eq(PATIENT_ID))).thenReturn(clientDTO);
        when(clientMapper.toSenaite(patient)).thenReturn(client);

        // Act
        patientProcessor.process(exchange);

        // Assert
        assertEquals(exchange.getMessage().getHeader(HEADER_FHIR_EVENT_TYPE), "u");
        verify(clientMapper, times(1)).toSenaite(patient);
    }

    @Test
    void shouldProcessPatientWithCreateEventType() throws JsonProcessingException {
        // Setup
        Patient patient = new Patient();
        patient.setId(PATIENT_ID);
        Address address = new Address();
        address.setId(ADDRESS_ID);
        address.setUse(Address.AddressUse.HOME);
        patient.setAddress(Collections.singletonList(address));

        Exchange exchange = createExchange(patient, "c");


        Client client = new Client();
        client.setTitle("John Doe");

        when(clientHandler.getClientByPatientID(any(), eq(PATIENT_ID))).thenReturn(null);
        when(clientMapper.toSenaite(patient)).thenReturn(client);

        // Act
        patientProcessor.process(exchange);

        // Assert
        assertEquals(exchange.getMessage().getHeader(HEADER_FHIR_EVENT_TYPE), "c");
        verify(clientMapper, times(1)).toSenaite(patient);
    }
}