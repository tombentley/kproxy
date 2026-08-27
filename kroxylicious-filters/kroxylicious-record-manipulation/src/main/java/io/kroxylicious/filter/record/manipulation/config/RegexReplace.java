/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.config;

import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.kroxylicious.filter.record.manipulation.common.OpFactory;
import io.kroxylicious.filter.record.manipulation.common.RegexReplaceStringFunction;
import io.kroxylicious.filter.record.manipulation.common.TypedOp;
import io.kroxylicious.proxy.plugin.Plugin;

@Plugin(configType = RegexReplace.Config.class)
public class RegexReplace implements OpFactory<String, String> {

    /**
     * Configuration for {@link RegexReplace}.
     */
    public record Config(String pattern,
                         RegexReplaceStringFunction.Replacement replacement) {}

    @Override
    public TypedOp<String, String> create(Map<String, Object> configMap) {
        Config config = OpConfigs.MAPPER.convertValue(configMap, Config.class);
        return TypedOp.of(String.class, new RegexReplaceStringFunction(config.pattern(), config.replacement()));
    }
}
