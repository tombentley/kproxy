/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.transform.neo.ops.common;

import java.lang.reflect.Type;
import java.util.Map;

import io.kroxylicious.proxy.plugin.Plugin;
import io.kroxylicious.transform.neo.op.BaseTypedOp;
import io.kroxylicious.transform.neo.op.OpFactory;
import io.kroxylicious.transform.service.PluginLookup;

@Plugin(configType = RegexReplace.Config.class)
public class RegexReplace implements OpFactory<String, String> {

    /**
     * Configuration for {@link RegexReplace}.
     */
    public record Config(String pattern,
                         RegexReplaceStringFunction.Replacement replacement) {}

    @Override
    public BaseTypedOp<String, String> create(Map<String, Object> configMap, PluginLookup lookup, Type argumentType) {
        Config config = Mapper.OP_CONFIG_MAPPER.convertValue(configMap, Config.class);
        RegexReplaceStringFunction regexReplaceStringFunction = new RegexReplaceStringFunction(config.pattern(), config.replacement());
        return BaseTypedOp.of(String.class, String.class, regexReplaceStringFunction);
    }
}
