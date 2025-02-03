/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openmrs.senaite.handlers.bahmni;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.ICriterion;
import ca.uhn.fhir.rest.gclient.IQuery;
import ca.uhn.fhir.rest.gclient.IUntypedQuery;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ozonehis.eip.openmrs.senaite.handlers.openmrs.ObservationHandler;
import com.ozonehis.eip.openmrs.senaite.model.analyses.AnalysesDTO;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.camel.ProducerTemplate;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.r4.model.*;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
public class BahmniResultsHandlerTest {

    private ObjectMapper objectMapper = new ObjectMapper();

    private ServiceRequest serviceRequest;

    private Encounter savedResultEncounter;

    @MockBean
    private ProducerTemplate producerTemplate;

    @MockBean
    private IGenericClient openmrsFhirClient;

    @MockBean
    private ObservationHandler observationHandler;

    @Mock
    private Observation observation;

    @Mock
    private AnalysesDTO analysesDTO1, analysesDTO2;

    @Mock
    private IUntypedQuery<IBaseBundle> iUntypedQuery;

    @Mock
    private IQuery iQuery;

    @Mock
    private Bundle bundle;

    @InjectMocks
    private BahmniResultsHandler bahmniResultsHandler;

    @Value("${openmrs.baseUrl}")
    private String openmrsBaseUrl;

    @Value("${openmrs.username}")
    private String openmrsUsername;

    @Value("${openmrs.password}")
    private String openmrsPassword;

    @Before
    public void setUp() throws Exception {
        // Injecting the mock values into the class under test
        bahmniResultsHandler.openmrsBaseUrl = "http://example.com";
        bahmniResultsHandler.openmrsUsername = "username";
        bahmniResultsHandler.openmrsPassword = "password";

        // Initialize mock objects and set default behaviors
        MockitoAnnotations.initMocks(this);

        // Create Encounter object with reference to Patient
        savedResultEncounter = new Encounter();
        savedResultEncounter.setId("cb4f94cf-462c-4e0d-8c48-3dc4c7c0201b");
        savedResultEncounter.setStatus(Encounter.EncounterStatus.INPROGRESS);
        savedResultEncounter
                .getMeta()
                .addTag()
                .setSystem("http://fhir.openmrs.org/ext/encounter-tag")
                .setCode("encounter")
                .setDisplay("Encounter");
        savedResultEncounter.setSubject(
                new org.hl7.fhir.r4.model.Reference("Patient/5946f880-b197-400b-9caa-a3c661d23041"));

        // Create ServiceRequest object with reference to Patient
        serviceRequest = new ServiceRequest();
        serviceRequest.setId("05e271aa-5f11-4378-a443-3e7e3e8c7a71");
        serviceRequest.setStatus(ServiceRequest.ServiceRequestStatus.COMPLETED);
        serviceRequest.setIntent(ServiceRequest.ServiceRequestIntent.ORDER);
        serviceRequest
                .getCode()
                .addCoding()
                .setCode("1019AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA")
                .setDisplay("CBC");
        serviceRequest.setSubject(new org.hl7.fhir.r4.model.Reference("Patient/5946f880-b197-400b-9caa-a3c661d23041"));

        when(analysesDTO1.getDescription()).thenReturn("Test1(1016AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA)");
        when(analysesDTO1.getResult()).thenReturn("1");
        when(analysesDTO1.getResultCaptureDate()).thenReturn("2022-01-01");

        when(analysesDTO2.getDescription()).thenReturn("Test2(679AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA)");
        when(analysesDTO2.getResult()).thenReturn("2");
        when(analysesDTO2.getResultCaptureDate()).thenReturn("2022-01-02");

        when(producerTemplate.requestBodyAndHeaders(anyString(), anyString(), anyMap(), eq(String.class)))
                .thenReturn("{\"uuid\":\"0c4f44f7-7277-4a0a-9f6f-b842c874c95b\"}");

        when(openmrsFhirClient.search()).thenReturn(iUntypedQuery);
        when(iUntypedQuery.forResource(Observation.class)).thenReturn(iQuery);
        when(iQuery.where(any(ICriterion.class))).thenReturn(iQuery);
        when(iQuery.returnBundle(Bundle.class)).thenReturn(iQuery);

        observation = new Observation();
        observation.setId("0c4f44f7-7277-4a0a-9f6f-b842c874c95b");
        observation.setStatus(Observation.ObservationStatus.FINAL);

        // Set the code for the Observation (CBC)
        CodeableConcept code = new CodeableConcept();
        code.addCoding().setCode("1019AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
        observation.setValue(code);

        Quantity valueQuantity = new Quantity();
        valueQuantity
                .setValue(166.0)
                .setUnit("cm")
                .setSystem("http://unitsofmeasure.org")
                .setCode("cm");
        observation.setValue(valueQuantity);

        Reference subject = new Reference("Patient/5946f880-b197-400b-9caa-a3c661d23041");
        observation.setSubject(subject);
        observation.getSubject().setDisplay("Collet Chebaskwony");

        observation.setEffective(new DateTimeType("2025-01-27T13:30:00+00:00"));

        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.SEARCHSET);
        BundleEntryComponent entry = bundle.addEntry();
        entry.setResource(observation);

        when(iQuery.execute()).thenReturn(bundle);
    }

    @Test
    public void testBuildAndSendBahmniResultObservation() throws Exception {
        // setup
        List<AnalysesDTO> analysesDTOs = Arrays.asList(analysesDTO1, analysesDTO2);
        String datePublished = "2025-01-27T13:30:00+00:00";

        // replay
        Observation result = bahmniResultsHandler.buildAndSendBahmniResultObservation(
                producerTemplate, savedResultEncounter, serviceRequest, new ArrayList<>(analysesDTOs), datePublished);

        // replay
        assertNotNull(result);
        String bahmniResultObs = "{\r\n" + "  \"groupMembers\" : [ {\r\n"
                + "    \"groupMembers\" : [ {\r\n"
                + "      \"obsDatetime\" : \"2022-01-01\",\r\n"
                + "      \"person\" : \"5946f880-b197-400b-9caa-a3c661d23041\",\r\n"
                + "      \"concept\" : \"1016AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\",\r\n"
                + "      \"value\" : \"1\",\r\n"
                + "      \"order\" : \"05e271aa-5f11-4378-a443-3e7e3e8c7a71\"\r\n"
                + "    } ],\r\n"
                + "    \"obsDatetime\" : \"2022-01-01\",\r\n"
                + "    \"person\" : \"5946f880-b197-400b-9caa-a3c661d23041\",\r\n"
                + "    \"concept\" : \"1016AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\",\r\n"
                + "    \"order\" : \"05e271aa-5f11-4378-a443-3e7e3e8c7a71\"\r\n"
                + "  }, {\r\n"
                + "    \"groupMembers\" : [ {\r\n"
                + "      \"obsDatetime\" : \"2022-01-02\",\r\n"
                + "      \"person\" : \"5946f880-b197-400b-9caa-a3c661d23041\",\r\n"
                + "      \"concept\" : \"679AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\",\r\n"
                + "      \"value\" : \"2\",\r\n"
                + "      \"order\" : \"05e271aa-5f11-4378-a443-3e7e3e8c7a71\"\r\n"
                + "    } ],\r\n"
                + "    \"obsDatetime\" : \"2022-01-02\",\r\n"
                + "    \"person\" : \"5946f880-b197-400b-9caa-a3c661d23041\",\r\n"
                + "    \"concept\" : \"679AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\",\r\n"
                + "    \"order\" : \"05e271aa-5f11-4378-a443-3e7e3e8c7a71\"\r\n"
                + "  } ],\r\n"
                + "  \"obsDatetime\" : \"2025-01-27T13:30:00+00:00\",\r\n"
                + "  \"person\" : \"5946f880-b197-400b-9caa-a3c661d23041\",\r\n"
                + "  \"concept\" : \"1019AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\",\r\n"
                + "  \"encounter\" : \"cb4f94cf-462c-4e0d-8c48-3dc4c7c0201b\",\r\n"
                + "  \"order\" : \"05e271aa-5f11-4378-a443-3e7e3e8c7a71\"\r\n"
                + "}";
        JsonNode expectedBahmniResultObs = objectMapper.readTree(bahmniResultObs);

        verify(producerTemplate)
                .requestBodyAndHeaders(
                        anyString(),
                        argThat(new ArgumentMatcher<String>() {
                            @Override
                            public boolean matches(String payload) {
                                try {
                                    JsonNode payloadNode =
                                            objectMapper.readTree(payload); // Parse the payload into JsonNode
                                    return expectedBahmniResultObs.equals(payloadNode); // Compare the structure
                                } catch (Exception e) {
                                    return false;
                                }
                            }
                        }),
                        anyMap(),
                        eq(String.class));
        assertEquals(observation, result);
    }

    @Test(expected = RuntimeException.class)
    public void testBuildAndSendBahmniResultObservationExceptionHandling() throws Exception {
        // setup
        when(producerTemplate.requestBodyAndHeaders(anyString(), anyString(), anyMap(), eq(String.class)))
                .thenThrow(new RuntimeException("Failed to send request"));

        List<AnalysesDTO> analysesDTOs = Arrays.asList(analysesDTO1, analysesDTO2);
        String datePublished = "2025-01-27T13:30:00+00:00";

        // replay
        bahmniResultsHandler.buildAndSendBahmniResultObservation(
                producerTemplate, savedResultEncounter, serviceRequest, new ArrayList<>(analysesDTOs), datePublished);
    }
}
