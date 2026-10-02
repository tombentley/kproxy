/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.filter;

import java.util.List;

import io.kroxylicious.filter.record.manipulation.filter.apicurio.SchemaFacets;
import io.kroxylicious.filter.record.manipulation.filter.resolve.CompiledManipulation;

import edu.umd.cs.findbugs.annotations.Nullable;

public record Init(List<CompiledManipulation> compiledManipulations, @Nullable SchemaFacets schemaFacets) {}
