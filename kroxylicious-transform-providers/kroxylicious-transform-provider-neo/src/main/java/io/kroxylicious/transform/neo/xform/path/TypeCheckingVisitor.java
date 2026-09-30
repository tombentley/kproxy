/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.transform.neo.xform.path;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import io.leangen.geantyref.GenericTypeReflector;

import io.kroxylicious.kafka.common.record.internal.Record;
import io.kroxylicious.transform.neo.op.BaseTypedOp;
import io.kroxylicious.transform.neo.type.Null;
import io.kroxylicious.transform.neo.type.SchemaType;
import io.kroxylicious.transform.neo.type.Union;

import edu.umd.cs.findbugs.annotations.Nullable;

class TypeCheckingVisitor implements Ast.AstVisitor {
    private final List<String> errors;
    public final String propertyName;
    public final Type requiredType;
    private final SchemaType<?> rootType;
    /**
     * The type system currently in effect, threaded up the pipeline: it starts at {@link #rootType} and is
     * switched by any operation that {@linkplain BaseTypedOp#introducedSchema() introduces} a new one (a
     * deserializer/validator). Paths are typed against this context.
     */
    private SchemaType<?> activeType;
    /**
     * The type context bound to {@code @} while type-checking a filter body (the candidate element being
     * filtered); {@code null} outside a filter. {@code $}-rooted paths continue to use {@link #activeType}.
     */
    private @Nullable SchemaType<?> currentNodeType;
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
        this.activeType = rootType;
    }

    /** The type system in effect after the last-visited expression; exposed for tests. */
    SchemaType<?> activeType() {
        return activeType;
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
        // The `null` literal is the present null value (RFC 9535 §2.3.5.2), not absence: its type is the
        // unit Null type, distinct from Nothing.
        literalVal.setType(Null.class);
    }

    @Override
    public void visitFunctionExpr(Ast.FnCall ctx) {
        // look up the function
        String functionName = ctx.fnName();
        BaseTypedOp<?, ?> op = ctx.decl();
        // type it (args are visited first, so an inner deserializer switches activeType before we get here)
        ctx.args().forEach(arg -> arg.accept(this));
        Type argType = ctx.args().getFirst().type();
        Type paramType = op.inputType();
        // check that argType is a subtype of paramType
        checkAssignable(paramType, argType, functionName);
        Type declared = op.outputType();
        Type nodeType = declared;
        // If this op reinterprets its input under a new type system, switch the active context and refine
        // the node type to the withKeySchema's logical type when that is a subtype of the declared output (e.g. a
        // JSON withKeySchema narrows JsonNode to ObjectNode); wrapper outputs like AvroValue keep their declared type.
        Optional<SchemaType<?>> introduced = op.introducedSchema();
        if (introduced.isPresent()) {
            activeType = introduced.get();
            Type logical = activeType.javaType();
            if (logical != null && GenericTypeReflector.isSuperType(declared, logical)) {
                nodeType = logical;
            }
        }
        ctx.setType(nodeType);
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
        // `$` types against the threaded active context; `@` (only ever inside a filter body) against the candidate element.
        SchemaType<?> base = path.identifier() == Identifier.CURRENT && currentNodeType != null ? currentNodeType : activeType;
        path.setType(typeFrom(path.segments(), 0, base));
    }

    /**
     * The {@link Type} which evaluating {@code path} would yield, starting from the given {@code root} type context.
     * "Bolted-on" no longer: this replaces the former standalone {@code TypeEvaluator} and drives the traversal via
     * {@link SchemaType}, so it works with any {@link TypeSystem} (the root the visitor threads, or a JSON/Avro withKeySchema
     * supplied directly). The traversal is now instance-based (it type-checks filter bodies against the visitor's
     * error list and root), so this static entry point drives it through a throwaway visitor.
     */
    static Type typeOfPath(Ast.Path<?> path, SchemaType<?> root) {
        return new TypeCheckingVisitor("", Object.class, new ArrayList<>()).typeFrom(path.segments(), 0, root);
    }

    private <S> Type typeFrom(List<? extends Ast.Segment<?>> segments, int index, SchemaType<S> current) {
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

    private <S> void collectSelectorTypes(List<Type> types,
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
            collectChildrenTypes(types, segments, nextIndex, current);
        }
        else if (selector instanceof Ast.Selector.Filter<?> filter) {
            // A filter selects a subset of the same members/items, so its type contribution is that of a wildcard...
            collectChildrenTypes(types, segments, nextIndex, current);
            // ...and additionally the predicate is type-checked with `@` bound to the candidate element type.
            typeCheckFilter(filter, current);
        }
    }

    /** The types reachable by selecting every member value (objects) or item (arrays) of {@code current}. */
    private <S> void collectChildrenTypes(List<Type> types,
                                          List<? extends Ast.Segment<?>> segments,
                                          int nextIndex,
                                          SchemaType<S> current) {
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

    /**
     * Type-checks a filter's predicate against the element being filtered: {@code @} is bound to that element's
     * type for the duration, while {@code $} continues to resolve against the record root. The grammar already
     * constrains a filter body to a logical shape (existence path, comparison, conjunction or function), so no
     * "must be boolean" check is imposed here.
     */
    private <S> void typeCheckFilter(Ast.Selector.Filter<?> filter, SchemaType<S> current) {
        // The candidate element: an array's item type, an object's member value, else the value itself.
        // TODO item(0) represents a homogeneous array's element; heterogeneous tuples and union/scalar contexts
        // type `@` navigation only approximately.
        SchemaType<S> element = current.isArray() ? current.item(0)
                : current.isObject() ? current.anyProperty()
                        : current;
        SchemaType<?> previous = currentNodeType;
        currentNodeType = element;
        try {
            filter.expression().accept(this);
        }
        finally {
            currentNodeType = previous;
        }
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
