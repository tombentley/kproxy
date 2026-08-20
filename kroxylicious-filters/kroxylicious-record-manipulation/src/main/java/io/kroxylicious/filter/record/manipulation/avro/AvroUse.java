/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.avro;

import org.apache.avro.Schema;

public class AvroUse {

    public static void main(String[] a) {
        var avroSchemaYaml = """
                {"namespace": "example.avro",
                    "type": "record",
                    "name": "User",
                    "fields": [
                        {"name": "firstName", "type": "string"},
                        {"name": "surname", "type": "string"},
                        {"name": "ageYears", "type": "int"},
                        {"name": "aliases", "type": "int",
                            "items": "string"
                        },
                        {"name": "address",  "type": [{
                            "type": "record",
                            "name": "Address",
                            "fields": [
                                {"name": "streetAddress", "type": "string"},
                                {"name": "city", "type": "string"}
                            ]
                        }, "null"]},
                        {"name": "favorite_color", "type": ["string", "null"]}
                    ]
                }
                    """;

        Schema.Parser parser = new Schema.Parser();
        Schema schema = parser.parse(avroSchemaYaml);

        // Basically, let's just reuse the Avro schema schema, but add our own keywords (apply)
        var maskContent = """
                {"namespace": "example.avro",
                    "type": "record",
                    "name": "User",
                    "fields": [
                        {"name": "firstName", "type": "string", "apply": [
                            {"op": "random",
                             "minLength": 3,
                             "maxLength": 5
                            }
                        ]},
                        {"name": "surname", "type": "string", "apply": [
                            {"op": "choose",
                             "from": ["Smith", "Jones"]
                            }
                        ]},
                        {"name": "ageYears", "type": "int"},
                        {"name": "aliases", "type": "int",
                            "items": "string"
                        },
                        {"name": "address",  "type": [{
                            "type": "record",
                            "name": "Address",
                            "fields": [
                                {"name": "streetAddress", "type": "string"},
                                {"name": "city", "type": "string"}
                            ]
                        }, "null"]},
                        {"name": "favorite_color", "type": ["string", "null"]}
                    ]
                }
                    """;
    }
}
