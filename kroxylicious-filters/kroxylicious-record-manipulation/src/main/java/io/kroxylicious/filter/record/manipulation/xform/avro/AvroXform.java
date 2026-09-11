/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.xform.avro;

import java.lang.reflect.Type;
import java.util.Map;

import io.kroxylicious.filter.record.manipulation.op.PluginLookup;
import io.kroxylicious.filter.record.manipulation.common.StaticTypedOp;
import io.kroxylicious.filter.record.manipulation.format.avro.AvroValue;
import io.kroxylicious.filter.record.manipulation.op.BaseTypedOp;
import io.kroxylicious.filter.record.manipulation.op.OpContext;
import io.kroxylicious.filter.record.manipulation.op.OpFactory;

/**
 * Example:
 * <pre>{@code
 *     op: io.kroxylicious.op.xform.AvroXform
 *     xform:
 *       firstName: "REDACTED";
 *       customerId: hmac($.customerId);
 *       email: encrypt($[email]);
 *       area: regexReplace("(([A-Z]+[0-9]{2}).*", $[address][postCode], "$1");
 *       city: $[address][city]
 * }</pre>
 *
 * <table>
 * <tr>
 * <th>Tgt Avro</th>                        <th>Xform JSON</th></tr>
 * <tr><td>{@code record}</td>              <td>Object with string keys and Path values</td></tr>
 * <tr><td>{@code array<T>}</td>            <td>List of T-typed path values (but would need functions on lists)</td></tr>
 * <tr><td>{@code map<String, T>}</td>      <td>Object with String-typed path keys and T-typed path values</td></tr>
 * <tr><td>{@code enum}</td>                <td>String-typed path</td></tr>                                                 string
 * <tr><td>{@code fixed}</td>               <td>byte[] typed path</td></tr>                                                 string
 * <tr><td>{@code bytes}</td>               <td>byte[] typed path (length check at runtime)</td></tr>                       string
 * <tr><td>{@code string}</td>              <td>String-typed path</td></tr>                                                 string
* <tr><td>{@code int},{@code long}</td>    <td>int,long-typed path</td></tr>                                                string
 * <tr><td>{@code float},{@code double}</td><td>float,double-typed path</td></tr>                                           string
 * <tr><td>{@code boolean}</td>             <td>boolean-typed path</td></tr>                                                string
 * <tr><td>{@code null}</td>                <td>null</td></tr>
 * <tr><td>{@code X|Y}</td>                 <td>the JSON for {@code X}</td></tr>                                            string
 * </table>
 */
public class AvroXform implements OpFactory<AvroValue, AvroValue> {
    @Override
    public BaseTypedOp<AvroValue, AvroValue> create(Map<String, Object> config, PluginLookup lookup, Type argumentType) {
        Object xform = config.get("xform"); // xform is the JSON encoding of a default value
        // https://avro.apache.org/docs/1.12.0/specification/#schema-record

        // TODO we need to know the target schema to be able to deserialize the xform

        return new StaticTypedOp<AvroValue, AvroValue>() {
            @Override
            public AvroValue apply(AvroValue value, OpContext opContext) {
                // TODO merge the types from the schema with the paths from the xform
                // and check the paths have result types which match any target schema
                // var reader = new GenericDatumReader<>(value.schema());
                // Decoder decoder = DecoderFactory.get().jsonDecoder(value.schema(), is);
                // return reader.read(null, decoder);

                return null;
            }
        };
    }
}
