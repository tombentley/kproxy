package io.kroxylicious.filter.transformation.api;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.concurrent.CompletionStage;

import io.kroxylicious.filter.transformation.api.format.DataFormat;
import io.kroxylicious.filter.transformation.api.mapper.Context;
import io.kroxylicious.filter.transformation.api.mapper.TypeCheckable;
import io.kroxylicious.filter.transformation.api.schema.identification.WireSchemaId;
import io.kroxylicious.filter.transformation.api.schema.registry.SchemaRegistry;
import io.kroxylicious.filter.transformation.format.avro.AvroFormat;
import io.kroxylicious.filter.transformation.format.avro.AvroSchemaDeserializer;

public class SchemaResolver implements TypeCheckable {

    SchemaRegistry schemaRegistry;

    @Override
    public Type<?, ?, ?> typeCheck(Type<?, ?, ?> type) {
        if (!WireSchemaId.class.isAssignableFrom(type.cls())) {
            throw new TypeException(String.format("Type %s is not a WireSchemaId.", type));
        }
        return new Type<>(WireSchemaId.class, Void.class, DataFormat.class);
    }

    public CompletionStage<DataFormat<?, ?>> transform(WireSchemaId wireSchemaId, Context context) {
        return schemaRegistry.getSchema(wireSchemaId).thenApply(resolvedSchema -> {
            return switch (resolvedSchema.type()) {
                case "avro" -> {
                    try {
                        yield new AvroFormat(wireSchemaId, new AvroSchemaDeserializer()
                                .deserialize(new ByteArrayInputStream(resolvedSchema.schema()), context));
                    }
                    catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                default -> throw new IllegalStateException("Unsupported schema type: " + resolvedSchema.type());
            };
        });
    }
}
