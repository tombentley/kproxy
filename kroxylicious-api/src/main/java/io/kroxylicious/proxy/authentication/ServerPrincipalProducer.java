/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.proxy.authentication;

import java.security.Principal;

/**
 * Annotation applied to {@link io.kroxylicious.proxy.filter.FilterFactory FilterFactories}
 * that produce filters which use {link io.kroxylicious.proxy.filter.FilterContext#serverAuthenticationSuccess()}
 */
public @interface ServerPrincipalProducer {
    /**
     * @return The type of principal produced
     */
    Class<? extends Principal> value();
}
