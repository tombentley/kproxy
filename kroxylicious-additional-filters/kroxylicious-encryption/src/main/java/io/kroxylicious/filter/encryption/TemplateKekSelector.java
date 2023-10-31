/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.encryption;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import io.kroxylicious.kms.service.Kms;
import io.kroxylicious.kms.service.UnknownAliasException;

import edu.umd.cs.findbugs.annotations.NonNull;

public class TemplateKekSelector<K> extends TopicNameBasedKekSelector<K> {

    public static final Pattern PATTERN = Pattern.compile("\\$\\{(.*?)}");
    private final String template;
    private final Kms<K, ?> kms;

    public TemplateKekSelector(@NonNull Kms<K, ?> kms, @NonNull String template) {
        var matcher = PATTERN.matcher(Objects.requireNonNull(template));
        while (matcher.find()) {
            switch (matcher.group(1)) {
                case "topicName":
                    continue;
                default:
                    throw new IllegalArgumentException("Unknown template parameter: " + matcher.group(1));
            }
        }
        this.template = Objects.requireNonNull(template);
        this.kms = Objects.requireNonNull(kms);
    }

    static record Pair<K>(String topicName, K key) {}

    @NonNull
    @Override
    public CompletionStage<Map<String, K>> selectKek(@NonNull Set<String> topicNames) {
        var collect = topicNames.stream().collect(Collectors.toMap(topicName -> topicName,
                topicName -> kms.resolveAlias(evaluateTemplate(topicName)).exceptionally(e -> {
                    if (e instanceof UnknownAliasException) {
                        return null;
                    }
                    else if (e instanceof RuntimeException re) {
                        throw re;
                    }
                    else {
                        throw new RuntimeException(e);
                    }
                }).thenApply(x -> new Pair<>(topicName, x)).toCompletableFuture()));
        var futures = new ArrayList<>(collect.values());
        var joined = EnvelopeEncryptionFilter.join(futures);
        return joined.thenApply(list -> {
            // Note we ca use java.util.stream for to collection is map, because it has null values
            HashMap<String, K> map = new HashMap<>();
            for (Pair<K> pair : list) {
                map.put(pair.topicName(), pair.key());
            }
            return map;
        });
    }

    String evaluateTemplate(String topicName) {
        var matcher = PATTERN.matcher(template);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String replacement;
            switch (matcher.group(1)) {
                case "topicName":
                    replacement = topicName;
                    break;
                default:
                    // this should be impossible because of the check in the constructor
                    throw new IllegalStateException();
            }
            matcher.appendReplacement(sb, replacement);
        }
        matcher.appendTail(sb);
        return sb.toString();
    }
}
