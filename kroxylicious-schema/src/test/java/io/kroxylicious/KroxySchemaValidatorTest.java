/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious;

import org.junit.jupiter.api.Test;

import io.kroxylicious.schema.KroxySchemaValidator;

class KroxySchemaValidatorTest {

    @Test
    void testResolveDefinitions() {
        var cv = new KroxySchemaValidator();
        cv.parseYaml("""
                type: object
                definitions:
                  Foo: 
                    true
                  Bar:
                    type: string
                type: object
                properties:
                  foo: 
                    $ref: Foo
                  bars: 
                    type: array
                    items:
                      $ref: Bar
                """).resolveDefinitions();
        // TODO definitions containing definitions OK
        var s0 = """
                definitions:
                  ContainsDefinitions:
                    definitions:
                      Contained: true
                type: array
                items:
                  $ref: #/definitions/ContainsDefinitions/definitions/Contained
                """;
        // TODO definitions not in the root are OK
        // TODO A definition directly referencing itself not OK
        var s = """
                definitions:
                  SelfRecursive:
                    $ref: #/definitions/SelfRecursive
                """;
        // TODO A definition indirectly referencing itself not OK
        var s2 = """
                definitions:
                  IndirectlyRecursive1:
                    $ref: #/definitions/IndirectlyRecursive2
                  IndirectlyRecursive2:
                    $ref: #/definitions/IndirectlyRecursive1
                """;

        // TODO   A definition referencing itself through a oneOf etc (indirect, but constraining the same value)
        var s3 = """
                definitions:
                  IndirectlyRecursive1:
                    not:
                      $ref: #/definitions/IndirectlyRecursive1
                """;
        //   Using a definition via each of the keywords
        var s4 = """
                definitions:
                  Foo: true
                properties:
                  foo: 
                    $ref: #/definitions/Foo
                """;
        // Also items, additionalProperties
        // anyOf, oneOf, not, allOf
    }

    @Test
    void validatesDraft4() {
        // TODO check an explicit $schema naming draft 4 is OK
        //   check an explicit $schema naming something else is not OK
        //   check no undefined keywords
        //   check no undefined keywords in definitions
    }

    @Test
    void validatesKubeBasic() {
        // TODO Check each disallowed keyword is prevented
    }

    @Test
    void validatesStructural() {

    }

}