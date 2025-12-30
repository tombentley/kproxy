/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation.api.mapper;

import java.util.List;

import org.apache.kafka.common.header.Header;

public interface HeaderMapping {

    List<Header> transform(List<Header> headers, Context context);
}
