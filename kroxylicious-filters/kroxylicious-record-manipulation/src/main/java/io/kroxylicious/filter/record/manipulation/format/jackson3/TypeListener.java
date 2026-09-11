/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.format.jackson3;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import io.kroxylicious.op.parser.TypeNameParser;

import tools.jackson.databind.JavaType;
import tools.jackson.databind.type.TypeFactory;

public class TypeListener extends io.kroxylicious.op.parser.TypeNameBaseListener {

    private final TypeFactory typeFactory;
    private final List<JavaType> stack = new ArrayList<>();


    public TypeListener(TypeFactory typeFactory) {
        this.typeFactory = typeFactory;
    }

    @Override
    public void enterTypeName(TypeNameParser.TypeNameContext ctx) {
        stack.clear();
    }

    @Override
    public void exitType(TypeNameParser.TypeContext ctx) {
        for (var a : ctx.BOX()) {
            stack.add(typeFactory.constructArrayType(stack.removeLast()));
        }
    }

    @Override
    public void exitRawType(TypeNameParser.RawTypeContext ctx) {
        String text = ctx.getText();
        Type t = switch (text) {
            case "boolean" -> Boolean.TYPE;
            case "byte" -> Byte.TYPE;
            case "short" -> Short.TYPE;
            case "int" -> Integer.TYPE;
            case "long" -> Long.TYPE;
            case "float" -> Float.TYPE;
            case "double" -> Double.TYPE;
            case "char" -> Character.TYPE;
            default -> {
                try {
                    yield Class.forName(text);
                }
                catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
            }
        };
        stack.add(typeFactory.constructType(t));
    }

    @Override
    public void exitTypeArgumentList(TypeNameParser.TypeArgumentListContext ctx) {
        int numTypeArgs = ctx.COMMA().size();
        JavaType[] typeArgs = new JavaType[numTypeArgs];
        for (int i = 0; i < numTypeArgs; i++) {
            typeArgs[numTypeArgs - 1 - i] = stack.removeLast();
        }
        var rawType = stack.removeLast().getRawClass();
        var pt = typeFactory.constructParametricType(rawType, typeArgs);
        stack.add(pt);
    }

    @Override
    public void exitTypeArgument(TypeNameParser.TypeArgumentContext ctx) {
        if (ctx.wildcardType() != null) {
            typeFactory.constructFromCanonical()
        }
    }
}
