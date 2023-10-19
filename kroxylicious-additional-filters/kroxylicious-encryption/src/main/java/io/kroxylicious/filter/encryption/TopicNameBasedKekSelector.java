/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.encryption;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletionStage;

public abstract non-sealed class TopicNameBasedKekSelector<Kr> implements KekSelector {

    public abstract CompletionStage<Map<String, Kr>> selectKek(Set<String> topicNames);
}
