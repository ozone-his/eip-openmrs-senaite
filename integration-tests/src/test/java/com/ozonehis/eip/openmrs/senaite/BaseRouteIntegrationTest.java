/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openmrs.senaite;

import ca.uhn.fhir.context.FhirContext;
import com.ozonehis.camel.test.infra.senaite.services.SenaiteService;
import com.ozonehis.camel.test.infra.senaite.services.SenaiteServiceFactory;
import com.ozonehis.eip.openmrs.senaite.config.SenaiteConfig;
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

    //    protected static SenaiteConfig createSenaiteClient() {
    //        return new SenaiteConfig(SENAITE_SERVER_URL, SENAITE_USERNAME, SENAITE_PASSWORD);
    //    }
    //
    //    public SenaiteClient getSenaiteClient() {
    //        if (senaiteClient == null) {
    //            senaiteClient = createSenaiteClient();
    //        }
    //        return senaiteClient;
    //    }

    protected @Nonnull CamelContext getContextWithRouting(CamelContext context) throws Exception {

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
