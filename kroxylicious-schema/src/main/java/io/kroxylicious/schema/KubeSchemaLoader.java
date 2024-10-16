/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.schema;

import java.nio.file.Path;
import java.util.List;

/**
 * Knows how to obtain the KubeSchemas from a (plugin) jar file.
 */
public class KubeSchemaLoader {
    List<KubeSchema> load(Path jar) {
        // Open the jar as a ZIP
        // io.kroxylicious.plugins/${plugin interface}/${plugin impl}.yaml
        // TODO which comes first, Service discovery or schema discovery?
        //      Probably service discovery, since that's very well defined and build into the platform?
        // We start with a config file, an expected type, and maybe a top level schema
        // If we want to support different schema versions we might not know an expected type
        // (there might be several allowed top level schema types).
        // So we start with a config file at the root
        // We parse it (can just use SAX) to obtain a version
        // Using the version we can look up a root schema and root type type (e.g. KroxyConfigV1)
        // - what we do currently is we find plugin types as part of deserialization
        //   by hooking into Jackson's lookup for the type of a property of an object
        //   e.g. Suppose KroxyConfigV1 has a property of type Object, with the @Plugin annotation
        // - what we ought to do is inspect the root schema, find the place where it references something else
        //   and then look up that schema
        //   ... recursively
        //   ... construct the closed world schema
        //   ... validate the config doc against that
        //   ... and only then attempt deserialization
        return null;
    }
}
