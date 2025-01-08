/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openmrs.senaite.handlers.bahmni;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.server.exceptions.ResourceGoneException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import lombok.AllArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.apache.camel.ProducerTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Setter
@AllArgsConstructor
@Component
public class BahmniResultsHandler {

	@Value("${openmrs.baseUrl}")
    private String openmrsBaseUrl;
	
	@Value("${openmrs.username}")
    private String openmrsUsername;

    @Value("${openmrs.password}")
    private String openmrsPassword;
    
    @Autowired
    private IGenericClient openmrsFhirClient;

    public Observation  buildAndSendBahmniResultObservation(
    		ProducerTemplate producerTemplate,
            Encounter savedResultEncounter,
            String conceptUuid,
            String analysesResult,
            String analysesResultCaptureDate) {
        
    	// Create the top-level map
        Map<String, Object> resultMap = new HashMap<>();

        resultMap.put("encounter", savedResultEncounter.getIdPart());
        resultMap.put("concept", conceptUuid);
        resultMap.put("order", "");
        resultMap.put("person", savedResultEncounter.getSubject().getReference().substring("Patient/".length()));
        resultMap.put("obsDatetime", analysesResultCaptureDate);

        // Create the groupMembers list for the first level
        List<Map<String, Object>> groupMembersLevel1 = new ArrayList<>();

        // Create a nested map for the first group member
        Map<String, Object> groupMember1 = new HashMap<>();
        groupMember1.put("concept", conceptUuid);
        groupMember1.put("order", "");
        groupMember1.put("person", savedResultEncounter.getSubject().getReference().substring("Patient/".length()));
        groupMember1.put("obsDatetime", analysesResultCaptureDate);

        // Create the groupMembers list for the second level (nested inside groupMember1)
        List<Map<String, Object>> groupMembersLevel2 = new ArrayList<>();

        // Create a nested map for the second group member
        Map<String, Object> groupMember2 = new HashMap<>();
        groupMember2.put("value", analysesResult);
        groupMember2.put("order", "");
        groupMember2.put("person", savedResultEncounter.getSubject().getReference().substring("Patient/".length()));
        groupMember2.put("obsDatetime", analysesResultCaptureDate);
        groupMember2.put("concept", "conceptUuid");

        groupMembersLevel2.add(groupMember2);

        groupMember1.put("groupMembers", groupMembersLevel2);

        groupMembersLevel1.add(groupMember1);

        resultMap.put("groupMembers", groupMembersLevel1);

        String jsonString = null;
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            String jsonString = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(resultMap);
        } catch (Exception e) {
        	throw new RuntimeException("Could not generate Bahmni ENR results payload : ", e);
        }
        
        String payload = jsonString;

        Map<String, Object> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Authorization", "Basic " + encodeBasicAuth(openmrsUsername, openmrsPassword));

        String obsEndpointUrl = openmrsBaseUrl + "/ws/rest/v1/obs";

        String response = producerTemplate.requestBodyAndHeaders(
                "direct:" + obsEndpointUrl, payload, headers, String.class);
        
        String observationUuid = null;
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(response);
            observationUuid = rootNode.get("uuid").asText();
        } catch (Exception e) {
        	throw new RuntimeException("Could not extract Bahmni EMR results observation uuid : ", e);
        }
        
        Bundle bundle = openmrsFhirClient
                .search()
                .forResource(Observation.class)
                .where(Observation.RES_ID.exactly().identifier(observationUuid))
                .returnBundle(Bundle.class)
                .execute();

        return bundle.getEntry().stream()
                .map(Bundle.BundleEntryComponent::getResource)
                .filter(Observation.class::isInstance)
                .map(Observation.class::cast)
                .findFirst()
                .orElse(null);
    }
    
    private String encodeBasicAuth(String username, String password) {
        String credentials = username + ":" + password;
        return new String(java.util.Base64.getEncoder().encode(credentials.getBytes()));
    }
}
