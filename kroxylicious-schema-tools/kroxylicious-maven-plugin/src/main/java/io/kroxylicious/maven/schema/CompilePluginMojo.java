/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.maven.schema;

import java.net.URI;
import java.util.List;
import java.util.Map;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.BooleanLiteralExpr;
import com.github.javaparser.ast.expr.ClassExpr;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.Name;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;

import io.kroxylicious.tools.schema.compiler.BeanPropertyStrategy;
import io.kroxylicious.tools.schema.compiler.Diagnostics;
import io.kroxylicious.tools.schema.compiler.PropertyAnnotator;
import io.kroxylicious.tools.schema.compiler.SchemaCompiler;
import io.kroxylicious.tools.schema.compiler.TypeAnnotator;
import io.kroxylicious.tools.schema.model.SchemaObject;
import io.kroxylicious.tools.schema.model.SchemaType;

@Mojo(name = "compile-plugin", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class CompilePluginMojo extends AbstractCompileSchemaMojo {
    @Override
    protected SchemaCompiler schemaCompiler() throws MojoExecutionException {
        return new SchemaCompiler(List.of(source.toPath()),
                null,
                readHeaderFile(),
                existingClasses != null ? existingClasses : Map.of(),
                List.of(new TypeAnnotator() {
                    @Override
                    public List<AnnotationExpr> annotateClass(
                                                              Diagnostics diagnostics,
                                                              SchemaObject typeSchema) {
                        // @Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false, lazyCollectionInitEnabled = false, builderPackage =
                        // "io.fabric8.kubernetes.api.builder")
                        return List.of(
                                new NormalAnnotationExpr(
                                        new Name("io.sundr.builder.annotations.Buildable"),
                                        NodeList.nodeList(
                                                new MemberValuePair("editableEnabled", new BooleanLiteralExpr(false)),
                                                new MemberValuePair("validationEnabled", new BooleanLiteralExpr(false)),
                                                new MemberValuePair("generateBuilderPackage", new BooleanLiteralExpr(true)),
                                                new MemberValuePair("builderPackage", new StringLiteralExpr("io.fabric8.kubernetes.api.builder")))));
                    }
                }),
                new BeanPropertyStrategy(),
                true,
                List.of(new PluginAnnotator()));
    }

    @Override
    protected void validate(Diagnostics diagnostics, URI base, SchemaObject schemaObject) {
        // schemaObject.visitSchemas(diagnostics, base, new PluginValidatorVisitor());
    }

    /**
     * A {@link PluginAnnotator} that:
     * <ul>
     * <li>adds {@code @PluginImplName} to string-typed properties with {@code format: plugin-impl-name}
     * and which also have a {@code plugin-interface-name} keyword naming the interface.
     * </li>
     * <li>adds {@code @PluginImplConfig} to object-typed properties with {@code format: plugin-impl-config}
     * and which also have a {@code impl-name-property} keyword naming corresponding property.
     * </li>
     * </ul>
     */
    private static class PluginAnnotator implements PropertyAnnotator {
        @Override
        public List<AnnotationExpr> annotateConstructorParameter(
                                                                 Diagnostics diagnostics,
                                                                 String property,
                                                                 SchemaObject propertySchema) {

            if (List.of(SchemaType.STRING).equals(propertySchema.getType())
                    && "plugin-impl-name".equals(propertySchema.getFormat())) {
                Object o = propertySchema.getUnknownProperties().get("plugin-interface-name");
                if (o instanceof String interfaceName) {
                    return List.of(new SingleMemberAnnotationExpr(
                            new Name("io.kroxylicious.proxy.plugin.PluginImplName"),
                            new ClassExpr(new ClassOrInterfaceType(null,
                                    interfaceName))));
                }
                diagnostics.reportError("`format: plugin-impl-name` requires the `plugin-interface-name` property");
            }
            else if (List.of(SchemaType.OBJECT).equals(propertySchema.getType())
                    && "plugin-impl-config".equals(propertySchema.getFormat())) {
                Object o = propertySchema.getUnknownProperties().get("impl-name-property");
                if (o instanceof String implName) {
                    return List.of(new NormalAnnotationExpr(
                            new Name("io.kroxylicious.proxy.plugin.PluginImplConfig"),
                            NodeList.nodeList(
                                    new MemberValuePair(new SimpleName("implNameProperty"),
                                            new StringLiteralExpr(implName)))));
                }

                diagnostics.reportError("`format: plugin-impl-config` requires the `impl-name-property` property");
            }

            return List.of();
        }
    }
}
