package com.ozonehis.eip.routes.senaite;

import ch.qos.logback.classic.Level;
import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.support.DefaultExchange;
import org.apache.camel.test.spring.junit5.MockEndpoints;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@MockEndpoints
public class CancelOrderToSenaiteRouteITest extends BaseCamelRoutesTest {

    @EndpointInject(value = "mock:fetchcanceledOrderFromSenaiteEndpoint")
    private MockEndpoint fetchcanceledOrderFromSenaiteEndpoint;

    @EndpointInject(value = "mock:fetchActiveOrderFromSenaiteEndpoint")
    private MockEndpoint fetchActiveOrderFromSenaiteEndpoint;

    @EndpointInject(value = "mock:updateSenaiteWithoutThrowingEndpoint")
    private MockEndpoint updateSenaiteWithoutThrowingEndpoint;

    @EndpointInject(value = "mock:updateSenaiteEndpoint")
    private MockEndpoint updateSenaiteEndpoint;

    @EndpointInject(value = "mock:authenticateToSenaiteRoute")
    private MockEndpoint authenticateToSenaiteRoute;

    private Exchange exchange;

    private int resultWaitTimeMillis = 100;

    @BeforeEach
    public void setup() throws Exception {
        loadXmlRoutesInDirectory("camel", "cancel-order-to-senaite-route.xml");

        advise("cancel-order-to-senaite", new AdviceWithRouteBuilder() {
            @Override
            public void configure() {
                weaveByToString(
                                ".*@@API/senaite/v1/search\\?getClientSampleID=\\$\\{exchangeProperty.order-to-cancel\\}\\&catalog=senaite_catalog_sample\\&complete=true\\&review_state=cancelled]")
                        .replace()
                        .to("mock:fetchcanceledOrderFromSenaiteEndpoint");
                weaveByToString(
                                ".*@@API/senaite/v1/search\\?getClientSampleID=\\$\\{exchangeProperty.order-to-cancel\\}\\&catalog=senaite_catalog_sample\\&complete=true]")
                        .replace()
                        .to("mock:fetchActiveOrderFromSenaiteEndpoint");
                weaveByToString(".*@@API/senaite/v1/update\\?throwExceptionOnFailure=false]")
                        .replace()
                        .to("mock:updateSenaiteWithoutThrowingEndpoint");
                weaveByToString(".*@@API/senaite/v1/update]").replace().toD("mock:updateSenaiteEndpoint");
                weaveByToString("To[direct:authenticate-to-senaite]").replace().toD("mock:authenticateToSenaiteRoute");
            }
        });

        setupExpectations();

        exchange = new DefaultExchange(camelContext);
    }

    @AfterEach
    public void reset() throws Exception {
        authenticateToSenaiteRoute.reset();
        fetchcanceledOrderFromSenaiteEndpoint.reset();
        fetchActiveOrderFromSenaiteEndpoint.reset();
        updateSenaiteWithoutThrowingEndpoint.reset();
    }

    @Test
    public void shouldCancelOrder() throws Exception {
        // setup
        exchange.getIn().setBody("27d730cb-1c04-4ced-a2ed-ad0f18fed728");

        // replay
        producerTemplate.send("direct:cancel-order-to-senaite", exchange);

        // verify
        authenticateToSenaiteRoute.assertExchangeReceived(0);
        fetchcanceledOrderFromSenaiteEndpoint.assertIsSatisfied();
        fetchActiveOrderFromSenaiteEndpoint.assertIsSatisfied();
        updateSenaiteWithoutThrowingEndpoint.assertIsSatisfied();
    }

    @Test
    public void shouldLogExistanceOfMultipleOrdersForCancelledOrder() throws Exception {
        // setup
        exchange.getIn().setBody("e585eced-0dd5-48eb-a267-a3049ab1ee538");

        // replay
        producerTemplate.send("direct:cancel-order-to-senaite", exchange);

        // verify
        authenticateToSenaiteRoute.assertExchangeReceived(0);
        // fetchcanceledOrderFromSenaiteEndpoint.assertIsSatisfied();
        // fetchActiveOrderFromSenaiteEndpoint.assertIsSatisfied();
        updateSenaiteWithoutThrowingEndpoint.assertIsNotSatisfied();
        assertMessageLogged(
                Level.INFO,
                "Could not cancel order identified by: 'e585eced-0dd5-48eb-a267-a3049ab1ee538' due to multiple orders existing with same identifier on SENAITE");
    }

    private void setupExpectations() {
        fetchcanceledOrderFromSenaiteEndpoint.whenAnyExchangeReceived(
                exchange -> exchange.getIn()
                        .setBody(
                                "{\"count\":0,\"pagesize\":25,\"items\":[],\"page\":1,\"_runtime\":0.005473136901855469,\"next\":null,\"pages\":1,\"previous\":null}"));
        fetchcanceledOrderFromSenaiteEndpoint.expectedHeaderReceived(Exchange.HTTP_METHOD, "GET");

        fetchActiveOrderFromSenaiteEndpoint.whenAnyExchangeReceived(exchange -> {
            if ("27d730cb-1c04-4ced-a2ed-ad0f18fed728".equals(exchange.getProperty("order-to-cancel"))) {
                exchange.getIn()
                        .setBody(
                                "{\"count\":1,\"pagesize\":25,\"items\":[{\"SampleTypeTitle\":\"Blood\",\"getSampleTypeUID\":\"520131495de146a88ab58c614839054b\",\"RejectionReasons\":null,\"getContactFullName\":\"Super Man\",\"ProfilesUID\":[\"ae25816a6d804edba01ebdb24ffcd14e\"],\"ResultsInterpretation\":null,\"Template\":{\"url\":\"http://localhost:8081/senaite/bika_setup/bika_artemplates/artemplate-6\",\"uid\":\"13da8d9611d6485099c6f7c08a2c6355\",\"api_url\":\"http://localhost:8081/senaite/@@API/senaite/v1/artemplate/13da8d9611d6485099c6f7c08a2c6355\"},\"ContactUID\":\"4f8b31a780bf4754af104d42173d3c41\",\"getInternalUse\":false,\"getSamplerEmail\":\"\",\"ClientSampleID\":\"27d730cb-1c04-4ced-a2ed-ad0f18fed728\",\"title\":\"BLD-0001\",\"MemberDiscount\":\"0.00\",\"parent_id\":\"client-1\",\"parent_url\":\"http://localhost:8081/senaite/@@API/senaite/v1/client/b774b05115824532bb589cbf47715570\",\"TemplateURL\":\"/senaite/bika_setup/bika_artemplates/artemplate-6\",\"assigned_state\":\"unassigned\",\"getDistrict\":null,\"getProfilesUID\":[\"ae25816a6d804edba01ebdb24ffcd14e\"],\"getTemplateTitle\":\"LAB1015 - Thyroid Function Tests(TSH-T3-T4) Template\",\"getContactURL\":\"/senaite/clients/client-1/contact-1\",\"TemplateTitle\":\"LAB1015 - Thyroid Function Tests(TSH-T3-T4) Template\",\"getContactUID\":\"4f8b31a780bf4754af104d42173d3c41\",\"parent_uid\":\"b774b05115824532bb589cbf47715570\",\"getClientSampleID\":\"27d730cb-1c04-4ced-a2ed-ad0f18fed728\",\"getProfilesTitleStr\":\"LAB1015 - Thyroid Function Tests(TSH-T3-T4)\",\"creation_date\":\"2021-11-29T13:17:37+00:00\",\"Priority\":\"3\",\"modification_date\":\"2021-11-29T13:17:39+00:00\",\"getClientTitle\":\"Johnson Smith (0298aa1b-7fa1-4244-93e7-c5138df63bb3)\",\"getAnalysesNum\":[0,3,3,0],\"getClientID\":\"0298aa1b-7fa1-4244-93e7-c5138df63bb3\",\"review_state\":\"sample_due\",\"tags\":[],\"getProfilesURL\":[\"/senaite/bika_setup/bika_analysisprofiles/analysisprofile-6\"],\"Profiles\":[{\"url\":\"http://localhost:8081/senaite/bika_setup/bika_analysisprofiles/analysisprofile-6\",\"uid\":\"ae25816a6d804edba01ebdb24ffcd14e\",\"api_url\":\"http://localhost:8081/senaite/@@API/senaite/v1/analysisprofile/ae25816a6d804edba01ebdb24ffcd14e\"}],\"Contact\":{\"url\":\"http://localhost:8081/senaite/clients/client-1/contact-1\",\"uid\":\"4f8b31a780bf4754af104d42173d3c41\",\"api_url\":\"http://localhost:8081/senaite/@@API/senaite/v1/contact/4f8b31a780bf4754af104d42173d3c41\"},\"path\":\"/senaite/clients/client-1/BLD-0001\",\"CreatorFullName\":\"admin\",\"language\":\"en\",\"created\":\"2021-11-29T13:17:37+00:00\",\"getSampleTypeTitle\":\"Blood\",\"getCreatorFullName\":\"admin\",\"TemplateUID\":\"13da8d9611d6485099c6f7c08a2c6355\",\"parent_path\":\"/senaite/clients/client-1\",\"Analyses\":[{\"url\":\"http://localhost:8081/senaite/clients/client-1/BLD-0001/TSH\",\"uid\":\"f1f60a5498bb4a64bd6bfcda2ce28a5a\",\"api_url\":\"http://localhost:8081/senaite/@@API/senaite/v1/analysis/f1f60a5498bb4a64bd6bfcda2ce28a5a\"},{\"url\":\"http://localhost:8081/senaite/clients/client-1/BLD-0001/T3\",\"uid\":\"8dc73cfcfaca4d2892d1de9b959a51c1\",\"api_url\":\"http://localhost:8081/senaite/@@API/senaite/v1/analysis/8dc73cfcfaca4d2892d1de9b959a51c1\"},{\"url\":\"http://localhost:8081/senaite/clients/client-1/BLD-0001/T4\",\"uid\":\"84aa9d51a11e4fa8b205c1e40bc29831\",\"api_url\":\"http://localhost:8081/senaite/@@API/senaite/v1/analysis/84aa9d51a11e4fa8b205c1e40bc29831\"}],\"getPrioritySortkey\":\"3.2021-11-29T13:17:37+00:00\",\"getSamplingWorkflowEnabled\":false,\"getClientURL\":\"/senaite/clients/client-1\",\"modified\":\"2021-11-29T13:17:39+00:00\",\"Preservation\":{},\"getSamplingDeviationTitle\":\"\",\"getTemplateUID\":\"13da8d9611d6485099c6f7c08a2c6355\",\"ProfilesTitle\":[\"LAB1015 - Thyroid Function Tests(TSH-T3-T4)\"],\"Container\":{},\"uid\":\"0b493f7934da4d618d57938e4fdc4246\",\"ProfilesURL\":[\"/senaite/bika_setup/bika_analysisprofiles/analysisprofile-6\"],\"getPrinted\":\"0\",\"id\":\"BLD-0001\",\"getSamplingDate\":null,\"getTemplateURL\":\"/senaite/bika_setup/bika_artemplates/artemplate-6\",\"api_url\":\"http://localhost:8081/senaite/@@API/senaite/v1/analysisrequest/0b493f7934da4d618d57938e4fdc4246\",\"author\":\"admin\",\"SampleTypeUID\":\"520131495de146a88ab58c614839054b\",\"SampleCondition\":{},\"Remarks\":null,\"description\":\"BLD-0001 Johnson Smith (0298aa1b-7fa1-4244-93e7-c5138df63bb3)\",\"SampleType\":{\"url\":\"http://localhost:8081/senaite/bika_setup/bika_sampletypes/sampletype-1\",\"uid\":\"520131495de146a88ab58c614839054b\",\"api_url\":\"http://localhost:8081/senaite/@@API/senaite/v1/sampletype/520131495de146a88ab58c614839054b\"},\"ProfilesTitleStr\":\"LAB1015 - Thyroid Function Tests(TSH-T3-T4)\",\"getProgress\":0,\"getClientUID\":\"b774b05115824532bb589cbf47715570\",\"effective\":\"1000-01-01T00:00:00+00:00\",\"url\":\"http://localhost:8081/senaite/clients/client-1/BLD-0001\",\"getPhysicalPath\":[\"\",\"senaite\",\"clients\",\"client-1\",\"BLD-0001\"],\"getProfilesTitle\":[\"LAB1015 - Thyroid Function Tests(TSH-T3-T4)\"],\"creators\":[\"admin\"]}],\"page\":1,\"_runtime\":0.05715298652648926,\"pages\":1}");
            } else if ("e585eced-0dd5-48eb-a267-a3049ab1ee538".equals(exchange.getProperty("order-to-cancel"))) {
                exchange.getIn()
                        .setBody(
                                "{\"count\":2,\"pagesize\":25,\"items\":[{\"SampleTypeTitle\":\"Blood\",\"getSampleTypeUID\":\"520131495de146a88ab58c614839054b\",\"RejectionReasons\":null,\"getContactFullName\":\"Super Man\",\"ProfilesUID\":[\"ae25816a6d804edba01ebdb24ffcd14e\"],\"ResultsInterpretation\":null,\"Template\":{\"url\":\"http://localhost:8081/senaite/bika_setup/bika_artemplates/artemplate-6\",\"uid\":\"13da8d9611d6485099c6f7c08a2c6355\",\"api_url\":\"http://localhost:8081/senaite/@@API/senaite/v1/artemplate/13da8d9611d6485099c6f7c08a2c6355\"},\"ContactUID\":\"4f8b31a780bf4754af104d42173d3c41\",\"getInternalUse\":false,\"getSamplerEmail\":\"\",\"ClientSampleID\":\"27d730cb-1c04-4ced-a2ed-ad0f18fed728\",\"title\":\"BLD-0001\",\"MemberDiscount\":\"0.00\",\"parent_id\":\"client-1\",\"parent_url\":\"http://localhost:8081/senaite/@@API/senaite/v1/client/b774b05115824532bb589cbf47715570\",\"TemplateURL\":\"/senaite/bika_setup/bika_artemplates/artemplate-6\",\"assigned_state\":\"unassigned\",\"getDistrict\":null,\"getProfilesUID\":[\"ae25816a6d804edba01ebdb24ffcd14e\"],\"getTemplateTitle\":\"LAB1015 - Thyroid Function Tests(TSH-T3-T4) Template\",\"getContactURL\":\"/senaite/clients/client-1/contact-1\",\"TemplateTitle\":\"LAB1015 - Thyroid Function Tests(TSH-T3-T4) Template\",\"getContactUID\":\"4f8b31a780bf4754af104d42173d3c41\",\"parent_uid\":\"b774b05115824532bb589cbf47715570\",\"getClientSampleID\":\"27d730cb-1c04-4ced-a2ed-ad0f18fed728\",\"getProfilesTitleStr\":\"LAB1015 - Thyroid Function Tests(TSH-T3-T4)\",\"creation_date\":\"2021-11-29T13:17:37+00:00\",\"Priority\":\"3\",\"modification_date\":\"2021-11-29T13:17:39+00:00\",\"getClientTitle\":\"Johnson Smith (0298aa1b-7fa1-4244-93e7-c5138df63bb3)\",\"getAnalysesNum\":[0,3,3,0],\"getClientID\":\"0298aa1b-7fa1-4244-93e7-c5138df63bb3\",\"review_state\":\"sample_due\",\"tags\":[],\"getProfilesURL\":[\"/senaite/bika_setup/bika_analysisprofiles/analysisprofile-6\"],\"Profiles\":[{\"url\":\"http://localhost:8081/senaite/bika_setup/bika_analysisprofiles/analysisprofile-6\",\"uid\":\"ae25816a6d804edba01ebdb24ffcd14e\",\"api_url\":\"http://localhost:8081/senaite/@@API/senaite/v1/analysisprofile/ae25816a6d804edba01ebdb24ffcd14e\"}],\"Contact\":{\"url\":\"http://localhost:8081/senaite/clients/client-1/contact-1\",\"uid\":\"4f8b31a780bf4754af104d42173d3c41\",\"api_url\":\"http://localhost:8081/senaite/@@API/senaite/v1/contact/4f8b31a780bf4754af104d42173d3c41\"},\"path\":\"/senaite/clients/client-1/BLD-0001\",\"CreatorFullName\":\"admin\",\"language\":\"en\",\"created\":\"2021-11-29T13:17:37+00:00\",\"getSampleTypeTitle\":\"Blood\",\"getCreatorFullName\":\"admin\",\"TemplateUID\":\"13da8d9611d6485099c6f7c08a2c6355\",\"parent_path\":\"/senaite/clients/client-1\",\"Analyses\":[{\"url\":\"http://localhost:8081/senaite/clients/client-1/BLD-0001/TSH\",\"uid\":\"f1f60a5498bb4a64bd6bfcda2ce28a5a\",\"api_url\":\"http://localhost:8081/senaite/@@API/senaite/v1/analysis/f1f60a5498bb4a64bd6bfcda2ce28a5a\"},{\"url\":\"http://localhost:8081/senaite/clients/client-1/BLD-0001/T3\",\"uid\":\"8dc73cfcfaca4d2892d1de9b959a51c1\",\"api_url\":\"http://localhost:8081/senaite/@@API/senaite/v1/analysis/8dc73cfcfaca4d2892d1de9b959a51c1\"},{\"url\":\"http://localhost:8081/senaite/clients/client-1/BLD-0001/T4\",\"uid\":\"84aa9d51a11e4fa8b205c1e40bc29831\",\"api_url\":\"http://localhost:8081/senaite/@@API/senaite/v1/analysis/84aa9d51a11e4fa8b205c1e40bc29831\"}],\"getPrioritySortkey\":\"3.2021-11-29T13:17:37+00:00\",\"getSamplingWorkflowEnabled\":false,\"getClientURL\":\"/senaite/clients/client-1\",\"modified\":\"2021-11-29T13:17:39+00:00\",\"Preservation\":{},\"getSamplingDeviationTitle\":\"\",\"getTemplateUID\":\"13da8d9611d6485099c6f7c08a2c6355\",\"ProfilesTitle\":[\"LAB1015 - Thyroid Function Tests(TSH-T3-T4)\"],\"Container\":{},\"uid\":\"0b493f7934da4d618d57938e4fdc4246\",\"ProfilesURL\":[\"/senaite/bika_setup/bika_analysisprofiles/analysisprofile-6\"],\"getPrinted\":\"0\",\"id\":\"BLD-0001\",\"getSamplingDate\":null,\"getTemplateURL\":\"/senaite/bika_setup/bika_artemplates/artemplate-6\",\"api_url\":\"http://localhost:8081/senaite/@@API/senaite/v1/analysisrequest/0b493f7934da4d618d57938e4fdc4246\",\"author\":\"admin\",\"SampleTypeUID\":\"520131495de146a88ab58c614839054b\",\"SampleCondition\":{},\"Remarks\":null,\"description\":\"BLD-0001 Johnson Smith (0298aa1b-7fa1-4244-93e7-c5138df63bb3)\",\"SampleType\":{\"url\":\"http://localhost:8081/senaite/bika_setup/bika_sampletypes/sampletype-1\",\"uid\":\"520131495de146a88ab58c614839054b\",\"api_url\":\"http://localhost:8081/senaite/@@API/senaite/v1/sampletype/520131495de146a88ab58c614839054b\"},\"ProfilesTitleStr\":\"LAB1015 - Thyroid Function Tests(TSH-T3-T4)\",\"getProgress\":0,\"getClientUID\":\"b774b05115824532bb589cbf47715570\",\"effective\":\"1000-01-01T00:00:00+00:00\",\"url\":\"http://localhost:8081/senaite/clients/client-1/BLD-0001\",\"getPhysicalPath\":[\"\",\"senaite\",\"clients\",\"client-1\",\"BLD-0001\"],\"getProfilesTitle\":[\"LAB1015 - Thyroid Function Tests(TSH-T3-T4)\"],\"creators\":[\"admin\"]},{\"SampleTypeTitle\":\"Blood\",\"getSampleTypeUID\":\"520131495de146a88ab58c614839054b\",\"RejectionReasons\":null,\"getContactFullName\":\"Super Man\",\"ProfilesUID\":[\"ae25816a6d804edba01ebdb24ffcd14e\"],\"ResultsInterpretation\":null,\"Template\":{\"url\":\"http://localhost:8081/senaite/bika_setup/bika_artemplates/artemplate-6\",\"uid\":\"13da8d9611d6485099c6f7c08a2c6355\",\"api_url\":\"http://localhost:8081/senaite/@@API/senaite/v1/artemplate/13da8d9611d6485099c6f7c08a2c6355\"},\"ContactUID\":\"4f8b31a780bf4754af104d42173d3c41\",\"getInternalUse\":false,\"getSamplerEmail\":\"\",\"ClientSampleID\":\"27d730cb-1c04-4ced-a2ed-ad0f18fed728\",\"title\":\"BLD-0001\",\"MemberDiscount\":\"0.00\",\"parent_id\":\"client-1\",\"parent_url\":\"http://localhost:8081/senaite/@@API/senaite/v1/client/b774b05115824532bb589cbf47715570\",\"TemplateURL\":\"/senaite/bika_setup/bika_artemplates/artemplate-6\",\"assigned_state\":\"unassigned\",\"getDistrict\":null,\"getProfilesUID\":[\"ae25816a6d804edba01ebdb24ffcd14e\"],\"getTemplateTitle\":\"LAB1015 - Thyroid Function Tests(TSH-T3-T4) Template\",\"getContactURL\":\"/senaite/clients/client-1/contact-1\",\"TemplateTitle\":\"LAB1015 - Thyroid Function Tests(TSH-T3-T4) Template\",\"getContactUID\":\"4f8b31a780bf4754af104d42173d3c41\",\"parent_uid\":\"b774b05115824532bb589cbf47715570\",\"getClientSampleID\":\"27d730cb-1c04-4ced-a2ed-ad0f18fed728\",\"getProfilesTitleStr\":\"LAB1015 - Thyroid Function Tests(TSH-T3-T4)\",\"creation_date\":\"2021-11-29T13:17:37+00:00\",\"Priority\":\"3\",\"modification_date\":\"2021-11-29T13:17:39+00:00\",\"getClientTitle\":\"Johnson Smith (0298aa1b-7fa1-4244-93e7-c5138df63bb3)\",\"getAnalysesNum\":[0,3,3,0],\"getClientID\":\"0298aa1b-7fa1-4244-93e7-c5138df63bb3\",\"review_state\":\"sample_due\",\"tags\":[],\"getProfilesURL\":[\"/senaite/bika_setup/bika_analysisprofiles/analysisprofile-6\"],\"Profiles\":[{\"url\":\"http://localhost:8081/senaite/bika_setup/bika_analysisprofiles/analysisprofile-6\",\"uid\":\"ae25816a6d804edba01ebdb24ffcd14e\",\"api_url\":\"http://localhost:8081/senaite/@@API/senaite/v1/analysisprofile/ae25816a6d804edba01ebdb24ffcd14e\"}],\"Contact\":{\"url\":\"http://localhost:8081/senaite/clients/client-1/contact-1\",\"uid\":\"4f8b31a780bf4754af104d42173d3c41\",\"api_url\":\"http://localhost:8081/senaite/@@API/senaite/v1/contact/4f8b31a780bf4754af104d42173d3c41\"},\"path\":\"/senaite/clients/client-1/BLD-0001\",\"CreatorFullName\":\"admin\",\"language\":\"en\",\"created\":\"2021-11-29T13:17:37+00:00\",\"getSampleTypeTitle\":\"Blood\",\"getCreatorFullName\":\"admin\",\"TemplateUID\":\"13da8d9611d6485099c6f7c08a2c6355\",\"parent_path\":\"/senaite/clients/client-1\",\"Analyses\":[{\"url\":\"http://localhost:8081/senaite/clients/client-1/BLD-0001/TSH\",\"uid\":\"f1f60a5498bb4a64bd6bfcda2ce28a5a\",\"api_url\":\"http://localhost:8081/senaite/@@API/senaite/v1/analysis/f1f60a5498bb4a64bd6bfcda2ce28a5a\"},{\"url\":\"http://localhost:8081/senaite/clients/client-1/BLD-0001/T3\",\"uid\":\"8dc73cfcfaca4d2892d1de9b959a51c1\",\"api_url\":\"http://localhost:8081/senaite/@@API/senaite/v1/analysis/8dc73cfcfaca4d2892d1de9b959a51c1\"},{\"url\":\"http://localhost:8081/senaite/clients/client-1/BLD-0001/T4\",\"uid\":\"84aa9d51a11e4fa8b205c1e40bc29831\",\"api_url\":\"http://localhost:8081/senaite/@@API/senaite/v1/analysis/84aa9d51a11e4fa8b205c1e40bc29831\"}],\"getPrioritySortkey\":\"3.2021-11-29T13:17:37+00:00\",\"getSamplingWorkflowEnabled\":false,\"getClientURL\":\"/senaite/clients/client-1\",\"modified\":\"2021-11-29T13:17:39+00:00\",\"Preservation\":{},\"getSamplingDeviationTitle\":\"\",\"getTemplateUID\":\"13da8d9611d6485099c6f7c08a2c6355\",\"ProfilesTitle\":[\"LAB1015 - Thyroid Function Tests(TSH-T3-T4)\"],\"Container\":{},\"uid\":\"0b493f7934da4d618d57938e4fdc4246\",\"ProfilesURL\":[\"/senaite/bika_setup/bika_analysisprofiles/analysisprofile-6\"],\"getPrinted\":\"0\",\"id\":\"BLD-0001\",\"getSamplingDate\":null,\"getTemplateURL\":\"/senaite/bika_setup/bika_artemplates/artemplate-6\",\"api_url\":\"http://localhost:8081/senaite/@@API/senaite/v1/analysisrequest/0b493f7934da4d618d57938e4fdc4246\",\"author\":\"admin\",\"SampleTypeUID\":\"520131495de146a88ab58c614839054b\",\"SampleCondition\":{},\"Remarks\":null,\"description\":\"BLD-0001 Johnson Smith (0298aa1b-7fa1-4244-93e7-c5138df63bb3)\",\"SampleType\":{\"url\":\"http://localhost:8081/senaite/bika_setup/bika_sampletypes/sampletype-1\",\"uid\":\"520131495de146a88ab58c614839054b\",\"api_url\":\"http://localhost:8081/senaite/@@API/senaite/v1/sampletype/520131495de146a88ab58c614839054b\"},\"ProfilesTitleStr\":\"LAB1015 - Thyroid Function Tests(TSH-T3-T4)\",\"getProgress\":0,\"getClientUID\":\"b774b05115824532bb589cbf47715570\",\"effective\":\"1000-01-01T00:00:00+00:00\",\"url\":\"http://localhost:8081/senaite/clients/client-1/BLD-0001\",\"getPhysicalPath\":[\"\",\"senaite\",\"clients\",\"client-1\",\"BLD-0001\"],\"getProfilesTitle\":[\"LAB1015 - Thyroid Function Tests(TSH-T3-T4)\"],\"creators\":[\"admin\"]}],\"page\":1,\"_runtime\":0.05715298652648926,\"pages\":1}");
            }
        });
        fetchActiveOrderFromSenaiteEndpoint.expectedHeaderReceived(Exchange.HTTP_METHOD, "GET");

        updateSenaiteWithoutThrowingEndpoint.expectedHeaderReceived(Exchange.HTTP_METHOD, "POST");
        updateSenaiteWithoutThrowingEndpoint.expectedBodiesReceived(
                "{\"uid\": \"0b493f7934da4d618d57938e4fdc4246\", \"Client\": \"b774b05115824532bb589cbf47715570\", \"transition\": \"cancel\"}");
    }
}
