/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.encryption;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionStage;

import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * KEK selection based on topic name
 * @param <K> the type of key
 */
public abstract class TopicNameBasedKekSelector<K> {

    /**
     * Returns a completion stage whose value, on successful completion, is a map from each of the given topic
     * names to the KEK id to use for encrypting records in that topic, or a null KEK if that topic should not be encrypted.
     * @param topicNames A set of topic names
     * @return A completion stage for the map form topic name to KEK id.
     */
    public abstract @NonNull Map<String, CompletionStage<Optional<K>>> selectKek(@NonNull Set<String> topicNames);
}
