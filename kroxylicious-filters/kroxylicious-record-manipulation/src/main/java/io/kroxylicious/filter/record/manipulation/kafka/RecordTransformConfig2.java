/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.kafka;

import java.util.Map;

/**
 * <pre>{@code
 * key: your_mask(my_avro($key))
 * value: my_mask(json($value))
 * timestamp: $value.timestamp
 * headers = $headers[2,1]
 * where:
 *   - name: my_mask # the name used above
 *     type: io.kroxylicious.op.jsonata.Jsonata
 *     jsonata: <jsonata>
 *   - name: json
 *     type: io.kroxylicious.op.jackson2.Json       # package name encodes vendor, Json is encoding
 *   - name: my_avro
 *     type: io.kroxylicious.op.avro.Binary
 *     schema: <avro schema>
 *   - name: your_mask
 *     type: io.kroxylicious.op.xform.Avro
 *     xform: |
 *       {
 *         "firstName": "REDACTED"
 *         "customerId": Hmac($customerId)
 *         "email": Encrypt($email)
 *         "area": RegexReplace("(([A-Z]+[0-9]{2}).*", $address.postCode, "$1")
 *         "city": $address.city
 *       }
 *   - fn: io.kroxylicious.op.Hmac # fn is short for when the name is the uqcn of the type
 *     key:
 *   - fn: io.kroxylicious.op.Encrypt
 *     kek:
 *   - fn: RegexReplace
 *   - name: my_protobuf
 *     type: io.kroxylicious.op.protobuf.Binary
 *     schema: <avro schema>
 * }</pre>
 *
 *
 * @param key F
 * @param value F
 * @param timestamp F
 * @param headers F
 * @param where F
 */
record RecordTransformConfig2(Expr key,
                              Expr value,
                              Expr timestamp,
                              Expr headers,
                              Map<String, String> where) {}
