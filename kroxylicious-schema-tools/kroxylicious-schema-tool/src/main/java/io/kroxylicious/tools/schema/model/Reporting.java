/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.tools.schema.model;

public interface Reporting {

    void reportFatal(String message, Object... arguments);

    void reportError(String message, Object... arguments);

    void reportWarning(String message, Object... arguments);
}
