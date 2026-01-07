/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation.config;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;

class RecordTransformationConfigTest {
    ObjectMapper objectMapper = new YAMLMapper();
    @Test
    void shouldCreateRecordTransformationConfig() throws JsonProcessingException {
        RecordTransformationConfig recordTransformationConfig =
                objectMapper.readValue("""
                    schemaRegistries:
                      - name: my-apicurio-reg
                        type: ApicurioRegistry
                        url: https://schema.apicurio.example.com
                      - name: my-confluent-reg
                        type: ConfluentSchemaRegistry
                        url: https://schema.confluent.example.com
                    formats:
                      - name: json
                        type: Json
                      - name: UserRecord
                        type: JsonSchema
                        schemaFile: path/to/my/schema.json
                      - name: Invoice
                        type: Avro
                        schemaFile: path/to/my/schema.avsc
                      - name: PurchaseOrder
                        registry: my-apicurio-reg
                        groupId: default
                        contentId: my-content
                      - name: SupplierKey
                        registry: my-confluent-reg
                        subject: suppliers-key
                      - name: SupplierValue
                        registry: my-confluent-reg
                        subject: suppliers-value
                    
                    topicBindings:
                      - topicName: users
                        keyFormat: Json   ## need to solve the format naming problem
                        valueFormat: UserRecord  ## implied by the mappings and/or the presence of schema
                        tranformations:
                          ...
                      - topicName: invoices
                        valueFormat: Invoice  ## implied by the mappings and/or the presence of schema
                        tranformations:
                          - type: JsonRemove
                            config:
                              recordKey:
                                - path: /name
                          - type: AvroReplaceField
                            config: # a value transform
                              recordValue:
                                fieldName: name
                                stringValue: "***"
                          - type: AppendRecordHeader
                            config:
                              headerKey: my-header
                              utf8Value: hello-world
                          - type: ReplaceRecordHeader
                            config: 
                              headerKey: your-header
                              base64Value: vrnui45908gj8034
                               """,
                        RecordTransformationConfig.class);
        System.out.println(recordTransformationConfig);
    }

}