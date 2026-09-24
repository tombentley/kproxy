/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.kafka;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import io.kroxylicious.filter.record.manipulation.op.BaseTypedOp;
import io.kroxylicious.filter.record.manipulation.op.OpConfig;
import io.kroxylicious.filter.record.manipulation.op.PluginLookup;
import io.kroxylicious.filter.record.manipulation.xform.path.Ast;
import io.kroxylicious.kafka.common.header.Header;

public class Check {

    private final PluginLookup pluginLookup;

    public Check(PluginLookup pluginLookup) {
        this.pluginLookup = Objects.requireNonNull(pluginLookup);
    }

    void check(RecordTransformConfig2 config) {
        Map<String, OpConfig> decls = config.where();

        // Build ASTs from the parse trees
        var keyTree = config.key().tree().accept(new AstBuilder());
        var valueTree = config.value().tree().accept(new AstBuilder());
        var timestampTree = config.timestamp().tree().accept(new AstBuilder());
        var headersTree = config.headers().tree().accept(new AstBuilder());
        // Resolve the function calls in the expressions to their declarations via the `where` clause
        var ops = resolve(decls, keyTree, valueTree, timestampTree, headersTree);
        // Type check the expressions
        typeCheck(keyTree, ops, valueTree, timestampTree, headersTree);
    }

    HashMap<String, BaseTypedOp<?, ?>> resolve(Map<String, OpConfig> decls,
                                               Ast keyTree,
                                               Ast valueTree,
                                               Ast timestampTree,
                                               Ast headersTree) {
        var ops = new HashMap<String, BaseTypedOp<?, ?>>();
        List<String> errors = new ArrayList<>();
        ResolverVisitor visitor = new ResolverVisitor(pluginLookup, decls, ops, errors);
        visitor.resolve("key", keyTree);
        visitor.resolve("value", valueTree);
        visitor.resolve("timestamp", timestampTree);
        visitor.resolve("headers", headersTree);
        if (!errors.isEmpty()) {
            throw new CheckException(String.join("\n", errors) + "\n(Hint: functions are defined using the 'where' property)");
        }
        return ops;
    }

    private void typeCheck(Ast keyTree, Map<String, BaseTypedOp<?, ?>> ops, Ast valueTree, Ast timestampTree, Ast headersTree) {
        List<String> errors = new ArrayList<>();
        keyTree.accept(new TypeCheckingVisitor("key", ByteBuffer.class, ops, errors));
        valueTree.accept(new TypeCheckingVisitor("value", ByteBuffer.class, ops, errors));
        timestampTree.accept(new TypeCheckingVisitor("timestamp", Long.TYPE, ops, errors));
        headersTree.accept(new TypeCheckingVisitor("headers", Header[].class, ops, errors));
        if (!errors.isEmpty()) {
            throw new CheckException(String.join("\n", errors));
        }
    }

}
