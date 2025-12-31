/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation.model;

import java.util.Optional;

import io.kroxylicious.filter.transformation.api.mapper.DataMapping;
import io.kroxylicious.filter.transformation.api.schema.identification.SchemaIdSerializer;
import io.kroxylicious.filter.transformation.api.schema.identification.SchemaIdDeserializer;
import io.kroxylicious.filter.transformation.api.schema.identification.WireSchemaId;

/**
 * <p>Specifies a transformation to be applied to record keys or record values.
 * We break such transformations into two cases:</p>
 * <dl>
 *    <dt>{@linkplain EarlyBoundDataTransform early bound}</dt>
 *    <dd>when the {@link io.kroxylicious.filter.transformation.api.format.DataFormat}
 *    is known, or can be found out, when the filter factory is configured.
 *    This is often the case when certain topics are known to use a data format that
 *    doesn't require a schema to be deserialized, such as JSON.
 *    This case also covers the situation where a schema is held in a schema registry, but
 *    the coordinates for the schema are given in the configuration.
 *    </dd>
 *    <dt>{@linkplain LateBoundDataTransform late bound}</dt>
 *    <dd>when the {@link io.kroxylicious.filter.transformation.api.format.DataFormat}
 *    depends in information in the record itself.
 *    For these transformations everything has to be done on the hot path,
 *    during the handling of a {@code Fetch} response.
 *    This is the case when a schema registry is used, and we don't know ahead of time of
 *    a topic to schema association.</dd>
 * </dl>
 *
 * <p>In either case, once a {@code DataFormat} is known the transformation proceeds in the same way:</p>
 * <ol>
 *     <li>The data is deserialized</li>
 *     <li>The data mapping is applied</li>
 *     <li>The data is serialized</li>
 * </ol>
 *
 * @param <W> The initial type of wire schema id.
 * @param <S> The initial type of schema.
 * @param <V> The initial type of value.
 * @param <W2> The final type of wire schema id.
 * @param <S2> The final type of schema.
 * @param <V2> The final type of value.
 */
public sealed interface DataTransform<W extends WireSchemaId, S, V,
        W2 extends WireSchemaId, S2, V2> permits EarlyBoundDataTransform, LateBoundDataTransform {
    SchemaIdDeserializer<W> schemaIdDeserializer();
    Optional<DataMapping<W, S, V, W2, S2, V2>> mapperOpt();
    SchemaIdSerializer<W2> schemaIdSerializer();
}
