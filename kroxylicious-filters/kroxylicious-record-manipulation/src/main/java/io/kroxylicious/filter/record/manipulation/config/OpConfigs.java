/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.config;

import io.kroxylicious.filter.record.manipulation.common.BooleanOp;
import io.kroxylicious.filter.record.manipulation.common.BooleanOpFactory;
import io.kroxylicious.filter.record.manipulation.common.BytesOp;
import io.kroxylicious.filter.record.manipulation.common.BytesOpFactory;
import io.kroxylicious.filter.record.manipulation.common.DoubleOp;
import io.kroxylicious.filter.record.manipulation.common.DoubleOpFactory;
import io.kroxylicious.filter.record.manipulation.common.FloatOp;
import io.kroxylicious.filter.record.manipulation.common.FloatOpFactory;
import io.kroxylicious.filter.record.manipulation.common.IntOp;
import io.kroxylicious.filter.record.manipulation.common.IntOpFactory;
import io.kroxylicious.filter.record.manipulation.common.LongOp;
import io.kroxylicious.filter.record.manipulation.common.LongOpFactory;
import io.kroxylicious.filter.record.manipulation.common.PluginLookup;
import io.kroxylicious.filter.record.manipulation.common.StringOp;
import io.kroxylicious.filter.record.manipulation.common.StringOpFactory;

/**
 * Resolves an {@link OpConfig} to a built operation via {@link PluginLookup} - the part of building an
 * {@code apply} chain that's identical across every format engine (Jackson/Avro/Protobuf), so it lives
 * here once rather than being duplicated in each engine's own {@code buildStringOp}/{@code buildIntegerOp}.
 * <p>
 * Deliberately does not know about {@link #DELETE}: whether that name is legal at all is a property of the
 * calling format's container model (can it represent "this property is absent"?), not something a shared,
 * name-keyed plugin registry can answer - see each engine's own {@code buildStringOp}/{@code
 * buildIntegerOp} for how they handle it before ever calling here.
 */
public final class OpConfigs {

    /**
     * The reserved, non-pluggable op name every format engine special-cases itself, rather than resolving
     * through {@link PluginLookup} - see the class javadoc for why.
     */
    public static final String DELETE = "Delete";

    private OpConfigs() {
    }

    /**
     * Resolves {@code op} to a {@link StringOpFactory} and builds its operation.
     * @param op the operation to resolve; must not be {@link #DELETE}
     * @param lookup the plugin lookup to resolve {@code op}'s name against
     * @return the built operation
     */
    public static StringOp resolveStringOp(OpConfig op, PluginLookup lookup) {
        StringOpFactory factory = lookup.pluginInstance(StringOpFactory.class, op.op());
        return factory.create(op.config());
    }

    /**
     * Resolves {@code op} to a {@link BytesOpFactory} and builds its operation.
     * @param op the operation to resolve; must not be {@link #DELETE}
     * @param lookup the plugin lookup to resolve {@code op}'s name against
     * @return the built operation
     */
    public static BytesOp resolveBytesOp(OpConfig op, PluginLookup lookup) {
        BytesOpFactory factory = lookup.pluginInstance(BytesOpFactory.class, op.op());
        return factory.create(op.config());
    }

    /**
     * Resolves {@code op} to an {@link BooleanOpFactory} and builds its operation.
     * @param op the operation to resolve; must not be {@link #DELETE}
     * @param lookup the plugin lookup to resolve {@code op}'s name against
     * @return the built operation
     */
    public static BooleanOp resolveBooleanOp(OpConfig op, PluginLookup lookup) {
        BooleanOpFactory factory = lookup.pluginInstance(BooleanOpFactory.class, op.op());
        return factory.create(op.config());
    }

    /**
     * Resolves {@code op} to an {@link IntOpFactory} and builds its operation.
     * @param op the operation to resolve; must not be {@link #DELETE}
     * @param lookup the plugin lookup to resolve {@code op}'s name against
     * @return the built operation
     */
    public static IntOp resolveIntOp(OpConfig op, PluginLookup lookup) {
        IntOpFactory factory = lookup.pluginInstance(IntOpFactory.class, op.op());
        return factory.create(op.config());
    }

    /**
     * Resolves {@code op} to an {@link LongOpFactory} and builds its operation.
     * @param op the operation to resolve; must not be {@link #DELETE}
     * @param lookup the plugin lookup to resolve {@code op}'s name against
     * @return the built operation
     */
    public static LongOp resolveLongOp(OpConfig op, PluginLookup lookup) {
        LongOpFactory factory = lookup.pluginInstance(LongOpFactory.class, op.op());
        return factory.create(op.config());
    }

    /**
     * Resolves {@code op} to an {@link DoubleOpFactory} and builds its operation.
     * @param op the operation to resolve; must not be {@link #DELETE}
     * @param lookup the plugin lookup to resolve {@code op}'s name against
     * @return the built operation
     */
    public static FloatOp resolveFloatOp(OpConfig op, PluginLookup lookup) {
        FloatOpFactory factory = lookup.pluginInstance(FloatOpFactory.class, op.op());
        return factory.create(op.config());
    }

    /**
     * Resolves {@code op} to an {@link DoubleOpFactory} and builds its operation.
     * @param op the operation to resolve; must not be {@link #DELETE}
     * @param lookup the plugin lookup to resolve {@code op}'s name against
     * @return the built operation
     */
    public static DoubleOp resolveDoubleOp(OpConfig op, PluginLookup lookup) {
        DoubleOpFactory factory = lookup.pluginInstance(DoubleOpFactory.class, op.op());
        return factory.create(op.config());
    }
}
