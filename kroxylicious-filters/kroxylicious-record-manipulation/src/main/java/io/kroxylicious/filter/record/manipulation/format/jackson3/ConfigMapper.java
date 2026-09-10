/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.format.jackson3;

import tools.jackson.databind.ObjectMapper;

public class ConfigMapper {

    // Note for the _config_ we use jackson2, because that's what the rest of kroxy currently used for config parsing
    static final ObjectMapper CONFIG_MAPPER = new ObjectMapper();

    private ConfigMapper() {}

}
