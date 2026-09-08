/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.op;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.leangen.geantyref.GenericTypeReflector;

/**
 * Centralizes the type-compatibility reasoning that {@link BaseTypedOp} composition needs, so it isn't
 * duplicated (with inconsistent error messages) across every place that builds or composes ops.
 */
public final class OpTypeChecker {

    private OpTypeChecker() {
    }

    /**
     * Throws a {@link TypeException} unless {@code actual} is assignable to {@code expected}.
     * @param expected the required type
     * @param actual the type being checked against it
     * @param message the exception message to use on failure
     */
    public static void requireAssignable(Type expected, Type actual, String message) {
        if (!GenericTypeReflector.isSuperType(expected, actual)) {
            throw new TypeException(message);
        }
    }

    /**
     * Throws a {@link TypeException} unless {@code then} can be applied to {@code first}'s output, i.e.
     * {@code then.inputType()} is a supertype of {@code first.outputType()}. Only checked when both types
     * are fully bound (no unresolved {@link TypeVariable}), mirroring the historic behaviour of
     * {@code ComposedOp}'s constructor.
     * @param first the first op in the composition
     * @param then the op applied to {@code first}'s output
     */
    public static void requireComposable(BaseTypedOp<?, ?> first, BaseTypedOp<?, ?> then) {
        if (GenericTypeReflector.isFullyBound(first.outputType())
                && GenericTypeReflector.isFullyBound(then.inputType())
                && !GenericTypeReflector.isSuperType(then.inputType(), first.outputType())) {
            throw new TypeException("Cannot compose " + first + " with " + then);
        }
    }

    /**
     * Extracts the single type argument of a {@link Type} known to be a subtype of {@code rawType} (e.g.
     * {@code List<Foo>} against {@code List.class} yields {@code Foo}).
     * @param argumentType the type to inspect
     * @param rawType the raw generic type {@code argumentType} is expected to be a subtype of
     * @return the sole actual type argument
     */
    public static Type singleTypeArgumentOf(Type argumentType, Class<?> rawType) {
        Type superType = GenericTypeReflector.getExactSuperType(argumentType, rawType);
        if (!(superType instanceof ParameterizedType parameterizedType)) {
            throw new TypeException("Argument type " + GenericTypeReflector.getTypeName(argumentType) + " is not a subtype of " + rawType);
        }
        return parameterizedType.getActualTypeArguments()[0];
    }

    // Unfinished: generic type-parameter substitution for ops with non-empty typeParameters() (currently
    // only IdentityOp). Not yet wired up to any caller - moved here (out of BaseTypedOp) so the interface
    // doesn't carry half-finished default-method logic, ready to be finished if/when a composition scenario
    // actually needs it.
    static Type typeApply(BaseTypedOp<?, ?> op, List<Type> typeArguments) {
        if (typeArguments.size() != op.typeParameters().size()) {
            String prefix;
            if (typeArguments.size() < op.typeParameters().size()) {
                prefix = "Too few type arguments given: there are ";
            }
            else {
                prefix = "Too many type arguments given: there are ";
            }
            throw new IllegalArgumentException(prefix + op.typeParameters().size() + " parameters, but " + typeArguments.size() + " arguments were given");
        }

        if (op.typeParameters().isEmpty()) {
            // not generic
            if (!GenericTypeReflector.isFullyBound(op.outputType())) {
                throw new TypeException();
            }
            return op.outputType();
        }

        Map<TypeVariable<? extends Class<?>>, Type> substitutions = new HashMap<>();
        for (int i = 0; i < typeArguments.size(); i++) {
            substitutions.put(op.typeParameters().get(i), typeArguments.get(i));
        }

        return null;
        // return op.typeParameters().stream().map(typeParameter -> {
        // var typeArgument = substitutions.get(typeParameter);
        // for (var bound : typeParameter.getBounds()) {
        // if (!GenericTypeReflector.isSuperType(bound, typeArgument)) {
        // throw new TypeException("Type argument " + typeArgument + " is within bound " + bound);
        // }
        // }
        // return typeArgument;
        // });
    }
}
