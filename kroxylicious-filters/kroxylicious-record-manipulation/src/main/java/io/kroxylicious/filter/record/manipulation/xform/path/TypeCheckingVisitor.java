/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.xform.path;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import io.leangen.geantyref.GenericTypeReflector;

import io.kroxylicious.filter.record.manipulation.op.BaseTypedOp;
import io.kroxylicious.kafka.common.record.internal.Record;

class TypeCheckingVisitor implements Ast.AstVisitor {
    private final List<String> errors;
    public final String propertyName;
    public final Type requiredType;
    private final SchemaType<?> rootType;
    private String expressionText;

    TypeCheckingVisitor(String propertyName,
                        Type requiredType,
                        List<String> errors) {
        this(propertyName, requiredType, errors, SchemaType.of(new KafkaRecordTypeSystem(), Record.class));
    }

    TypeCheckingVisitor(String propertyName,
                        Type requiredType,
                        List<String> errors,
                        SchemaType<?> rootType) {
        this.propertyName = propertyName;
        this.requiredType = requiredType;
        this.errors = errors;
        this.rootType = rootType;
    }

    @Override
    public void visitExpr(Ast.Expr expr) {
        this.expressionText = expr.tree().getText();
        expr.expr().accept(this);
        Type type = expr.expr().type();
        if (!GenericTypeReflector.isSuperType(requiredType, type)) {
            errors.add("Property '%s' with expression `%s`: Expression type is not assignable to %s: Expression type is %s".formatted(
                    propertyName,
                    expressionText,
                    GenericTypeReflector.getTypeName(requiredType),
                    GenericTypeReflector.getTypeName(type)));
        }
    }

    @Override
    public void visitBool(Ast.Bool literalVal) {
        literalVal.setType(Boolean.TYPE);
    }

    @Override
    public void visitStr(Ast.Str literalVal) {
        literalVal.setType(String.class);
    }

    @Override
    public void visitNum(Ast.Num literalVal) {
        literalVal.setType(Double.TYPE);
    }

    @Override
    public void visitInt(Ast.Int literalVal) {
        literalVal.setType(Long.TYPE);
    }

    @Override
    public void visitNull(Ast.Null literalVal) {
        literalVal.setType(Void.class);
    }

    @Override
    public void visitFunctionExpr(Ast.FnCall ctx) {
        // look up the function
        String functionName = ctx.fnName();
        BaseTypedOp<?, ?> op = ctx.decl();
        // type it
        ctx.args().forEach(arg -> arg.accept(this));
        Type argType = ctx.args().getFirst().type();
        Type paramType = op.inputType();
        // check that argType is a subtype of paramType
        checkAssignable(paramType, argType, functionName);
        Type type = op.outputType();
        ctx.setType(type);
    }

    @Override
    public void visitBinaryOp(Ast.BinaryOp op) {
        // The jsonpath semantics are defined https://www.rfc-editor.org/info/rfc9535/#name-comparisons
        // and are constructed so that evaluation doesn't need to throw if the types are not compatible
        // which avoids error handling, but it's possible for x < y and y < x to both be false
        op.left().accept(this);
        op.right().accept(this);
        op.setType(Boolean.TYPE);
    }

    @Override
    public void visitDisjunction(Ast.Disjunction disjunction) {
        disjunction.disjunctees().forEach(d -> d.accept(this));
        disjunction.setType(disjunction.disjunctees().getLast().type());
    }

    @Override
    public void visitConjunction(Ast.Conjunction conjunction) {
        conjunction.conjunctees().forEach(d -> d.accept(this));
        conjunction.setType(conjunction.conjunctees().getLast().type());
    }

    @Override
    public void visitNot(Ast.Not not) {
        Ast.AstVisitor.super.visitNot(not);
    }

    @Override
    public <N> void visitPath(Ast.Path<N> path) {
        path.setType(typeOfPath(path, rootType));
    }

    /**
     * The {@link Type} which evaluating {@code path} would yield, starting from the given {@code root} type context.
     * "Bolted-on" no longer: this replaces the former standalone {@code TypeEvaluator} and drives the traversal via
     * {@link SchemaType}, so it works with any {@link TypeSystem} (the root the visitor threads, or a JSON/Avro schema
     * supplied directly).
     */
    static Type typeOfPath(Ast.Path<?> path, SchemaType<?> root) {
        return typeFrom(path.segments(), 0, root);
    }

    private static <S> Type typeFrom(List<? extends Ast.Segment<?>> segments, int index, SchemaType<S> current) {
        if (index >= segments.size()) {
            return current.javaType();
        }
        Ast.Segment<?> segment = segments.get(index);
        if (segment instanceof Ast.Segment.Child<?> child) {
            List<Type> types = new ArrayList<>();
            for (Ast.Selector<?> selector : child.selectors()) {
                collectSelectorTypes(types, segments, index + 1, selector, current);
            }
            return Union.of(types);
        }
        else if (segment instanceof Ast.Segment.Descendant<?>) {
            // In theory, we could explore the types reachable from the current type
            // and compute the union. In practice, it seems likely that this would often result in the top type
            // anyway, so let's return that always, for now at least.
            return current.topType();
        }
        else {
            throw new IllegalStateException("Unexpected segment type: " + segment);
        }
    }

    private static <S> void collectSelectorTypes(List<Type> types,
                                                 List<? extends Ast.Segment<?>> segments,
                                                 int nextIndex,
                                                 Ast.Selector<?> selector,
                                                 SchemaType<S> current) {
        if (selector instanceof Ast.Selector.Name<?> nameSelector) {
            if (current.isObject()) {
                types.add(typeFrom(segments, nextIndex, current.property(nameSelector.name())));
            }
            // TODO union type which admits object
        }
        else if (selector instanceof Ast.Selector.Index<?> indexed) {
            if (current.isArray()) {
                types.add(typeFrom(segments, nextIndex, current.item(indexed.index())));
            }
            // TODO union type which admits array
        }
        else if (selector instanceof Ast.Selector.Slice<?> sliced) {
            if (current.isArray()) {
                // TODO compute the union type over the slice's indexes
                types.add(typeFrom(segments, nextIndex, current.item(sliced.start())));
            }
            // TODO union type which admits array
        }
        else if (selector instanceof Ast.Selector.Children<?>) {
            if (current.isObject()) {
                for (String property : current.propertyNames()) {
                    types.add(typeFrom(segments, nextIndex, current.property(property)));
                }
                if (current.isObjectOpen()) {
                    types.add(typeFrom(segments, nextIndex, current.anyProperty()));
                }
            }
            else if (current.isArray()) {
                for (int itemIndex : current.itemIndexes()) {
                    types.add(typeFrom(segments, nextIndex, current.item(itemIndex)));
                }
                if (current.isArrayOpen()) {
                    types.add(typeFrom(segments, nextIndex, current.anyItem()));
                }
            }
            // TODO union type which admits object/array
        }
        // TODO Ast.Selector.FilterFilter: assume the predicate matches; we also need to type check the predicate!
    }

    private void checkAssignable(Type paramType,
                                 Type argType,
                                 String functionName) {
        if (!GenericTypeReflector.isSuperType(paramType, argType)) {
            addTypeError(functionName, paramType, argType);
        }
    }

    private void addTypeError(String functionName, Type paramType, Type argType) {
        errors.add("Property '%s' with expression `%s`: Function %s should take a %s, but is being called with a value of type %s".formatted(
                propertyName,
                expressionText,
                functionName,
                GenericTypeReflector.getTypeName(paramType),
                GenericTypeReflector.getTypeName(argType)));
    }
}
