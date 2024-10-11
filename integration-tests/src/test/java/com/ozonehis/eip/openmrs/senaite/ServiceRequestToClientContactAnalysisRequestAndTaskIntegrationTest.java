/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openmrs.senaite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.openmrs.eip.fhir.Constants.HEADER_FHIR_EVENT_TYPE;

import ca.uhn.fhir.context.FhirContext;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ozonehis.eip.openmrs.senaite.model.SenaiteResponseWrapper;
import com.ozonehis.eip.openmrs.senaite.model.analysisRequest.AnalysisRequestDTO;
import com.ozonehis.eip.openmrs.senaite.model.analysisRequest.AnalysisRequestMapper;
import com.ozonehis.eip.openmrs.senaite.model.analysisRequest.response.AnalysisRequestItem;
import com.ozonehis.eip.openmrs.senaite.model.client.ClientDTO;
import com.ozonehis.eip.openmrs.senaite.model.client.ClientMapper;
import com.ozonehis.eip.openmrs.senaite.model.client.response.ClientItem;
import com.ozonehis.eip.openmrs.senaite.model.contact.ContactDTO;
import com.ozonehis.eip.openmrs.senaite.model.contact.ContactMapper;
import com.ozonehis.eip.openmrs.senaite.model.contact.response.ContactItem;
import com.ozonehis.eip.openmrs.senaite.routes.analyses.GetAnalysesRoute;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.camel.CamelContext;
import org.apache.camel.test.infra.core.annotations.RouteFixture;
import org.apache.commons.codec.binary.Base64;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.Task;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class ServiceRequestToClientContactAnalysisRequestAndTaskIntegrationTest extends BaseRouteIntegrationTest {

    private static final String GET_ANALYSIS_REQUEST_BY_PATIENT_ID =
            "http://localhost:8081/senaite/@@API/senaite/v1/AnalysisRequest?catalog=senaite_catalog_sample&complete=true&getClientID=%s";

    private static final String GET_CLIENT_BY_PATIENT_ID =
            "http://localhost:8081/senaite/@@API/senaite/v1/client?getClientID=%s";

    private static final String GET_CONTACT_BY_FIRSTNAME =
            "http://localhost:8081/senaite/@@API/senaite/v1/Contact?firstname=%s";

    private static final String GET_TASK_BY_SERVICE_REQUEST =
            "http://localhost/openmrs/ws/fhir2/R4/Task?based-on:ServiceRequest=%s";

    private static final String PATIENT_ID = "487b174a-8f7a-475f-899b-752cf419edba";

    private static final String SERVICE_REQUEST_ID = "5a92fd97-89ed-48df-aea5-907f42810709";

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static final OkHttpClient client = new OkHttpClient();

    private Bundle serviceRequestBundle;

    @BeforeEach
    public void initializeData() {
        serviceRequestBundle = loadResource("fhir.bundle/service-request-bundle.json", new Bundle());
    }

    @RouteFixture
    public void createRouteBuilder(CamelContext context) throws Exception {
        context = getContextWithRouting(context);

        context.addRoutes(new GetAnalysesRoute(getSenaiteConfig()));
    }

    @Test
    @DisplayName("Should verify routes.")
    public void shouldVerifySaleOrderRoutes() {
        assertTrue(hasRoute(contextExtension.getContext(), "service-request-to-analysis-request-router"));
        assertTrue(hasRoute(contextExtension.getContext(), "service-request-to-analysis-request-processor"));
        assertTrue(hasRoute(contextExtension.getContext(), "senaite-get-client-route"));
        assertTrue(hasRoute(contextExtension.getContext(), "senaite-create-client-route"));
        assertTrue(hasRoute(contextExtension.getContext(), "senaite-get-contact-route"));
        assertTrue(hasRoute(contextExtension.getContext(), "senaite-create-contact-route"));
        assertTrue(hasRoute(contextExtension.getContext(), "senaite-get-analysis-request-route"));
        assertTrue(hasRoute(contextExtension.getContext(), "senaite-get-analysis-request-template-route"));
        assertTrue(hasRoute(contextExtension.getContext(), "senaite-create-analysis-request-route"));
        assertTrue(hasRoute(contextExtension.getContext(), "senaite-get-analysis-request-by-client-sample-id-route"));
        assertTrue(hasRoute(contextExtension.getContext(), "senaite-update-analysis-request-route"));
    }

    @Test
    @DisplayName("Should create AnalysisRequest in Senaite given ServiceRequest Bundle.")
    public void shouldCreateAnalysisRequestInSenaiteGivenServiceRequest() throws IOException {
        // Act
        Map<String, Object> headers = new HashMap<>();
        headers.put(HEADER_FHIR_EVENT_TYPE, "c");
        sendBodyAndHeaders("direct:service-request-to-analysis-request-processor", serviceRequestBundle, headers);

        // Verify
        // AnalysisRequest should be created
        String response = fetchFromSenaite(String.format(GET_ANALYSIS_REQUEST_BY_PATIENT_ID, PATIENT_ID));

        TypeReference<SenaiteResponseWrapper<AnalysisRequestItem>> analysisRequestItemTypeReference =
                new TypeReference<>() {};
        SenaiteResponseWrapper<AnalysisRequestItem> analysisRequestItemResponseWrapper =
                objectMapper.readValue(response, analysisRequestItemTypeReference);
        AnalysisRequestDTO analysisRequestDTO = AnalysisRequestMapper.map(analysisRequestItemResponseWrapper);

        assertNotNull(analysisRequestDTO);
        assertNotNull(analysisRequestDTO.getUid());

        // Client should be created
        response = fetchFromSenaite(String.format(GET_CLIENT_BY_PATIENT_ID, PATIENT_ID));
        ObjectMapper objectMapper = new ObjectMapper();
        TypeReference<SenaiteResponseWrapper<ClientItem>> clientItemTypeReference = new TypeReference<>() {};
        SenaiteResponseWrapper<ClientItem> clientItemResponseWrapper =
                objectMapper.readValue(response, clientItemTypeReference);
        ClientDTO clientDTO = ClientMapper.map(clientItemResponseWrapper);

        assertNotNull(clientDTO);
        assertEquals(PATIENT_ID, clientDTO.getClientID());

        // Contact should be created
        response = fetchFromSenaite(String.format(GET_CONTACT_BY_FIRSTNAME, "Super"));
        TypeReference<SenaiteResponseWrapper<ContactItem>> contactItemTypeReference = new TypeReference<>() {};
        SenaiteResponseWrapper<ContactItem> contactItemResponseWrapper =
                objectMapper.readValue(response, contactItemTypeReference);
        ContactDTO contactDTO = ContactMapper.map(contactItemResponseWrapper);

        assertNotNull(contactDTO);
        assertTrue(contactDTO.getTitle().contains("Super User"));

        // Task should be created in OpenMRS
        response = fetchFromOpenMRS(String.format(GET_TASK_BY_SERVICE_REQUEST, SERVICE_REQUEST_ID));
        Bundle bundle = FhirContext.forR4().newJsonParser().parseResource(Bundle.class, response);
        Task task = null;
        List<Bundle.BundleEntryComponent> entries = bundle.getEntry();
        for (Bundle.BundleEntryComponent entry : entries) {
            Resource resource = entry.getResource();
            if (resource instanceof Task) {
                task = (Task) resource;
                break;
            }
        }

        assertNotNull(task);
        assertEquals(SERVICE_REQUEST_ID, task.getBasedOn().get(0).getReference());
        assertEquals("REQUESTED", task.getStatus().toString());
    }

    private String fetchFromSenaite(String url) {
        Request request = new Request.Builder()
                .url(url)
                .header("Authorization", getSenaiteConfig().authHeader())
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            } else {
                return response.body().string();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String fetchFromOpenMRS(String url) {
        String auth = "admin:Admin123";
        byte[] encodedAuth = Base64.encodeBase64(auth.getBytes());

        Request request = new Request.Builder()
                .url(url)
                .header("Authorization", "Basic " + new String(encodedAuth))
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            } else {
                return response.body().string();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
