/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.transform.neo;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Data mapping type representing an expression in the expression language.
 * The expression is parsed after Jackson deserialization.
 */
public class Expr {

    private final String node;

    @JsonValue
    public String node() {
        return node;
    }

    @JsonCreator
    public Expr(String node) {
        this.node = node;
    }

}
