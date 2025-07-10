/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.proxy.authentication;

import java.security.Principal;

/**
 * Annotates {@link io.kroxylicious.proxy.filter.FilterFactory FilterFactories}
 * that produce filters which use {link io.kroxylicious.proxy.filter.FilterContext#clientAuthenticationSuccess()}
 *
 * The runtime validates that there is at most one such plugin defined for a virtual cluster,
 * and that all {@link ClientPrincipalAware} filters use a
 * compatible {@link Principal} type.
 */
public @interface ClientPrincipalProducer {
    /**
     * @return The type of principal produced
     */
    Class<? extends Principal> value();
}
