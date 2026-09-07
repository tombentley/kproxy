/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.common;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Random;
import java.util.function.ToIntFunction;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class ComposedOpTest {


    public MethodHandle asHandle() throws NoSuchMethodException, IllegalAccessException {
        var lookup = MethodHandles.lookup();
        var mh = lookup.findVirtual(ToIntFunction.class, "applyAsInt", MethodType.methodType(Integer.TYPE, OpContext.class));
        return mh;
    }

    public MethodHandle doublerAsHandle() throws NoSuchMethodException, IllegalAccessException {
        var lookup = MethodHandles.lookup();
        var mh = lookup.findVirtual(ConstantIntSupplier.class, "applyAsInt", MethodType.methodType(Integer.TYPE, OpContext.class));
        return mh;
    }

    @Test
    void handle() throws Throwable {
        var mh = asHandle();
        ConstantIntSupplier constantIntSupplier = new ConstantIntSupplier(42);
        OpContext opContext = new OpContext(new Random(), new byte[0]);
        int result = (int) mh.invokeExact(constantIntSupplier, opContext);
        assertThat(result).isEqualTo(42);
    }

}