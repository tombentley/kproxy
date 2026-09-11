/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.config;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.leangen.geantyref.GenericTypeReflector;

import io.kroxylicious.filter.record.manipulation.common.ComposedOp;
import io.kroxylicious.filter.record.manipulation.common.IdentityOp;
import io.kroxylicious.filter.record.manipulation.op.PluginLookup;
import io.kroxylicious.filter.record.manipulation.common.Requirement;
import io.kroxylicious.filter.record.manipulation.op.BaseTypedOp;
import io.kroxylicious.filter.record.manipulation.op.OpConfig;
import io.kroxylicious.filter.record.manipulation.op.OpFactory;
import io.kroxylicious.filter.record.manipulation.common.OpTypeChecker;

/**
 * Resolves an {@link OpConfig} to a built operation via {@link PluginLookup} - the part of building an
 * {@code apply} chain that's identical across every format engine (Jackson/Avro/Protobuf), so it lives
 * here once rather than being duplicated in each engine's own {@code buildOp}.
 */
public final class OpConfigs {

    private OpConfigs() {
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static <T> BaseTypedOp<T, ?> compose(
                                                Class<T> inputType,
                                                List<OpConfig> opConfigs,
                                                Set<Requirement> requirements,
                                                PluginLookup lookup) {

        if (opConfigs.isEmpty()) {
            return IdentityOp.identity();
        }
        else {
            Type currentType = inputType;
            BaseTypedOp last = null;
            for (var opConfig : opConfigs) {
                OpFactory<?, ?> factory = lookup.pluginInstance(OpFactory.class, opConfig.op());
                BaseTypedOp<?, ?> op = factory.create(opConfig.config(), lookup, currentType);
                OpTypeChecker.requireAssignable(op.inputType(), currentType, "Op " + opConfig.op() + " has input type " + GenericTypeReflector.getTypeName(op.inputType())
                        + " which is not a subtype of the expected type " + GenericTypeReflector.getTypeName(currentType));
                currentType = op.outputType();
                if (last == null) {
                    last = op;
                }
                else {
                    last = new ComposedOp<>(last, op);
                }
            }
            return last;
        }
    }
}
