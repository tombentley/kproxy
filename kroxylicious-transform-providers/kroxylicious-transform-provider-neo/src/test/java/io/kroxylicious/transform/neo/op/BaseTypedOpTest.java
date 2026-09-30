/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.transform.neo.op;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.kroxylicious.transform.neo.type.SchemaType;
import io.kroxylicious.transform.neo.type.TypeSystem;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class BaseTypedOpTest {

    @Mock
    TypeSystem<Object> typeSystem;

    @Test
    void introducedSchemaDefaultsToEmpty() {
        // When
        var op = BaseTypedOp.of(String.class, String.class, (value, opContext) -> value);

        // Then
        assertThat(op.introducedSchema()).isEmpty();
    }

    @Test
    void carriesIntroducedSchema() {
        // Given
        SchemaType<Object> schemaType = SchemaType.of(typeSystem, "withKeySchema");

        // When
        var op = BaseTypedOp.<String, String> of(String.class, String.class, (value, opContext) -> value, schemaType);

        // Then
        assertThat(op.introducedSchema()).contains(schemaType);
    }
}
