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
package org.apache.camel.cdi.ee;

import org.apache.camel.CamelContext;
import org.apache.camel.ServiceStatus;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

@RunWith(Arquillian.class)
public class CamelEE7Test {

    @Deployment
    public static Archive<?> deployment() {
        return ShrinkWrap.create(EnterpriseArchive.class, "camel-ee7.ear")
            .addAsLibraries(
                Maven.configureResolver()
                    .workOffline()
                    .loadPomFromFile("pom.xml")
                    .resolve("io.astefanutti.camel.cdi:camel-cdi")
                    .withTransitivity()
                    .asList(JavaArchive.class))
            .addAsModule(
                ShrinkWrap.create(JavaArchive.class, "camel-ee7.jar")
                    // FIXME: Arquillian throws a ClassNotFoundException when removing the Bootstrap class
                    .addClasses(Bootstrap.class, CamelRoute.class, HelloCamel.class)
                    // TODO: to be removed when ARQ-659 is fixed
                    .addClass(CamelEE7Test.class)
                    .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml"));
    }

    @Test
    @InSequence(1)
    public void verifyContext(CamelContext context) {
        assertThat("Camel context is not started!", context.getStatus(), is(equalTo(ServiceStatus.Started)));
        assertThat("Timer route is not started!", context.getRouteStatus("timer"), is(equalTo(ServiceStatus.Started)));
    }

    @Test
    @InSequence(2)
    public void pauseWhileLogging() throws InterruptedException {
        Thread.sleep(10000L);
    }
}