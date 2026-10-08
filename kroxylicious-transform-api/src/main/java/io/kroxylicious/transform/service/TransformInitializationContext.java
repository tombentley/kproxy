/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.transform.service;

/**
 * A context for transformations to obtain references to services and create plugins.
 * The caller of {@link #serviceClient(Class, String)} is simply using a service
 * which is owned by something else.
 * In contrast, the caller of {@link #pluginInstance(Class, String)}
 * is responsible for managing the lifecycle of the returned plugin.
 * For example if the plugin has a close method then it's the caller's job to arrange that it's called
 * at the appropriate time.
 */
public interface TransformInitializationContext {

    /**
     * Gets a plugin instance for the given plugin type and name.
     * @param pluginClass the plugin type
     * @param implementationName the plugin implementation name
     * @return the plugin instance
     * @param <P> the plugin type
     */
    <P> P pluginInstance(Class<P> pluginClass, String implementationName);

    /**
     * Gets a service instance for the given type and name.
     * @param serviceClass the plugin type
     * @param instanceName the name of the service instance
     * @return the service instance
     * @param <S> the plugin type
     */
    <S> S serviceClient(Class<S> serviceClass, String instanceName);
}
