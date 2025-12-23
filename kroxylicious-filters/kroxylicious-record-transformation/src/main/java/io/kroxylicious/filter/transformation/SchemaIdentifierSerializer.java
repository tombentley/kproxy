/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation;

import java.io.IOException;

import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeader;

public interface SchemaIdentifierSerializer {
    Header[] serialize(SchemaIdentifier schemaIdentifier, TransformationOutputStream out) throws IOException;
}

class PrefixSchemaIdentifierSerializer implements SchemaIdentifierSerializer {

    @Override
    public Header[] serialize(SchemaIdentifier schemaIdentifier, TransformationOutputStream out) throws IOException {
        if (schemaIdentifier instanceof Prefix prefix) {
            out.write(prefix.prefix());
        }
        else if (schemaIdentifier instanceof GlobalId globalId) {
            // TODO 4 byte
            out.writeLong(globalId.id());
        }
        else if (schemaIdentifier instanceof ContentId contentId) {
            // TODO 4 byte
            out.writeLong(contentId.id());
        }
        else if (schemaIdentifier instanceof ContentHash contentHash) {
            throw new RuntimeException();
        }
        return new Header[0];
    }
}

class HeaderSchemaIdentifierSerializer implements SchemaIdentifierSerializer {

    @Override
    public Header[] serialize(SchemaIdentifier schemaIdentifier, TransformationOutputStream out) {
        if (schemaIdentifier instanceof Prefix prefix) {
            return new Header[]{ new RecordHeader("io.apicurio.global.id", prefix.prefix()) };
        }
        else if (schemaIdentifier instanceof GlobalId globalId) {
            // TODO 4 byte
            //return new Header[]{ new RecordHeader("io.apicurio.global.id", globalId.id()) };
        }
        else if (schemaIdentifier instanceof ContentId contentId) {
            // TODO 4 byte
            //return new Header[]{ new RecordHeader("io.apicurio.content.id", contentId.id()) };
        }
        else if (schemaIdentifier instanceof ContentHash contentHash) {
            return new Header[]{ new RecordHeader("", contentHash.hash()) };
        }
        return new Header[0];
    }
}

