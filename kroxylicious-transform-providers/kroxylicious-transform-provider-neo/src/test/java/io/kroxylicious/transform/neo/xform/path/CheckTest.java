/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.transform.neo.xform.path;

import java.util.function.Function;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import io.kroxylicious.proxy.config.ServiceBasedPluginFactoryRegistry;
import io.kroxylicious.transform.neo.Expr;
import io.kroxylicious.transform.neo.ExprParser;
import io.kroxylicious.transform.neo.RecordTransformConfig2;
import io.kroxylicious.transform.service.PluginLookup;

import tools.jackson.dataformat.yaml.YAMLMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CheckTest {

    private Check check = new Check(new PluginLookup() {
        @Override
        public <P> P pluginInstance(Class<P> c, String n) {
            return new ServiceBasedPluginFactoryRegistry().pluginFactory(c).pluginInstance(n);
        }
    });

    static Arguments scenario(String name, String input, String... expected) {
        return Arguments.argumentSet(name, input, expected);
    }

    static Stream<Arguments> rejects() {
        return Stream.of(
                scenario("Undefined function",
                        """
                                key: $.key
                                value: read_json($.value) # <--- read_json is not defined in the where property
                                timestamp: $.timestamp
                                headers: $.headers
                                """,
                        """
                                Property 'value' has expression `read_json($.value)` which references an undefined function 'read_json'
                                (Hint: functions are defined using the 'where' property)"""),
                scenario("Defined function, is not a known plugin",
                        """
                                key: $.key
                                value: read_json($.value)
                                timestamp: $.timestamp
                                headers: $.headers
                                where:
                                  read_json:
                                    op: this.package.does.not.exist.ReadJson # <-- that package does not exist
                                    format: json
                                """,
                        "Could not instantiate function 'read_json' referred to in expression for property 'value': "
                                + "Unknown io.kroxylicious.transform.neo.op.OpFactory plugin instance for name "
                                + "'this.package.does.not.exist.ReadJson'. "
                                + "Known plugin instances are [",
                        "Plugins must be loadable by java.util.ServiceLoader and annotated with @Plugin."),
                scenario("Defined function with unknown config property",
                        """
                                key: $.key
                                value: read_json($.value)
                                timestamp: $.timestamp
                                headers: $.headers
                                where:
                                  read_json:
                                    op: io.kroxylicious.transform.neo.format.jackson3.ReadJson
                                    fruit: apples # <--- there is no fruit property
                                """,
                        "Could not instantiate function 'read_json' referred to in expression for property 'value': "
                                + "Unrecognized property \"fruit\" (class io.kroxylicious.transform.neo.format.jackson3.ReadJson$JsonReaderConfig), "
                                + "not marked as ignorable",
                        "known properties",
                        "\"type\"",
                        "\"readFeatures\"",
                        "\"deserializationFeatures\""),
                scenario("Argument not assignable",
                        """
                                key: write_json('') # <--- Jackson in 'JsonNode mode' want's a StringNode
                                value: $['value']
                                timestamp: $.timestamp
                                headers: $.headers
                                where:
                                  write_json:
                                    op: io.kroxylicious.transform.neo.format.jackson3.WriteJson
                                    format: json
                                """,
                        "Property 'key' with expression `write_json('')`: Function write_json should take a tools.jackson.databind.JsonNode, but is being called with a value of type java.lang.String"),
                scenario("Expression not assignable (path)",
                        """
                                key: $.timestamp  # <--- timestamp has type long, not ByteBuffer
                                value: $['key']
                                timestamp: $.timestamp
                                headers: $.headers
                                where:
                                  write_json:
                                    op: io.kroxylicious.transform.neo.format.jackson3.WriteJson
                                    type: java.lang.Object
                                    format: json
                                """,
                        "Property 'key' with expression `$.timestamp`: Expression type is not assignable to java.nio.ByteBuffer: Expression type is long"),
                scenario("Expression not assignable (function)",
                        """
                                key: $.key
                                value: $['value']
                                timestamp: $.timestamp
                                headers: write_json('') # <----- `headers` has type Header[], not ByteBuffer
                                where:
                                  write_json:
                                    op: io.kroxylicious.transform.neo.format.jackson3.WriteJson
                                    type: java.lang.Object
                                    format: json
                                """,
                        "Property 'headers' with expression `write_json('')`: Expression type is not assignable to io.kroxylicious.kafka.common.header.Header[]: Expression type is java.nio.ByteBuffer"),
                scenario("Filter body references an undefined function",
                        """
                                key: $.key
                                value: $.value
                                timestamp: $.timestamp
                                headers: $.headers[?bogus(@)] # <--- the filter uses a function not defined in 'where'
                                """,
                        "references an undefined function 'bogus'"));
    }

    @ParameterizedTest
    @MethodSource
    void rejects(String content,
                 String... errorMessage) {
        // Given
        var config = new YAMLMapper().readValue(content, RecordTransformConfig2.class);

        // When/Then
        assertThatThrownBy(() -> check.check(config)).isExactlyInstanceOf(CheckException.class)
                .message().contains(errorMessage);
    }

    static Stream<Arguments> acceptsWellTyped() {
        return Stream.of(
                Arguments.argumentSet("Identity",
                        """
                                key: $.key
                                value: $.value
                                timestamp: $.timestamp
                                headers: $.headers
                                """),
                Arguments.argumentSet("Permutation",
                        """
                                key: $.value
                                value: $.key
                                timestamp: $.timestamp
                                headers: $.headers
                                """),
                Arguments.argumentSet("Well-typed no-op",
                        """
                                key: $.key
                                value: write_json(read_json($.value))
                                timestamp: $.timestamp
                                headers: $.headers
                                where:
                                  read_json:
                                    op: io.kroxylicious.transform.neo.format.jackson3.ReadJson
                                    format: json
                                  write_json:
                                    op: io.kroxylicious.transform.neo.format.jackson3.WriteJson
                                    format: json
                                """),
                Arguments.argumentSet("Jsonata",
                        """
                                key: $.key
                                value: write_json(jsonata(read_json($.value)))
                                timestamp: $.timestamp
                                headers: $.headers
                                where:
                                  read_json:
                                    op: io.kroxylicious.transform.neo.format.jackson3.ReadJson
                                    format: json
                                  jsonata:
                                    op: io.kroxylicious.transform.neo.ops.jsonata.dashjoin.Jsonata
                                    expression: $sum(c)
                                  write_json:
                                    op: io.kroxylicious.transform.neo.format.jackson3.WriteJson
                                    format: json
                                    type: java.lang.Object # <--- The dashjoin version of jsonata use the Map/List JSON typing
                                """),
                Arguments.argumentSet("read_avro introduces an Avro type system",
                        """
                                key: write_avro(read_avro($.key))
                                value: $.value
                                timestamp: $.timestamp
                                headers: $.headers
                                where:
                                  read_avro:
                                    op: io.kroxylicious.transform.neo.format.avro.ReadAvro
                                    schema: |
                                      {"type":"record","name":"FooBar","fields":[{"name":"foo","type":"int"}]}
                                  write_avro:
                                    op: io.kroxylicious.transform.neo.format.avro.WriteAvro
                                    format: json
                                """));
    }

    @ParameterizedTest
    @MethodSource
    void acceptsWellTyped(String content) {
        // Given
        var config = new YAMLMapper().readValue(content, RecordTransformConfig2.class);

        // When/Then
        assertThatNoException().isThrownBy(() -> check.check(config));
    }

    @Test
    void deserializeFailedOnInvalidJsonPathExpressions() {
        // Given
        Expr expr = new Expr("foo");
        // When/Then

        assertThatThrownBy(() -> Check.parseExpr(expr))
                .isInstanceOf(InvalidExpressionException.class)
                .hasMessage(
                        """
                                Found 1 syntax errors
                                1:3: no viable alternative at input 'foo'.
                                """);

    }

    static Stream<Arguments> deserializeJsonPathExpressions() {
        return Stream.of(
                Arguments.arguments("$.key",
                        (Function<ExprParser.ExprContext, Object>) e -> e.expr1().jsonpathQuery()),
                Arguments.arguments("$[\"value\"]",
                        (Function<ExprParser.ExprContext, Object>) e -> e.expr1().jsonpathQuery()),
                Arguments.arguments("$['timestamp']",
                        (Function<ExprParser.ExprContext, Object>) e -> e.expr1().jsonpathQuery()),
                Arguments.arguments("1",
                        (Function<ExprParser.ExprContext, Object>) e -> e.expr1().literal()),
                Arguments.arguments("\"'hello'\"",
                        (Function<ExprParser.ExprContext, Object>) e -> e.expr1().literal()),
                Arguments.arguments("true",
                        (Function<ExprParser.ExprContext, Object>) e -> e.expr1().literal()),
                Arguments.arguments("null",
                        (Function<ExprParser.ExprContext, Object>) e -> e.expr1().literal()),
                Arguments.arguments("foo(1, $.headers)",
                        (Function<ExprParser.ExprContext, Object>) e -> e.expr1().logicalExpr()));
    }

    @ParameterizedTest
    @MethodSource
    void deserializeJsonPathExpressions(String pathExpression,
                                        Function<ExprParser.ExprContext, Object> mustBeNonNull) {
        // Given
        Expr expr = new Expr(pathExpression);

        // When
        ExprParser.ExprContext t = Check.parseExpr(expr);

        // Then
        assertThat(mustBeNonNull.apply(t)).isNotNull();
        assertThat(t.getText()).isEqualTo(pathExpression);
    }

    @Test
    void serializeJsonPathExpressions() {
        // Given
        YAMLMapper yamlMapper = new YAMLMapper();
        var config = yamlMapper.readValue("""
                key: $.key
                value: $.value
                timestamp: $.timestamp
                headers: foo(1, $.headers)
                """, RecordTransformConfig2.class);

        // When
        var str = yamlMapper.writeValueAsString(config);

        // Then
        assertThat(str).isEqualTo("""
                ---
                key: "$.key"
                value: "$.value"
                timestamp: "$.timestamp"
                headers: "foo(1, $.headers)"
                """);
    }

}
