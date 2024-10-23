/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openmrs.senaite;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.BasicAuthInterceptor;
import com.ozonehis.camel.test.infra.senaite.services.SenaiteService;
import com.ozonehis.camel.test.infra.senaite.services.SenaiteServiceFactory;
import com.ozonehis.eip.openmrs.senaite.config.SenaiteConfig;
import com.ozonehis.eip.openmrs.senaite.converters.ResourceConverter;
import com.ozonehis.eip.openmrs.senaite.handlers.openmrs.DiagnosticReportHandler;
import com.ozonehis.eip.openmrs.senaite.handlers.openmrs.EncounterHandler;
import com.ozonehis.eip.openmrs.senaite.handlers.openmrs.ObservationHandler;
import com.ozonehis.eip.openmrs.senaite.handlers.openmrs.ServiceRequestHandler;
import com.ozonehis.eip.openmrs.senaite.handlers.openmrs.TaskHandler;
import com.ozonehis.eip.openmrs.senaite.handlers.senaite.AnalysesHandler;
import com.ozonehis.eip.openmrs.senaite.handlers.senaite.AnalysisRequestHandler;
import com.ozonehis.eip.openmrs.senaite.handlers.senaite.AnalysisRequestTemplateHandler;
import com.ozonehis.eip.openmrs.senaite.handlers.senaite.ClientHandler;
import com.ozonehis.eip.openmrs.senaite.handlers.senaite.ContactHandler;
import com.ozonehis.eip.openmrs.senaite.mapper.fhir.TaskMapper;
import com.ozonehis.eip.openmrs.senaite.mapper.senaite.AnalysisRequestMapper;
import com.ozonehis.eip.openmrs.senaite.mapper.senaite.ClientMapper;
import com.ozonehis.eip.openmrs.senaite.mapper.senaite.ContactMapper;
import com.ozonehis.eip.openmrs.senaite.processors.PatientProcessor;
import com.ozonehis.eip.openmrs.senaite.processors.ServiceRequestProcessor;
import com.ozonehis.eip.openmrs.senaite.processors.TaskProcessor;
import com.ozonehis.eip.openmrs.senaite.routes.PatientRouting;
import com.ozonehis.eip.openmrs.senaite.routes.ServiceRequestRouting;
import com.ozonehis.eip.openmrs.senaite.routes.TaskRouting;
import com.ozonehis.eip.openmrs.senaite.routes.analyses.GetAnalysesRoute;
import com.ozonehis.eip.openmrs.senaite.routes.analysisRequestTemplate.GetAnalysisRequestTemplateRoute;
import com.ozonehis.eip.openmrs.senaite.routes.analysisrequest.CreateAnalysisRequestRoute;
import com.ozonehis.eip.openmrs.senaite.routes.analysisrequest.GetAnalysisRequestByClientSampleIDRoute;
import com.ozonehis.eip.openmrs.senaite.routes.analysisrequest.GetAnalysisRequestRoute;
import com.ozonehis.eip.openmrs.senaite.routes.analysisrequest.UpdateAnalysisRequestRoute;
import com.ozonehis.eip.openmrs.senaite.routes.client.CreateClientRoute;
import com.ozonehis.eip.openmrs.senaite.routes.client.GetClientRoute;
import com.ozonehis.eip.openmrs.senaite.routes.client.UpdateClientRoute;
import com.ozonehis.eip.openmrs.senaite.routes.contact.CreateContactRoute;
import com.ozonehis.eip.openmrs.senaite.routes.contact.GetContactRoute;
import com.ozonehis.eip.openmrs.senaite.routes.openmrsFhirTask.GetOpenmrsFhirTaskByStatusRoute;
import jakarta.annotation.Nonnull;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.Getter;
import org.apache.camel.CamelContext;
import org.apache.camel.component.http.HttpComponent;
import org.apache.camel.test.infra.core.CamelContextExtension;
import org.apache.camel.test.infra.core.DefaultCamelContextExtension;
import org.apache.camel.test.infra.core.annotations.ContextFixture;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.hl7.fhir.r4.model.Resource;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@Getter
@ActiveProfiles("test")
@CamelSpringBootTest
@SpringBootTest(classes = {TestSpringConfiguration.class})
public abstract class BaseRouteIntegrationTest {

    private SenaiteConfig senaiteConfig;

    private static final String SENAITE_SERVER_URL = "http://localhost:8081/senaite";

    private static final String SENAITE_USERNAME = "admin";

    private static final String SENAITE_PASSWORD = "password";

    @RegisterExtension
    protected static CamelContextExtension contextExtension = new DefaultCamelContextExtension();

    @RegisterExtension
    protected static final SenaiteService service = SenaiteServiceFactory.createSingletonService();

    @ContextFixture
    public void configureContext(CamelContext context) {
        context.getComponent("http", HttpComponent.class).setHttpContext(new HttpClientContext());
    }

    protected static SenaiteConfig createSenaiteClient() {
        return new SenaiteConfig(SENAITE_USERNAME, SENAITE_PASSWORD, SENAITE_SERVER_URL);
    }

    public SenaiteConfig getSenaiteConfig() {
        if (senaiteConfig == null) {
            senaiteConfig = createSenaiteClient();
        }
        return senaiteConfig;
    }

    protected @Nonnull CamelContext getContextWithRouting(CamelContext context) throws Exception {
        FhirContext fhirContext = FhirContext.forR4();
        String serverBase = "http://localhost/openmrs/ws/fhir2/R4";
        IGenericClient client = fhirContext.newRestfulGenericClient(serverBase);

        String username = "admin";
        String password = "Admin123";
        BasicAuthInterceptor authInterceptor = new BasicAuthInterceptor(username, password);
        client.registerInterceptor(authInterceptor);

        ClientHandler clientHandler = new ClientHandler();
        ClientMapper clientMapper = new ClientMapper();
        ContactMapper contactMapper = new ContactMapper();
        AnalysisRequestMapper analysisRequestMapper = new AnalysisRequestMapper();
        AnalysisRequestHandler analysisRequestHandler = new AnalysisRequestHandler();
        ContactHandler contactHandler = new ContactHandler();
        AnalysisRequestTemplateHandler analysisRequestTemplateHandler = new AnalysisRequestTemplateHandler();
        AnalysesHandler analysesHandler = new AnalysesHandler();

        TaskMapper taskMapper = new TaskMapper();

        TaskHandler taskHandler = new TaskHandler(client);
        ServiceRequestHandler serviceRequestHandler = new ServiceRequestHandler(client);
        EncounterHandler encounterHandler = new EncounterHandler("7aa974b5-7523-11eb-8077-0242ac120009", client);
        ObservationHandler observationHandler = new ObservationHandler(client);
        DiagnosticReportHandler diagnosticReportHandler = new DiagnosticReportHandler(client);

        PatientProcessor patientProcessor = new PatientProcessor();
        patientProcessor.setClientHandler(clientHandler);
        patientProcessor.setClientMapper(clientMapper);

        ServiceRequestProcessor serviceRequestProcessor = new ServiceRequestProcessor();
        serviceRequestProcessor.setClientMapper(clientMapper);
        serviceRequestProcessor.setContactMapper(contactMapper);
        serviceRequestProcessor.setAnalysisRequestMapper(analysisRequestMapper);
        serviceRequestProcessor.setClientHandler(clientHandler);
        serviceRequestProcessor.setAnalysisRequestHandler(analysisRequestHandler);
        serviceRequestProcessor.setContactHandler(contactHandler);
        serviceRequestProcessor.setAnalysisRequestTemplateHandler(analysisRequestTemplateHandler);
        serviceRequestProcessor.setTaskHandler(taskHandler);
        serviceRequestProcessor.setTaskMapper(taskMapper);

        TaskProcessor taskProcessor = new TaskProcessor();
        taskProcessor.setServiceRequestHandler(serviceRequestHandler);
        taskProcessor.setTaskHandler(taskHandler);
        taskProcessor.setAnalysisRequestHandler(analysisRequestHandler);
        taskProcessor.setEncounterHandler(encounterHandler);
        taskProcessor.setAnalysesHandler(analysesHandler);
        taskProcessor.setObservationHandler(observationHandler);
        taskProcessor.setDiagnosticReportHandler(diagnosticReportHandler);

        ResourceConverter resourceConverter = new ResourceConverter();

        PatientRouting patientRouting = new PatientRouting();
        patientRouting.setPatientProcessor(patientProcessor);
        patientRouting.setResourceConverter(resourceConverter);

        ServiceRequestRouting serviceRequestRouting = new ServiceRequestRouting();
        serviceRequestRouting.setServiceRequestProcessor(serviceRequestProcessor);
        serviceRequestRouting.setResourceConverter(resourceConverter);

        TaskRouting taskRouting = new TaskRouting();
        taskRouting.setTaskProcessor(taskProcessor);
        taskRouting.setResourceConverter(resourceConverter);

        GetAnalysesRoute getAnalysesRoute = new GetAnalysesRoute(getSenaiteConfig());
        CreateAnalysisRequestRoute createAnalysisRequestRoute = new CreateAnalysisRequestRoute(getSenaiteConfig());
        GetAnalysisRequestByClientSampleIDRoute getAnalysisRequestByClientSampleIDRoute =
                new GetAnalysisRequestByClientSampleIDRoute(getSenaiteConfig());
        GetAnalysisRequestRoute getAnalysisRequestRoute = new GetAnalysisRequestRoute(getSenaiteConfig());
        UpdateAnalysisRequestRoute updateAnalysisRequestRoute = new UpdateAnalysisRequestRoute(getSenaiteConfig());
        GetAnalysisRequestTemplateRoute getAnalysisRequestTemplateRoute =
                new GetAnalysisRequestTemplateRoute(getSenaiteConfig());
        CreateClientRoute createClientRoute = new CreateClientRoute(getSenaiteConfig());
        GetClientRoute getClientRoute = new GetClientRoute(getSenaiteConfig());
        UpdateClientRoute updateClientRoute = new UpdateClientRoute(getSenaiteConfig());
        CreateContactRoute createContactRoute = new CreateContactRoute(getSenaiteConfig());
        GetContactRoute getContactRoute = new GetContactRoute(getSenaiteConfig());
        GetOpenmrsFhirTaskByStatusRoute getOpenmrsFhirTaskByStatusRoute = new GetOpenmrsFhirTaskByStatusRoute();

        context.addRoutes(patientRouting);
        context.addRoutes(serviceRequestRouting);
        context.addRoutes(taskRouting);
        context.addRoutes(getAnalysesRoute);
        context.addRoutes(createAnalysisRequestRoute);
        context.addRoutes(getAnalysisRequestByClientSampleIDRoute);
        context.addRoutes(getAnalysisRequestRoute);
        context.addRoutes(updateAnalysisRequestRoute);
        context.addRoutes(getAnalysisRequestTemplateRoute);
        context.addRoutes(createClientRoute);
        context.addRoutes(getClientRoute);
        context.addRoutes(updateClientRoute);
        context.addRoutes(createContactRoute);
        context.addRoutes(getContactRoute);
        context.addRoutes(getOpenmrsFhirTaskByStatusRoute);

        return context;
    }

    protected boolean hasRoute(CamelContext context, String routeId) {
        return context.getRoute(routeId) != null;
    }

    /**
     * Send a body and headers to an endpoint.
     *
     * @param endpoint the endpoint to send the body to.
     * @param body     the body to send.
     * @param headers  the headers to send.
     */
    protected void sendBodyAndHeaders(String endpoint, Object body, Map<String, Object> headers) {
        contextExtension
                .getProducerTemplate()
                .sendBodyAndHeaders(contextExtension.getContext().getEndpoint(endpoint), body, headers);
    }

    /**
     * Load resource from a file path.
     *
     * @param filePath the file path of the resource to load.
     * @param resource resource object
     * @param <T>      The type of the resource to load e.g., Patient, Encounter, etc.
     * @return resource object
     */
    @SuppressWarnings("unchecked")
    protected <T extends Resource> T loadResource(String filePath, T resource) {
        FhirContext ctx = FhirContext.forR4();
        return (T) ctx.newJsonParser().parseResource(resource.getClass(), readJSON(filePath));
    }

    /**
     * Read JSON file from the classpath.
     *
     * @param filePath the file path of the JSON file to read.
     * @return JSON content as a string
     */
    protected String readJSON(String filePath) {
        InputStream is = getClass().getClassLoader().getResourceAsStream(filePath);
        if (is == null) {
            throw new IllegalArgumentException("File not found! " + filePath);
        } else {
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            return reader.lines().collect(Collectors.joining(System.lineSeparator()));
        }
    }
}
