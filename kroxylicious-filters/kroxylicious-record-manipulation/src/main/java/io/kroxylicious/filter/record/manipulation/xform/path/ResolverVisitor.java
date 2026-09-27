/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.xform.path;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.List;
import java.util.Map;

import io.kroxylicious.filter.record.manipulation.op.BaseTypedOp;
import io.kroxylicious.filter.record.manipulation.op.OpConfig;
import io.kroxylicious.filter.record.manipulation.op.OpFactory;
import io.kroxylicious.filter.record.manipulation.op.PluginLookup;

import edu.umd.cs.findbugs.annotations.Nullable;

class ResolverVisitor implements Ast.AstVisitor {
    private final List<String> errors;
    private final @Nullable Map<String, OpConfig> decls;
    private final PluginLookup pluginLookup;
    String propertyName;
    private String expressionText;

    ResolverVisitor(
            PluginLookup pluginLookup,
            @Nullable Map<String, OpConfig> decls,
            List<String> errors) {
        this.pluginLookup = pluginLookup;
        this.decls = decls;
        this.errors = errors;
    }

    void resolve(String propsertyName, Ast tree) {
        this.propertyName = propsertyName;
        this.expressionText = tree.tree().getText();
        tree.accept(this);
    }

    @Override
    public <N> void visitPath(Ast.Path<N> nPath) {
        Ast.AstVisitor.super.visitPath(nPath);
    }

    @Override
    public void visitFunctionExpr(Ast.FnCall fnExpr) {
        Ast.AstVisitor.super.visitFunctionExpr(fnExpr);
        var functionName = fnExpr.fnName();
        if (decls == null || !decls.containsKey(functionName)) {
            errors.add(String.format("Property '%s' has expression `%s` which references an undefined function '%s'",
                    propertyName, expressionText, functionName));
        }
        else {
            try {
                // TODO functions should probably be instantiated for each call site
                // because their type attribution could vary by call site.
                OpFactory<?, ?> opFactory = pluginLookup.pluginInstance(OpFactory.class, decls.get(functionName).op());
                var op = opFactory.create(decls.get(functionName).config(), pluginLookup, null);
                fnExpr.setDecl(op);
            }
            catch (Exception e) {
                errors.add(String.format("Could not instantiate function '%s' referred to in expression for property '%s': %s", functionName, propertyName,
                        e.getMessage()));
            }
        }
    }
}
