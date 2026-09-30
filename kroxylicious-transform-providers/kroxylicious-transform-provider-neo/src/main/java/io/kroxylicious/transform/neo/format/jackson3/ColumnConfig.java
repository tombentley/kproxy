/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.transform.neo.format.jackson3;

import edu.umd.cs.findbugs.annotations.Nullable;
import tools.jackson.dataformat.csv.CsvSchema;

public record ColumnConfig(String name,
                           CsvSchema.ColumnType type,
                           @Nullable String arrayElementSep) {}
