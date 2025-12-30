/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation.format.avro;

import org.apache.avro.Schema;

import io.kroxylicious.filter.transformation.api.SchemaAndValue;
import io.kroxylicious.filter.transformation.api.mapper.Context;

public class AvroPatch implements AvroDataMapping {



    @Override
    public SchemaAndValue<Schema, Object> transform(SchemaAndValue<Schema, Object> schemaAndValue, Context context) {
        return null;
        /*
        Schema schema;
        GenericData gd = null;
        return switch (schema.getType()) {

            case NULL -> new AvroValue(null);
            case BOOLEAN -> new AvroValue((Boolean) value.value());
            case INT -> new AvroValue((Integer) value.value());
            case LONG -> new AvroValue((Long) value.value());
            case FLOAT -> new AvroValue((Float) value.value());
            case DOUBLE ->  new AvroValue((Double) value.value());
            case STRING -> new AvroValue((String) value.value());
            case BYTES -> new AvroValue((byte[]) value.value()); // ticky
            case ENUM -> {
                new GenericData.Fixed(schema);
                yield new AvroValue((Enum) value.value());
            }
            case ARRAY -> new AvroValue((List) value.value());
            case MAP -> new AvroValue((Map) value.value());
            case FIXED -> new AvroValue((GenericData.Fixed) value.value());
            case RECORD -> {
                GenericRecord value1 = gd.deepCopy(schema, (GenericRecord) value.value());
                value1.put("", null);
                yield new AvroValue(value1);
            }
            case UNION -> new AvroValue((Union) value.value());

        };
         */
    }
    /*
    {op:add, path:/redacted, value:true}
    {op:replace, path:/givenName, value:"xxx"}
    {op:remove, path:/dob}
     */
}
