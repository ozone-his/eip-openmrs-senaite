/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openmrs.senaite.handlers.bahmni;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ozonehis.eip.openmrs.senaite.handlers.openmrs.ObservationHandler;
import com.ozonehis.eip.openmrs.senaite.model.analyses.AnalysesDTO;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.ProducerTemplate;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Setter
@Component
public class BahmniResultsHandler {

    private static final String UUID_REGEX =
            "^[0-9a-fA-F]{36}$|^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$";

    private static final Pattern UUID_PATTERN = Pattern.compile(UUID_REGEX);

    @Value("${openmrs.baseUrl}")
    protected String openmrsBaseUrl;

    @Value("${openmrs.username}")
    protected String openmrsUsername;

    @Value("${openmrs.password}")
    protected String openmrsPassword;

    @Autowired
    private IGenericClient openmrsFhirClient;

    @Autowired
    private ObservationHandler observationHandler;

    public Observation buildAndSendBahmniResultObservation(
            ProducerTemplate producerTemplate,
            Encounter savedResultEncounter,
            ServiceRequest serviceRequest,
            ArrayList<AnalysesDTO> analysesDTOs,
            String datePublished) {

        String panelConceptUuid = serviceRequest.getIdPart();

        // Create the top-level map
        Map<String, Object> resultMap = new HashMap<>();

        resultMap.put("encounter", savedResultEncounter.getIdPart());
        resultMap.put("concept", getServiceRequestCodingIdentifier(serviceRequest));
        resultMap.put("order", panelConceptUuid);
        resultMap.put("person", savedResultEncounter.getSubject().getReference().substring("Patient/".length()));
        resultMap.put("obsDatetime", datePublished);

        // Create the groupMembers list for the first level
        List<Map<String, Object>> groupMembersLevel1 = new ArrayList<>();

        for (AnalysesDTO resultAnalysesDTO : analysesDTOs) {

            String analysesDescription = resultAnalysesDTO.getDescription();

            String testConceptUuid = analysesDescription.substring(
                    analysesDescription.lastIndexOf("(") + 1, analysesDescription.lastIndexOf(")"));
            String analysesResult = resultAnalysesDTO.getResult();
            String analysesResultCaptureDate = resultAnalysesDTO.getResultCaptureDate();

            // Create a nested map for the first group member
            Map<String, Object> groupMember1 = new HashMap<>();
            groupMember1.put("concept", testConceptUuid);
            groupMember1.put("order", serviceRequest.getIdPart());
            groupMember1.put(
                    "person", savedResultEncounter.getSubject().getReference().substring("Patient/".length()));
            groupMember1.put("obsDatetime", analysesResultCaptureDate);

            // Create the groupMembers list for the second level (nested inside groupMember1)
            List<Map<String, Object>> groupMembersLevel2 = new ArrayList<>();

            // Create a nested map for the second group member
            Map<String, Object> groupMember2 = new HashMap<>();
            groupMember2.put("value", analysesResult);
            groupMember2.put("order", serviceRequest.getIdPart());
            groupMember2.put(
                    "person", savedResultEncounter.getSubject().getReference().substring("Patient/".length()));
            groupMember2.put("obsDatetime", analysesResultCaptureDate);
            groupMember2.put("concept", testConceptUuid);

            groupMembersLevel2.add(groupMember2);

            groupMember1.put("groupMembers", groupMembersLevel2);

            groupMembersLevel1.add(groupMember1);
        }

        resultMap.put("groupMembers", groupMembersLevel1);

        String jsonString = null;
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            jsonString = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(resultMap);
        } catch (Exception e) {
            throw new RuntimeException("Could not generate Bahmni ENR results payload : ", e);
        }

        String payload = jsonString;

        Map<String, Object> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Authorization", "Basic " + encodeBasicAuth(openmrsUsername, openmrsPassword));

        String obsEndpointUrl = openmrsBaseUrl + "/ws/rest/v1/obs";
        headers.put("obsEndpointUrl", obsEndpointUrl);

        String response = producerTemplate.requestBodyAndHeaders(
                "direct:create-bahmni-lab-results-route", payload, headers, String.class);

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

    public String getServiceRequestCodingIdentifier(ServiceRequest serviceRequest) {
        List<Coding> codings = serviceRequest.getCode().getCoding();
        for (Coding coding : codings) {
            String code = coding.getCode();

            // check if the code is a valid UUID
            if (isValidUUID(code)) {
                return code;
            }
            // check if it's a LOINC code
            if ("http://loinc.org".equals(coding.getSystem())) {
                return "LOINC:" + code;
            }
            // check if it's a CIEL code
            if ("https://cielterminology.org".equals(coding.getSystem())) {
                return "CIEL:" + code;
            }
            // check if it's a SNOMED code
            if ("http://snomed.info/sct/".equals(coding.getSystem())) {
                return "SNOMED CT:" + code;
            }
        }
        return null;
    }

    // Helper method to check if the code is a valid UUID
    private boolean isValidUUID(String code) {
        return UUID_PATTERN.matcher(code).matches();
    }
}
