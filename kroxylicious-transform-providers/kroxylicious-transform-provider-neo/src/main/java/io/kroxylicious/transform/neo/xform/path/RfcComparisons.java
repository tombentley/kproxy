/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.transform.neo.xform.path;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Objects;

import io.kroxylicious.transform.neo.type.Nothing;
import io.kroxylicious.transform.neo.type.Null;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.StringNode;

/**
 * The comparison semantics of the filter language, following RFC 9535 §2.3.5.2 and extended to this
 * module's richer value algebra (the canonical {@link Nothing} and {@link Null} values, and the boxed
 * scalars produced by the Kafka/Avro domains alongside Jackson {@code JsonNode}s).
 * <p>
 * The rules are constructed so evaluation never throws on a type mismatch:
 * <ul>
 *   <li>{@code ==}/{@code !=} apply to all values: {@code Nothing == Nothing} and {@code Null == Null} are
 *       true, {@code Nothing == Null} is false, numbers compare by value ({@code 1 == 1.0}), strings and
 *       booleans by value, and any cross-category comparison is unequal.</li>
 *   <li>{@code <}, {@code <=}, {@code >}, {@code >=} are meaningful only for number-vs-number and
 *       string-vs-string; every other combination (including any {@link Nothing}, {@link Null} or boolean
 *       operand, or a type mismatch) yields {@code false} — so {@code x < y} and {@code y < x} can both be
 *       false.</li>
 * </ul>
 * Values are classified into a small set of {@link Category categories}; Java {@code null} should not reach
 * here (the evaluator's algebra is Java-null-free) but is defensively treated as {@link Null}.
 */
final class RfcComparisons {

    private RfcComparisons() {
    }

    private enum Category {
        NOTHING,
        NULL,
        NUMBER,
        STRING,
        BOOLEAN,
        OTHER
    }

    /** Applies the comparison {@code op} to two already-evaluated values, never throwing. */
    static boolean compare(Ast.BinOp op, Object left, Object right) {
        return switch (op) {
            case EQ -> equalTo(left, right);
            case NE -> !equalTo(left, right);
            case LT, LE, GT, GE -> ordered(op, left, right);
            default -> throw new IllegalStateException("not a comparison operator: " + op);
        };
    }

    private static boolean equalTo(Object left, Object right) {
        Category cl = classify(left);
        Category cr = classify(right);
        if (cl != cr) {
            return false;
        }
        return switch (cl) {
            case NOTHING, NULL -> true; // the sole value of each unit type equals itself
            case NUMBER -> toNumber(left).compareTo(toNumber(right)) == 0;
            case STRING -> toStr(left).equals(toStr(right));
            case BOOLEAN -> toBool(left) == toBool(right);
            case OTHER -> deepEquals(left, right);
        };
    }

    private static boolean ordered(Ast.BinOp op, Object left, Object right) {
        Category cl = classify(left);
        Category cr = classify(right);
        int cmp;
        if (cl == Category.NUMBER && cr == Category.NUMBER) {
            cmp = toNumber(left).compareTo(toNumber(right));
        }
        else if (cl == Category.STRING && cr == Category.STRING) {
            cmp = toStr(left).compareTo(toStr(right));
        }
        else {
            return false; // ordering is undefined across (or outside) numbers and strings
        }
        return switch (op) {
            case LT -> cmp < 0;
            case LE -> cmp <= 0;
            case GT -> cmp > 0;
            case GE -> cmp >= 0;
            default -> throw new IllegalStateException("not an ordering operator: " + op);
        };
    }

    private static Category classify(Object value) {
        if (value == Nothing.INSTANCE) {
            return Category.NOTHING;
        }
        if (value == null || value == Null.INSTANCE) {
            return Category.NULL;
        }
        if (value instanceof JsonNode node) {
            if (node.isMissingNode()) {
                return Category.NOTHING;
            }
            if (node.isNull()) {
                return Category.NULL;
            }
            if (node.isNumber()) {
                return Category.NUMBER;
            }
            if (node.isBoolean()) {
                return Category.BOOLEAN;
            }
            if (node instanceof StringNode) {
                return Category.STRING;
            }
            return Category.OTHER; // array or object
        }
        if (value instanceof Number) {
            return Category.NUMBER;
        }
        if (value instanceof CharSequence) {
            return Category.STRING;
        }
        if (value instanceof Boolean) {
            return Category.BOOLEAN;
        }
        return Category.OTHER;
    }

    private static BigDecimal toNumber(Object value) {
        if (value instanceof JsonNode node) {
            return node.decimalValue();
        }
        if (value instanceof BigDecimal bd) {
            return bd;
        }
        if (value instanceof BigInteger bi) {
            return new BigDecimal(bi);
        }
        if (value instanceof Double || value instanceof Float) {
            return BigDecimal.valueOf(((Number) value).doubleValue());
        }
        return BigDecimal.valueOf(((Number) value).longValue());
    }

    private static String toStr(Object value) {
        if (value instanceof JsonNode node) {
            return node.asString();
        }
        return value.toString();
    }

    private static boolean toBool(Object value) {
        if (value instanceof JsonNode node) {
            return node.booleanValue();
        }
        return (Boolean) value;
    }

    private static boolean deepEquals(Object left, Object right) {
        if (left instanceof byte[] lb && right instanceof byte[] rb) {
            return Arrays.equals(lb, rb);
        }
        // JsonNode.equals is a deep, structural comparison; other domains fall back to Object equality.
        return Objects.equals(left, right);
    }
}
