/**
 * Copyright (C) 2014 Antonin Stefanutti (antonin.stefanutti@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.astefanutti.camel.cdi;

import org.apache.camel.IsSingleton;
import org.apache.camel.spi.Injector;

import javax.enterprise.inject.spi.BeanManager;

class CdiInjector implements Injector {

    private final Injector injector;

    private final BeanManager beanManager;

    CdiInjector(Injector parent, BeanManager beanManager) {
        this.injector = parent;
        this.beanManager = beanManager;
    }

    @Override
    public <T> T newInstance(Class<T> type) {
        T bean = BeanManagerUtil.getContextualReference(beanManager, type, true);
        if (bean != null)
            return type.cast(bean);

        return injector.newInstance(type);
    }

    @Override
    public <T> T newInstance(Class<T> type, Object instance) {
        if (instance instanceof IsSingleton) {
            boolean singleton = ((IsSingleton) instance).isSingleton();
            if (singleton)
                return type.cast(instance);
        }
        return newInstance(type);
    }
}
