/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.filter;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

import io.kroxylicious.filter.record.manipulation.filter.resolve.CompiledManipulation;

public record Init(List<CompiledManipulation<?, ?>> compiledManipulations,
                   Map<String, AutoCloseable> services, Executor executor) {}
