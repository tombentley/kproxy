/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.xform.path;

import java.util.ArrayList;
import java.util.List;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AvroGenericTreeTest {

    private static final Schema SCHEMA = new Schema.Parser()
            .parse("""
                    {
                      "type":"record",
                      "name":"Greeting",
                      "fields":[{
                        "name":"text",
                        "type":"string"
                      }]
                    }
                    """);

    @Test
    void canSelectRecordField() {
        GenericData.Record record = new GenericData.Record(SCHEMA);
        record.put("text", "Hello");
        List<Object> result = new ArrayList<>();
        new PathEvaluator<>(new AvroGenericTree()).eval(record,
                new Ast.Path<>(Identifier.ROOT, new Ast.Segment.Child<>(new Ast.Selector.Name<>("text")), result::add));
        assertThat(result).singleElement().isEqualTo("Hello");
    }

}