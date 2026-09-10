/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.kafka;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;

class TopTest {

    @Test
    void test() throws Exception {

        Top top = new YAMLMapper().readValue(
                """
                        result:
                          value: my_mask(read_json($.value))
                          key: your_mask(read_avro($.key))
                          timestamp: $.value.timestamp
                          headers: $.headers[2,1]
                        where:
                          read_json:
                            op: io.kroxylicious.op.jackson2.Json
                          my_mask:
                            op: io.kroxylicious.op.jsonata.Jsonata
                            jsonata: ""
                        
                          read_avro:
                            op: io.kroxylicious.op.avro.Binary
                            schema: avro_schema
                        
                          your_mask:
                            op: io.kroxylicious.op.xform.Avro
                            xform:
                              firstName: "REDACTED"
                              customerId: hmac($.customerId)
                              email: encrypt($[email])
                              area: regexReplace("(([A-Z]+[0-9]{2}).*", $[address][postCode], "$1");
                              city: $[address][city]
                        
                          resultSchema:
                            op: AvroSchema
                          # OR
                          resultSchemaFrom:
                            op: Apicurio
                            url: "https://my.apicurio.internal.example.com:8888"
                            contentId: 1234
                        
                        
                          regexReplace:
                            op: io.kroxylicious.op.regex.RegexReplace
                            engine: "re2j"
                        
                          hmac:
                            op: io.kroxylicious.op.hmac.Hmac
                            key:
                              op: Base64Decode
                              data: "my-key"
                        
                          encrypt:
                            op: io.kroxylicious.op.encryption.Encrypt
                            kek: "my-kek"
                            kms:
                             op: io.kroxylicious.AwsKms
                        """, Top.class);

    }

}