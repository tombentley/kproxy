/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.plugins;

import java.lang.module.Configuration;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.lang.module.ResolvedModule;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;

@SuppressWarnings("checkstyle:RegexpSinglelineJava")
public class Main {
    public static void main(String[] args) throws Exception {
        // Create a layer for the API
        var apiFinder = extracted(
                Path.of("kroxylicious-api/target/kroxylicious-api-0.14.0-SNAPSHOT.jar"),
                Path.of("/home/tom/.m2/repository/org/apache/kafka/kafka-clients/4.0.0/kafka-clients-4.0.0.jar"),
                Path.of("/home/tom/.m2/repository/com/github/spotbugs/spotbugs-annotations/4.9.3/spotbugs-annotations-4.9.3.jar"),
                Path.of("/home/tom/.m2/repository/com/fasterxml/jackson/core/jackson-annotations/2.19.1/jackson-annotations-2.19.1.jar"));
        URLClassLoader apiClassLoader = new URLClassLoader(getUrls(apiFinder));
        ModuleLayer apiLayer = getModuleLayer(apiFinder, apiClassLoader);

        var runtimeFinder = extracted(Path.of("kroxylicious-runtime/target/kroxylicious-runtime-0.14.0-SNAPSHOT-dist"));
        // create a class loader with the api as parent
        URLClassLoader runtimeClassLoader = new URLClassLoader(getUrls(runtimeFinder), apiClassLoader);
        ModuleLayer runtimeLayer = getModuleLayer(runtimeFinder, apiLayer, runtimeClassLoader);

        // Create a find for a plugin. We'd to this for each plugin (discovered by traversing a directory contains dir like this
        var pluginFinder = extracted(Path.of("kroxylicious-filters/kroxylicious-record-encryption/target/kroxylicious-record-encryption-0.14.0-SNAPSHOT-dist"));
        // create a class loader with the api as parent
        URLClassLoader pluginClassLoader = new URLClassLoader(getUrls(pluginFinder), apiClassLoader);
        ModuleLayer pluginLayer = getModuleLayer(pluginFinder, apiLayer, pluginClassLoader);

        System.out.println("Found module: " + pluginLayer);
        // Shouldn't be able to load the plugin from the API
        try {
            Class.forName("io.kroxylicious.filter.encryption.RecordEncryption", true, apiClassLoader);
            throw new RuntimeException("Oops");
        }
        catch (ClassNotFoundException e) {

        }
        // Should be able to load the plugin from its CL
        Class.forName("io.kroxylicious.filter.encryption.RecordEncryption", true, pluginClassLoader);
        // Should be able to load the API from the plugin CL
        var ff = Class.forName("io.kroxylicious.proxy.filter.FilterFactory", true, pluginClassLoader);
        // Actually this needs to be more general, because one plugin can consume types from another
        // (e.g. KMS), so we can't just use a single api classloader as parent
        // In full generality we'd need to support diamond => graphs of CL, and therefore not simply a parent-first model.

        // Eventually we want the ?runtime? to be able to ask for the filter factories (or more generally the plugin impls)
        // from the plugin layer
        for (var filterProvider : ServiceLoader.load(pluginLayer, ff).stream().toList()) {
            System.out.println(filterProvider.type());
        }
    }

    private static ModuleLayer getModuleLayer(ModuleFinder finder, ModuleLayer apiLayer, URLClassLoader pluginClassLoader) {
        ModuleLayer pluginLayer;
        {
            // ModuleLayer boot = ModuleLayer.boot();
            Configuration resolve = Configuration
                    .resolve(finder, List.of(apiLayer.configuration()), ModuleFinder.of(),
                            finder.findAll().stream()
                                    .map(ModuleReference::descriptor)
                                    .map(ModuleDescriptor::name)
                                    // .filter(n -> !n.equals("kafka.clients"))
                                    // .filter(n -> !n.equals("com.fasterxml.jackson.annotation"))
                                    .toList());
            // ResolvedModule pluginResolvedModule = resolve.findModule("io.kroxylicious.filter.encryption").get();

            pluginLayer = apiLayer.defineModules(resolve, x -> pluginClassLoader);

        }
        return pluginLayer;
    }

    private static ModuleLayer getModuleLayer(ModuleFinder apiFinder, URLClassLoader apiClassLoader) {
        ModuleLayer apiLayer;
        ModuleLayer boot = ModuleLayer.boot();
        Configuration resolve = boot.configuration()
                .resolve(apiFinder,
                        ModuleFinder.of(),
                        apiFinder.findAll().stream()
                                .map(ModuleReference::descriptor)
                                .map(ModuleDescriptor::name)
                                .toList());
        ResolvedModule resolvedModule = resolve.findModule("io.kroxylicious.proxy.api").get();
        apiLayer = boot.defineModules(resolve, x -> apiClassLoader);
        System.out.println("Found module: " + apiLayer);
        return apiLayer;
    }

    private static URL[] getUrls(ModuleFinder finder) {
        var urls = finder.findAll().stream().map(ModuleReference::location)
                .map(Optional::get)
                .map(uri -> {
                    try {
                        return uri.toURL();
                    }
                    catch (MalformedURLException e) {
                        throw new RuntimeException(e);
                    }
                })
                .toArray(URL[]::new);
        return urls;
    }

    private static ModuleFinder extracted(Path... path) {
        // System.out.println(path.toAbsolutePath());
        // System.out.println(Files.exists(path));
        var finder = ModuleFinder.of(path);
        for (var moduleReference : finder.findAll()) {
            System.out.printf("module %s {%n", moduleReference.descriptor().name());
            for (var required : moduleReference.descriptor().requires()) {
                System.out.printf("  requires %s; // %s  %s%n", required.name(), required.compiledVersion().map(ModuleDescriptor.Version::toString).orElse(""),
                        required.rawCompiledVersion().orElse(""));
            }
            for (var opened : moduleReference.descriptor().opens()) {
                if (opened.isQualified()) {
                    System.out.printf("  opens %s to %s;%n", opened.source(), String.join(", ", opened.targets()));
                }
                else {
                    System.out.printf("  opens %s;%n", opened.source());
                }
            }
            for (var used : moduleReference.descriptor().uses()) {
                System.out.printf("  uses %s;%n", used);
            }
            for (var provided : moduleReference.descriptor().provides()) {
                System.out.printf("  provides %s with %s;%n", provided.service(), String.join(", ", provided.providers()));
            }
            for (var export : moduleReference.descriptor().exports()) {
                if (export.isQualified()) {
                    System.out.printf("  exports %s to %s;%n", export.source(), String.join(", ", export.targets()));
                }
                else {
                    System.out.printf("  exports %s;%n", export.source());
                }
            }
            System.out.printf("}%n", moduleReference.descriptor().name());
        }
        return finder;
    }
}