/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.schema;

import com.fasterxml.jackson.core.TreeNode;

/**
 * A validator for proxy config JSONs/YAMLs
 */
public class PluginConfigValidator {

    private final KubeSchema schema;

    PluginConfigValidator(KubeSchema schema) {
        this.schema = schema;
    }

    void validate(TreeNode config) throws InvalidConfigException {

    }
}
