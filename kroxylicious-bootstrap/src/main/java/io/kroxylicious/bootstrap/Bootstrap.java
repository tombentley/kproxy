/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.kroxylicious.bootstrap;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

public class Bootstrap {
    private final Plugins plugins;

    Bootstrap() {
        // step 1: find all the plugins
        ServiceLoader<Plugin> sl = ServiceLoader.load(Plugin.class);

        var iter = sl.stream().iterator();
        List<Plugin> pluginList = new ArrayList<>();
        while (iter.hasNext()) {
            var pp = iter.next();
            //pp.type(); // e.g. RecordEncryption or AwsKms
            // step 2: instantiate all the plugins
            var plugin = pp.get();
            pluginList.add(plugin);

        }

        this.plugins = new Plugins(pluginList);
    }

    Plugins plugins() {
        return plugins;
    }

    <T extends Plugin> T start(Class<T> rootPlugin, String config) {
        var root = plugins.implementationsOf(rootPlugin).get(0); // TODO get(0)!

        // step 3: get the config schema for the root (this will construct the schema for the whole tree)
        var schema = root.configSchema(plugins);

        // step 4: validate the config
        var validConfig = schema.validateConfig(config);
        root.configure(plugins, validConfig);
        return root;
        // ???? convert the untyped config to typed config
        // return the typed config???
        // TODO we can't do this (for the whole tree) without fiddling with Jackson's type stuff
    }



    //    class Main {
//        Main(Config config) {}
//        void close() { }
//    }

    //class MainConfig {}
}
