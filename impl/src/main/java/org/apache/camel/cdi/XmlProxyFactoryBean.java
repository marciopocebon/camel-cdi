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
package org.apache.camel.cdi;

import org.apache.camel.CamelContext;
import org.apache.camel.Endpoint;
import org.apache.camel.Producer;
import org.apache.camel.cdi.xml.BeanManagerAware;
import org.apache.camel.cdi.xml.CamelContextFactoryBean;
import org.apache.camel.component.bean.ProxyHelper;
import org.apache.camel.core.xml.CamelProxyFactoryDefinition;
import org.apache.camel.util.ObjectHelper;
import org.apache.camel.util.ServiceHelper;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.CreationException;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanAttributes;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.InjectionPoint;
import java.util.Collections;
import java.util.Set;

import static org.apache.camel.cdi.XmlCdiBeanFactory.getAttribute;

final class XmlProxyFactoryBean<T> extends BeanAttributesDelegate<T> implements Bean<T> {

    private final BeanManager manager;

    private final CamelContextFactoryBean context;

    private final CamelProxyFactoryDefinition proxy;

    private Producer producer;

    XmlProxyFactoryBean(BeanManager manager, CamelContextFactoryBean context, CamelProxyFactoryDefinition proxy, BeanAttributes<T> attributes) {
        super(attributes);
        this.manager = manager;
        this.context = context;
        this.proxy = proxy;
    }

    @Override
    public Class<?> getBeanClass() {
        return getServiceInterface();
    }

    @Override
    public Set<InjectionPoint> getInjectionPoints() {
        return Collections.emptySet();
    }

    @Override
    public boolean isNullable() {
        return false;
    }

    @Override
    public T create(CreationalContext<T> creationalContext) {
        try {
            CamelContext context = BeanManagerAware.getCamelContextById(manager,
                ObjectHelper.isNotEmpty(getCamelContextId())
                    ? getCamelContextId()
                    : this.context.getId());

            Endpoint endpoint;
            if (ObjectHelper.isNotEmpty(getServiceRef()))
                endpoint = context.getRegistry().lookupByNameAndType(getServiceRef(), Endpoint.class);
            else if (ObjectHelper.isNotEmpty(getServiceUrl()))
                endpoint = context.getEndpoint(getServiceUrl());
            else
                throw new CreationException("serviceUrl or serviceRef must not be empty!");

            if (endpoint == null)
                throw new CreationException("Could not resolve endpoint: "
                    + (ObjectHelper.isNotEmpty(getServiceRef())
                    ? getServiceRef()
                    : getServiceUrl()));

            // binding is enabled by default
            boolean bind = getBinding() != null ? getBinding() : true;

            producer = endpoint.createProducer();
            ServiceHelper.startService(producer);
            return ProxyHelper.createProxy(endpoint, bind, producer, getServiceInterface());
        } catch (Exception cause) {
            throw new CreationException("Error while creating instance for " + this, cause);
        }
    }

    @Override
    public void destroy(T instance, CreationalContext<T> creationalContext) {
        try {
            ServiceHelper.stopService(producer);
        } catch (Exception cause) {
            throw ObjectHelper.wrapRuntimeCamelException(cause);
        }
    }

    private Class<T> getServiceInterface() {
        return getAttribute(proxy, "serviceInterface", Class.class);
    }

    private String getServiceUrl() {
        return getAttribute(proxy, "serviceUrl", String.class);
    }

    private String getServiceRef() {
        return getAttribute(proxy, "serviceRef", String.class);
    }

    private Boolean getBinding() {
        return getAttribute(proxy, "binding", Boolean.class);
    }

    private String getCamelContextId() {
        return getAttribute(proxy, "camelContextId", String.class);
    }
}
