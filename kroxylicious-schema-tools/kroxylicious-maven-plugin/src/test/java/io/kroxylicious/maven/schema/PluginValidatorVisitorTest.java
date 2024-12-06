/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.maven.schema;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;

import io.kroxylicious.tools.schema.model.Reporting;
import io.kroxylicious.tools.schema.model.SchemaObject;

import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class PluginValidatorVisitorTest {

    record Report(String message, String arguments) {}

    class MyReporting implements Reporting {

        List<Report> fatals = new ArrayList<>();

        List<Report> errors = new ArrayList<>();

        List<Report> warnings = new ArrayList<>();

        @Override
        public void reportFatal(
                String message,
                Object... arguments
        ) {
            fatals.add(new Report(message, Arrays.stream(arguments).map(s -> "'" + s + "'").collect(Collectors.joining(","))));
        }

        @Override
        public void reportError(
                String message,
                Object... arguments
        ) {
            errors.add(new Report(message, Arrays.stream(arguments).map(s -> "'" + s + "'").collect(Collectors.joining(","))));
        }

        @Override
        public void reportWarning(
                String message,
                Object... arguments
        ) {
            warnings.add(new Report(message, Arrays.stream(arguments).map(s -> "'" + s + "'").collect(Collectors.joining(","))));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Valid1.yaml",
            "EnumIsUsed.yaml",
            "AllOfWithPropertiesLifted.yaml"
    })
    void valid(String file) throws IOException, URISyntaxException {
        // Given
        YAMLMapper mapper = new YAMLMapper();
        URL resource = getClass().getResource("/plugin/valid/" + file);
        var schemaObject = mapper.readValue(resource, SchemaObject.class);
        MyReporting myReporting = new MyReporting();

        // When
        schemaObject.visitSchemas(myReporting, resource.toURI(), new PluginValidatorVisitor());

        // Then
        assertThat(myReporting.fatals).isEmpty();
        assertThat(myReporting.warnings).isEmpty();
        assertThat(myReporting.errors).isEmpty();

        // TODO test by constructing a CRD and trying to apply it to minikube: it should be accepted
    }

    static List<Arguments> invalid() {
        return List.of(
            Arguments.of("ItemsIsAnArray.yaml",
                    "[Report[message=`items` at path '{}' must not be an array, arguments='']]"),
            Arguments.of("TypeArrayWithoutItems.yaml",
                    "[Report[message=`items` at path '{}' must be present if `type` is `array`, arguments='']]"),
            Arguments.of("UniqueItemsIsTrue.yaml",
                    "[Report[message=`uniqueItems` at path '{}' must not be `true`, arguments='']]"),
            Arguments.of("TypeIsMissing.yaml",
                    "[Report[message=`type` at path '{}' must be a `string`, arguments='']]"),
            Arguments.of("TypeIsAnArray.yaml",
                    "[Report[message=`type` at path '{}' must be a `string`, arguments='']]"),
            Arguments.of("PatternPropertiesIsUsed.yaml",
                    "[Report[message=`patternProperties` at path '{}' must not be used, arguments='']]"),
            Arguments.of("AdditionalPropertiesIsFalse.yaml",
                    "[Report[message=`additionalProperties` at path '{}' must not be false, arguments='']]"),
            Arguments.of("AdditionalPropertiesAndPropertiesAreUsed.yaml",
                    "[Report[message=`additionalProperties` at path '{}' is mutually exclusive with `properties`, arguments='']]"),
            Arguments.of("AdditionalPropertiesWithinJunctor.yaml",
                    "[Report[message=`additionalProperties` at path '{}' must not be used within any of [allOf, anyOf, oneOf, not], arguments='/allOf/0']]"),
            Arguments.of("DependenciesIsUsed.yaml",
                    "[Report[message=`dependencies` at path '{}' must not be used, arguments='']]"),
            // TODO type with x-kubernetes-int-or-string
            Arguments.of("AllOfWithType.yaml",
                    "[Report[message=`type` at path '{}' must not be used within any of [allOf, anyOf, oneOf, not], arguments='/allOf/0']]"),
                Arguments.of("AllOfWithPropertiesNotLifted.yaml",
                        "[Report[message=`dependencies` at path '{}' must not be used, arguments='']]"),
                Arguments.of("DescriptionWithinJunctor.yaml",
                        "[Report[message=`description` at path '{}' must not be used within {}, arguments='/allOf/0','allOf']]"),
                Arguments.of("DefaultWithinJunctor.yaml",
                        "[Report[message=`default` at path '{}' must not be used within {}, arguments='/oneOf/0','oneOf']]")
        );
    }

    @ParameterizedTest
    @MethodSource
    void invalid(String file, String expectedErrors) throws IOException, URISyntaxException {
        // Given
        YAMLMapper mapper = new YAMLMapper();
        URL resource = Objects.requireNonNull(getClass().getResource("/plugin/invalid/" + file));
        var schemaObject = mapper.readValue(resource, SchemaObject.class);
        MyReporting myReporting = new MyReporting();
        PluginValidatorVisitor visitor = new PluginValidatorVisitor();

        // When
        schemaObject.visitSchemas(myReporting, resource.toURI(), visitor);

        // Then
        assertThat(myReporting.fatals).isEmpty();
        assertThat(myReporting.warnings).isEmpty();
        assertThat(myReporting.errors).hasToString(expectedErrors);

        // TODO test by constructing a CRD and trying to apply it to minikube: it should be rejected
    }

}