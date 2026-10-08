/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.transform.neo.format.avro;

import java.lang.reflect.Type;
import java.util.Map;

import org.apache.avro.generic.GenericRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import io.kroxylicious.transform.neo.op.OpContext;
import io.kroxylicious.transform.service.TransformInitializationContext;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class ReadAvroTest {

    @Mock
    TransformInitializationContext transformInitializationContext;

    @Mock
    Type argumentType;

    @Mock
    OpContext opContext;

    @AfterEach
    void afterEach() {
        Mockito.verifyNoInteractions(transformInitializationContext, argumentType, opContext);
    }

    @Test
    void introducesAvroTypeSystem() {
        // Given
        var factory = new ReadAvro();
        var config = Map.<String, Object> of(
                ReadAvro.SCHEMA_PARAMETRER, """
                        {
                          "type": "record",
                          "name": "FooBar",
                          "fields": [
                            {"name": "foo", "type": "int"}
                          ]
                        }
                        """);

        // When
        var op = factory.create(config, transformInitializationContext, argumentType);

        // Then
        assertThat(op.introducedSchema()).hasValueSatisfying(schemaType -> {
            assertThat(schemaType.isObject()).isTrue();
            assertThat(schemaType.propertyNames()).containsExactly("foo");
            assertThat(schemaType.property("foo").javaType()).isEqualTo(Integer.class);
            assertThat(schemaType.javaType()).isEqualTo(GenericRecord.class);
        });
    }
}
