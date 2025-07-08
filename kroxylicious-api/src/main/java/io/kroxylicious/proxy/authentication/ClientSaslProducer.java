/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.proxy.authentication;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.kroxylicious.proxy.filter.FilterContext;
import io.kroxylicious.proxy.filter.FilterFactory;

/**
 * <p>Annotation to be applied to {@link FilterFactory FilterFactories} that
 * create filters that make use of {@link FilterContext#clientSaslAuthenticationSuccess(SaslPrincipal)}.
 *
 * <p>Using this annotation allows the runtime to fail at startup if a filter
 * requires authentication information that cannot be provided by a filter chain
 * or virtual cluster in which it is used.</p>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ClientSaslProducer {
}
