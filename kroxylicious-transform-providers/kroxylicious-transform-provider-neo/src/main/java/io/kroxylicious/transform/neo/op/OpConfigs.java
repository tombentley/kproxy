/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.transform.neo.op;

import java.lang.reflect.Type;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import io.leangen.geantyref.GenericTypeReflector;

import io.kroxylicious.transform.neo.ops.common.ComposedOp;
import io.kroxylicious.transform.neo.ops.common.IdentityOp;
import io.kroxylicious.transform.neo.ops.common.OpTypeChecker;
import io.kroxylicious.transform.service.TransformInitializationContext;

/**
 * Resolves an {@link OpConfig} to a built operation via {@link TransformInitializationContext} - the part of building an
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
                                                TransformInitializationContext context) {

        if (opConfigs.isEmpty()) {
            return IdentityOp.identity();
        }
        else {
            Type currentType = inputType;
            BaseTypedOp last = null;
            for (var opConfig : opConfigs) {
                OpFactory<?, ?> factory = context.pluginInstance(OpFactory.class, opConfig.op());
                BaseTypedOp<?, ?> op = factory.create(opConfig.config(), context, currentType);
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

    public static <T, R> R applyNow(BaseTypedOp<T, R> stage, T input, OpContext context) {
        CompletionStage<R> apply = stage.apply(input, context);
        CompletableFuture<R> completableFuture = apply.toCompletableFuture();
        if (completableFuture.isDone()) {
            return completableFuture.getNow(null);
        }
        else {
            throw new IllegalStateException("stage is not done");
        }

    }
}
