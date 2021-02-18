package net.mekomsolutions.senaite.integration;

import java.net.URL;
import org.apache.camel.CamelContext;
import org.apache.camel.ExtendedCamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.model.ModelCamelContext;
import org.apache.camel.model.RoutesDefinition;
import org.apache.camel.test.spring.CamelSpringBootRunner;
import org.apache.camel.test.spring.MockEndpoints;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import com.google.common.io.Resources;

@RunWith(CamelSpringBootRunner.class)
@SpringBootTest(classes = TestConfiguration.class)
@MockEndpoints
@TestPropertySource(locations="classpath:application-test.properties")
public abstract class BaseCamelContextSensitiveTest {

    @Autowired
    protected CamelContext camelContext;
    
    @Autowired
    protected ProducerTemplate producerTemplate;
    
    @Before
	public void init() {
    	camelContext.adapt(ModelCamelContext.class).getRouteDefinitions().clear();
	}
    
    protected void loadXmlDefinedRoute(String fileName) throws Exception {
    	URL url = Resources.getResource(fileName);
    	
    	RoutesDefinition routes = (RoutesDefinition) camelContext.adapt(ExtendedCamelContext.class).getXMLRoutesDefinitionLoader().loadRoutesDefinition(camelContext, url.openStream());
    	camelContext.adapt(ModelCamelContext.class).addRouteDefinitions(routes.getRoutes());
    }
}