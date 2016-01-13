/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.cdi.se;

import org.apache.camel.Endpoint;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.cdi.CdiCamelExtension;
import org.apache.camel.cdi.ContextName;
import org.apache.camel.cdi.Uri;
import org.apache.camel.cdi.se.bean.FirstCamelContextBean;
import org.apache.camel.cdi.test.ExpectedDeploymentException;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;

import javax.enterprise.inject.spi.DeploymentException;
import javax.inject.Inject;

import static org.hamcrest.Matchers.containsString;

@RunWith(Arquillian.class)
public class UriWithWrongContextTest {

    @Deployment
    public static Archive<?> deployment() {
        return ShrinkWrap.create(JavaArchive.class)
            // Camel CDI
            .addPackage(CdiCamelExtension.class.getPackage())
            // Test classes
            .addClasses(FirstCamelContextBean.class, UriWithWrongContextRoute.class)
            // Bean archive deployment descriptor
            .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @ClassRule
    public static TestRule exception = ExpectedDeploymentException.none()
        .expect(DeploymentException.class)
        .expectMessage(containsString("Error adding routes of type [" + UriWithWrongContextRoute.class.getName() + "] to Camel context [first]"))
        .expectMessage(containsString("Error injecting endpoint annotated with @org.apache.camel.cdi.Uri"))
        .expectMessage(containsString("No Camel context with name [second] is deployed!"));

    @Test
    public void test() {
    }
}

@ContextName("first")
class UriWithWrongContextRoute extends RouteBuilder {

    @Inject
    @Uri(value = "direct:inbound", context = "second")
    Endpoint inbound;

    @Override
    public void configure() throws Exception {
        from(inbound).to("mock:outbound");
    }
}

