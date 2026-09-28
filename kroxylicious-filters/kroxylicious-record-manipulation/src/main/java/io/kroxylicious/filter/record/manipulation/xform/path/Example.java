/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.xform.path;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Objects;

public class Example {

    static boolean not(boolean value) {
        return !value;
    }

    static boolean and(boolean value1, boolean value2) {
        return value1 && value2;
    }

    static boolean or(boolean value1, boolean value2) {
        return value1 || value2;
    }

    public static void main(String[] args) throws Throwable {
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        var notH = lookup.findStatic(Example.class, "not", MethodType.methodType(Boolean.TYPE, Boolean.TYPE));
        var andH = lookup.findStatic(Example.class, "and", MethodType.methodType(Boolean.TYPE, Boolean.TYPE, Boolean.TYPE));
        var orH = lookup.findStatic(Example.class, "or", MethodType.methodType(Boolean.TYPE, Boolean.TYPE, Boolean.TYPE));

        var equalsH = lookup.findStatic(Objects.class, "equals", MethodType.methodType(Boolean.TYPE, Object.class, Object.class));
        var notEqualsH = MethodHandles.filterReturnValue(equalsH, notH);

        MethodHandle TRUE = MethodHandles.constant(Boolean.TYPE, true);
        MethodHandle FALSE = MethodHandles.constant(Boolean.TYPE, false);
        MethodHandle TWELVE = MethodHandles.constant(Integer.TYPE, 12);

        System.out.println((boolean) andH.invokeExact(true, true));
        System.out.println((boolean) andH.invokeExact(true, false));
        System.out.println((boolean) orH.invokeExact(true, true));
        System.out.println((boolean) orH.invokeExact(true, false));
        System.out.println((boolean) orH.invokeExact(false, false));

        System.out.println((boolean) equalsH.invoke(12, 13));
        System.out.println((boolean) notEqualsH.invoke(12, 13));
    }
}
