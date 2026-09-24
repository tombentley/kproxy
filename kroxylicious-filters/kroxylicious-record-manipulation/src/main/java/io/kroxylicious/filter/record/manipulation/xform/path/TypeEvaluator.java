/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.xform.path;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Return the type which the evaluation of path against an instance value would have assuming it conforms to the given schema.
 * @param <N> The Java type of the data tree nodes (e.g. {@code JsonNode} for a Jackson JSON tree).
 * For some libraries there is no specific type than {@link Object}.
 * @param <S> The type of schema object.
 */
public class TypeEvaluator<N, S> {

    public Type type(Ast.Path<N> path, S schema, TypeSystem<S> typeSystem) {
        return typeRecursive(path.segments().iterator(), schema, typeSystem);
    }

    private Type typeRecursive(Iterator<Ast.Segment<N>> iterator, S schema, TypeSystem<S> typeSystem) {
        if (!iterator.hasNext()) {
            return typeSystem.typeOf(schema);
        }
        var segment = iterator.next();

        if (segment instanceof Ast.Segment.Child child) {
            List<Type> types = new ArrayList<>();
            for (var selector : child.selectors()) {
                if (selector instanceof Ast.Selector.Name nameSelector) {
                    // case Ast.Selector.NameSelector(var name) -> {
                    if (typeSystem.isObjectType(schema)) {
                        Type type = typeRecursive(iterator, typeSystem.objectPropertySchema(schema, nameSelector.name()), typeSystem);
                        types.add(type);
                    }
                    // TODO union type which admits object
                    // }
                }
                if (selector instanceof Ast.Selector.Index indexed) {
                    // case Ast.Selector.IndexSelector(var index) -> {
                    if (typeSystem.isArrayType(schema)) {
                        types.add(typeRecursive(iterator, typeSystem.arrayItemSchema(schema, indexed.index()), typeSystem));
                    }
                    // TODO union type which admits array
                    // }
                }
                if (selector instanceof Ast.Selector.Slice sliced) {
                    // case Ast.Selector.SliceSelector(var start, var end, var step) -> {
                    if (typeSystem.isArrayType(schema)) {
                        // TODO compute the union type over the slice's indexes
                        types.add(typeRecursive(iterator, typeSystem.arrayItemSchema(schema, sliced.start()), typeSystem));
                    }
                    // TODO union type which admits array
                    // }
                }
                if (selector instanceof Ast.Selector.Children children) {
                    // case Ast.Selector.ChildrenSelector() -> {
                    if (typeSystem.isObjectType(schema)) {
                        for (var property : typeSystem.objectProperties(schema)) {
                            Type type = typeRecursive(iterator, typeSystem.objectPropertySchema(schema, property), typeSystem);
                            types.add(type);
                        }
                        if (typeSystem.isObjectOpen(schema)) {
                            Type type = typeRecursive(iterator, typeSystem.objectPropertySchema(schema), typeSystem);
                            types.add(type);
                        }
                    }
                    else if (typeSystem.isArrayType(schema)) {
                        for (var index : typeSystem.arrayIndexes(schema)) {
                            Type type = typeRecursive(iterator, typeSystem.arrayItemSchema(schema, index), typeSystem);
                            types.add(type);
                        }
                        if (typeSystem.isArrayOpen(schema)) {
                            Type type = typeRecursive(iterator, typeSystem.arrayItemSchema(schema), typeSystem);
                            types.add(type);
                        }
                    }
                    // TODO union type which admits object/array
                    // }
                }
                if (selector instanceof Ast.Selector.Children children) {
                    // case Ast.Selector.FilterSelector(var predicate) -> {
                    // yield null; // TODO assume the predicate will match
                    // TODO we also need to type check the predicate!
                    // }
                } // switch(selector)

            } // for
            return Union.of(types);
        } // child segment case
        else if (segment instanceof Ast.Segment.Descendant d) {
            // In theory, we could explore the types reachable from the root type
            // and compute the union. In practice, it seems likely that this would often result in the top type
            // anyway, so let's return that always, for now at least.
            return typeSystem.topType();
        }
        else {
            throw new IllegalStateException("Unexpected segment type: " + segment);
        }
    }
}
