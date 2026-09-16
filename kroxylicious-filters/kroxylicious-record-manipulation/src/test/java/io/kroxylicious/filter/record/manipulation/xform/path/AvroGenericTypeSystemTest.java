/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.xform.path;

import java.util.List;
import java.util.Map;

import org.apache.avro.Schema;
import org.apache.avro.SchemaParser;
import org.apache.avro.generic.GenericRecord;
import org.junit.jupiter.api.Test;

import io.leangen.geantyref.TypeFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AvroGenericTypeSystemTest {

    public static final AvroGenericTypeSystem AVRO_GENERIC_TYPE_SYSTEM = new AvroGenericTypeSystem();

    @Test
    void recordProperties() {
        var schema = new SchemaParser().parse("""
                {
                  "type": "record",
                  "name": "FooBar",
                  "fields": [
                    {"name": "foo", "type": "int"},
                    {"name": "bar", "type": "string"}
                  ]
                }
                """).mainSchema();
        var foo = AVRO_GENERIC_TYPE_SYSTEM.objectPropertySchema(schema, "foo");
        var bar = AVRO_GENERIC_TYPE_SYSTEM.objectPropertySchema(schema, "bar");
        var baz = AVRO_GENERIC_TYPE_SYSTEM.objectPropertySchema(schema, "baz");
        assertThat(AVRO_GENERIC_TYPE_SYSTEM.objectProperties(schema)).isEqualTo(List.of("foo", "bar"));
        assertThat(foo.getType()).isEqualTo(Schema.Type.INT);
        assertThat(bar.getType()).isEqualTo(Schema.Type.STRING);
        assertThat(baz).isNull();
        assertThat(AVRO_GENERIC_TYPE_SYSTEM.isObjectType(schema)).isTrue();
        assertThat(AVRO_GENERIC_TYPE_SYSTEM.isObjectOpen(schema)).isFalse();
        assertThat(AVRO_GENERIC_TYPE_SYSTEM.isArrayType(schema)).isFalse();
        assertThat(AVRO_GENERIC_TYPE_SYSTEM.isArrayOpen(schema)).isFalse();
        assertThat(AVRO_GENERIC_TYPE_SYSTEM.arrayIndexes(schema)).isNull();

        assertThat(AVRO_GENERIC_TYPE_SYSTEM.typeOf(schema))
                .isEqualTo(GenericRecord.class);
    }

    @Test
    void arrayItems() {
        var schema = new SchemaParser().parse("""
                {
                  "type": "array",
                  "items": {
                      "type": "int"
                  }
                }
                """).mainSchema();
        assertThat(AVRO_GENERIC_TYPE_SYSTEM.isUnionType(schema)).isFalse();
        assertThat(AVRO_GENERIC_TYPE_SYSTEM.isObjectType(schema)).isFalse();
        assertThat(AVRO_GENERIC_TYPE_SYSTEM.isObjectOpen(schema)).isFalse();
        assertThat(AVRO_GENERIC_TYPE_SYSTEM.objectProperties(schema)).isEmpty();
        assertThat(AVRO_GENERIC_TYPE_SYSTEM.isArrayType(schema)).isTrue();
        assertThat(AVRO_GENERIC_TYPE_SYSTEM.isArrayOpen(schema)).isTrue();
        assertThat(AVRO_GENERIC_TYPE_SYSTEM.arrayIndexes(schema)).isEmpty();
        var zero = AVRO_GENERIC_TYPE_SYSTEM.arrayItemSchema(schema, 0);
        assertThat(zero.getType()).isEqualTo(Schema.Type.INT);
        var rest = AVRO_GENERIC_TYPE_SYSTEM.arrayItemSchema(schema);
        assertThat(rest.getType()).isEqualTo(Schema.Type.INT);

        assertThat(AVRO_GENERIC_TYPE_SYSTEM.typeOf(schema))
                .isEqualTo(TypeFactory.parameterizedClass(List.class, Integer.class));
    }

    @Test
    void mapValues() {
        var schema = new SchemaParser().parse("""
                {
                  "type": "map",
                  "values": {
                      "type": "int"
                  }
                }
                """).mainSchema();
        assertThat(AVRO_GENERIC_TYPE_SYSTEM.isUnionType(schema)).isFalse();
        assertThat(AVRO_GENERIC_TYPE_SYSTEM.isObjectType(schema)).isTrue();
        assertThat(AVRO_GENERIC_TYPE_SYSTEM.isObjectOpen(schema)).isTrue();
        assertThat(AVRO_GENERIC_TYPE_SYSTEM.objectProperties(schema)).isEmpty();
        assertThat(AVRO_GENERIC_TYPE_SYSTEM.isArrayType(schema)).isFalse();
        assertThat(AVRO_GENERIC_TYPE_SYSTEM.isArrayOpen(schema)).isFalse();
        assertThat(AVRO_GENERIC_TYPE_SYSTEM.arrayIndexes(schema)).isNull();
        var foo = AVRO_GENERIC_TYPE_SYSTEM.objectPropertySchema(schema, "foo");
        assertThat(foo.getType()).isEqualTo(Schema.Type.INT);
        var bar = AVRO_GENERIC_TYPE_SYSTEM.objectPropertySchema(schema);
        assertThat(bar.getType()).isEqualTo(Schema.Type.INT);

        assertThat(AVRO_GENERIC_TYPE_SYSTEM.typeOf(schema))
                .isEqualTo(TypeFactory.parameterizedClass(Map.class, String.class, Integer.class));
    }

    @Test
    void unionType() {
        var schema = new SchemaParser().parse("""
                [
                  {
                    "type": "map",
                    "values": {
                      "type": "int"
                    }
                  },
                  {
                    "type": "int"
                  }
                ]
                """).mainSchema();
        assertThat(AVRO_GENERIC_TYPE_SYSTEM.isUnionType(schema)).isTrue();
        assertThat(AVRO_GENERIC_TYPE_SYSTEM.isObjectType(schema)).isFalse();
        assertThat(AVRO_GENERIC_TYPE_SYSTEM.isObjectOpen(schema)).isFalse();
        assertThat(AVRO_GENERIC_TYPE_SYSTEM.objectProperties(schema)).isEmpty();
        assertThat(AVRO_GENERIC_TYPE_SYSTEM.isArrayType(schema)).isFalse();
        assertThat(AVRO_GENERIC_TYPE_SYSTEM.isArrayOpen(schema)).isFalse();
        assertThat(AVRO_GENERIC_TYPE_SYSTEM.arrayIndexes(schema)).isNull();
    }

}