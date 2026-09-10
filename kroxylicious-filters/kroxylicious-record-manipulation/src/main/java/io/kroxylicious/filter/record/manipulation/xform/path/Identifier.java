/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.xform.path;

public enum Identifier {
    ROOT("$"),
    CURRENT("@");

    public final String symbol;

    Identifier(String symbol) {
        this.symbol = symbol;
    }
}
