/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.bootstrap;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

/**
 * A collection of plugs to be used together.
 */
public class Plugins {

    private final List<Plugin> plugins;

    Plugins(List<Plugin> plugins) {
        Objects.requireNonNull(plugins);
        this.plugins = plugins;
    }

    /** Create a schema from a classpath resource */
    public ConfigSchema schemaFromClasspathResource(String resource) {
        try {
            try (InputStream resourceAsStream = Plugins.class.getResourceAsStream(resource)) {
                // Parse and validate the schema is Kube compatible
                return ConfigSchema.create(resourceAsStream);
            }
        }
        catch (IOException closeException) {
            throw new UncheckedIOException(closeException);
        }
    }

    /**
     * Create a schema the chooses between
     * all the available implementations of the given plugin
     */
    public ConfigSchema schemaForChoice(Class<? extends Plugin> plugin) {
        return ConfigSchema.choice(this, plugin);
    }

    /** Create a schema from a string of JSON Schema */
    public ConfigSchema schemaFromString(String schemaAsString) {
        try {
            return ConfigSchema.create(schemaAsString);
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public ConfigSchema.Builder schemaBuilderFromString(String schemaAsString) {
        return ConfigSchema.builder(schemaAsString);
    }

    /**
     * @param plugin A plugin
     * @return the id of the given
     */
    public String choiceId(Plugin plugin) {
        // TODO is this returning a plugin id, or some name/id of the plugin's type??
        // Note the concept of plugin id implies some scope of uniqueness
        // With the hierarchical view given by the YAML, the id can be a path
        // if the scope of uniqueness of the id is not the whole tree
        return plugin.getClass().getSimpleName();
    }

    public <T extends Plugin> T configureChosen(
            Config config,
            List<String> pathToChoice,
            Class<T> pluginClass) {
        JsonNode choiceNode = config.path(pathToChoice).toJsonNode();
        if (choiceNode.isMissingNode()) {
            return null;
        }
        Iterator<String> iterator = choiceNode.fieldNames();
        if (!iterator.hasNext()) {
            throw new IllegalStateException();
        }
        String pluginId = iterator.next();
        if (iterator.hasNext()) {
            throw new IllegalStateException();
        }
        for (Plugin plugin : plugins) {
            if (pluginId.equals(choiceId(plugin))) {
                JsonNode pluginConfig = choiceNode.get(pluginId);
                plugin.configure(this, new Config(pluginConfig));
                return pluginClass.cast(plugin);
            }
        }
        return null;
    }

    /**
     * Return all known plugins that implement the given service.
     * @param pluginPoint
     * @return
     * @param <T>
     */
    public <T extends Plugin> List<T> implementationsOf(Class<T> pluginPoint) {
        List<T> result = new ArrayList<>();
        for (Plugin plugin : plugins) {
            if (pluginPoint.isInstance(plugin)) {
                result.add(pluginPoint.cast(plugin));
            }
        }
        return result;
    }
}
