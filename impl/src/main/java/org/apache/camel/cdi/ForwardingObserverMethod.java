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

import javax.enterprise.event.Reception;
import javax.enterprise.event.TransactionPhase;
import javax.enterprise.inject.spi.ObserverMethod;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/* package-private */ final class ForwardingObserverMethod<T> implements ObserverMethod<T> {

    // Should be replaced with the Java 8 functional interface Consumer<T>
    private final AtomicReference<CdiEventEndpoint<T>> observer = new AtomicReference<>();

    private final Type type;

    private final Set<Annotation> qualifiers;

    ForwardingObserverMethod(Type type, Set<Annotation> qualifiers) {
        this.type = type;
        this.qualifiers = qualifiers;
    }

    void setObserver(CdiEventEndpoint<T> observer) {
        this.observer.set(observer);
    }

    @Override
    public Class<?> getBeanClass() {
        return CamelContext.class;
    }

    @Override
    public Type getObservedType() {
        return type;
    }

    @Override
    public Set<Annotation> getObservedQualifiers() {
        return qualifiers;
    }

    @Override
    public Reception getReception() {
        return Reception.ALWAYS;
    }

    @Override
    public TransactionPhase getTransactionPhase() {
        return TransactionPhase.IN_PROGRESS;
    }

    @Override
    public void notify(T event) {
        if (observer.get() != null)
            observer.get().notify(event);
    }
}
