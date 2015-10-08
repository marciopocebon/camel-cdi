package org.apache.camel.cdi.se;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.cdi.CdiCamelContext;
import org.apache.camel.cdi.CdiCamelExtension;
import org.apache.camel.cdi.ContextName;
import org.apache.camel.cdi.Uri;
import org.apache.camel.cdi.se.bean.DefaultCamelContextBean;
import org.apache.camel.cdi.se.bean.FirstNamedCamelContextBean;
import org.apache.camel.cdi.se.bean.FirstNamedCamelContextRoute;
import org.apache.camel.cdi.se.bean.SecondNamedCamelContextBean;
import org.apache.camel.cdi.se.bean.SecondNamedCamelContextRoute;
import org.apache.camel.component.mock.MockEndpoint;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

@RunWith(Arquillian.class)
public class ContextComponentTest {

    @Deployment
    public static Archive<?> deployment() {
        return ShrinkWrap.create(JavaArchive.class)
            // Camel CDI
            .addPackage(CdiCamelExtension.class.getPackage())
            // Test classes
            .addClasses(DefaultCamelContextBean.class,
                FirstNamedCamelContextBean.class,
                FirstNamedCamelContextRoute.class,
                SecondNamedCamelContextBean.class,
                SecondNamedCamelContextRoute.class)
            // Bean archive deployment descriptor
            .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @Inject
    private CamelContext main;

    @Test
    @InSequence(1)
    public void addRouteToMainContext() throws Exception {
        main.addRoutes(new RouteBuilder() {
            @Override
            public void configure() {
                from("direct:inbound").to("first:in");
                // FIXME: The context component does not support multiple logical endpoints with the same remaining defined in two distinct Camel contexts. See https://issues.apache.org/jira/browse/CAMEL-9200.
                // from("first:out").to("second:in");
                from("first:out").to("mock:outbound");
            }
        });
    }

    @Test
    @InSequence(2)
    public void startCamelContexts(@ContextName("first") CamelContext first, @ContextName("second") CamelContext second) throws Exception {
        first.start();
        second.start();
        main.start();
    }

    @Test
    @InSequence(3)
    public void sendMessageToInbound(@Uri("direct:inbound") ProducerTemplate inbound, @Uri("mock:outbound") MockEndpoint outbound) throws InterruptedException {
        outbound.expectedMessageCount(1);
        outbound.expectedBodiesReceived("first-test");

        inbound.sendBody("test");

        MockEndpoint.assertIsSatisfied(1L, TimeUnit.SECONDS, outbound);
    }
}