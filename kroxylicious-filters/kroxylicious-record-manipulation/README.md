# Record Manipulation Filter

A Kroxylicious filter that transforms Kafka record data in flight. Masking/redacting fields
(e.g. replacing a credit card number with zeros, hashing a name) is the motivating use case, but
the design generalizes to synthetic data generation and other ad-hoc transforms.

**Status:** wired into the real Kroxylicious filter framework - `RecordManipulation` is a
`FilterFactory`, discoverable via `META-INF/services`, and `RecordManipulationFilter` transforms a
record's key/value/timestamp on produce (`IN`) or fetch (`OUT`) traffic for one topic. **JSON,
Avro (binary encoding), and Protobuf (binary encoding) are wired up today.** Avro's own JSON
encoding exists under `format/avro` but isn't reachable from the filter yet - see "Known
limitations" below.

## How it works

Everything is built from one core abstraction,
[`op/BaseTypedOp<T, R>`](src/main/java/io/kroxylicious/filter/record/manipulation/op/BaseTypedOp.java):
an operation from a value of type `T` to a value of type `R`, carrying its own
`java.lang.reflect.Type` for both `T` and `R`. Operations compose
([`ComposedOp`](src/main/java/io/kroxylicious/filter/record/manipulation/common/ComposedOp.java)),
and composition is type-checked at build time (via `geantyref`'s `GenericTypeReflector`) rather
than assumed. This one mechanism covers everything from extracting a record's key/value/timestamp,
to (de)serializing JSON, to walking a JSON-Schema-shaped tree down to individual fields, to the
leaf-level primitives (constants, random values, HMAC, encrypt/decrypt, regex replace, ...).

Operations are pluggable: each is an
[`op/OpFactory<T, R>`](src/main/java/io/kroxylicious/filter/record/manipulation/op/OpFactory.java)
(a standard Kroxylicious `@Plugin`), resolved by name at the point in the schema where it's
needed - see
[`config/OpConfigs.compose`](src/main/java/io/kroxylicious/filter/record/manipulation/config/OpConfigs.java)
for the shared resolve-and-typecheck logic every format engine uses.

## Configuration

```yaml
topic: my-topic
direction: IN   # IN = Produce traffic, OUT = Fetch/ShareFetch traffic
recordTransform:
  intoRecordValue:
    from: RecordValue
    apply:
      - op: DeserializeJson
      - op: JsonTransform
        schema:
          type: object
          properties:
            creditCardNumber:
              type: string
              apply:
                - op: ValueString
                  value: "0000 0000 0000 0000"
      - op: SerializeJson
```

Avro (binary encoding) follows the same shape, via `DeserializeAvro`/`AvroTransform`/
`SerializeAvro`, but only `DeserializeAvro` takes schema config - it bundles the parsed `Schema`
together with each decoded value (an `AvroValue`), so `AvroTransform`/`SerializeAvro` just use
whatever schema arrives rather than parsing their own copy. The `apply` chain for a field sits as a
sibling of its own `type`, the same way it sits alongside a JSON Schema field's `type` above:

```yaml
topic: my-topic
direction: IN
recordTransform:
  intoRecordValue:
    from: RecordValue
    apply:
      - op: DeserializeAvro
        schema: |
          {"type": "record", "name": "Payment", "fields": [
              {"name": "creditCardNumber", "type": "string", "apply": [
                  {"op": "ValueString", "value": "0000 0000 0000 0000"}
              ]}
          ]}
      - op: AvroTransform
      - op: SerializeAvro
```

Avro's schema format doesn't require the root to be a `record` - a schema of just `array`/scalar/
`enum`/`fixed` (with `apply` sitting directly on that root schema, the same way it sits on `items`
for an array's elements) works too; only `map` schemas (root or field) aren't supported yet.

Protobuf (binary encoding) follows the same shape, via `DeserializeProtobuf`/`ProtobufTransform`/
`SerializeProtobuf`, but only `DeserializeProtobuf` takes schema config - it bundles the parsed
schema (a `.proto` file, plus a `rootMessageName` naming the top-level message type) together with
each decoded message, so `ProtobufTransform`/`SerializeProtobuf` just use whatever schema arrives
rather than parsing their own copy (see the `ProtoValue` javadoc for why: unlike Avro/JSON schemas,
Protobuf's descriptors use reference identity, so every op independently re-parsing the same
`.proto` text would produce field references that don't match each other). The `apply` chain for a
field sits in the field's own option syntax:

```yaml
topic: my-topic
direction: IN
recordTransform:
  intoRecordValue:
    from: RecordValue
    apply:
      - op: DeserializeProtobuf
        protoText: |
          syntax = "proto3";
          message Payment {
              string credit_card_number = 1 [(apply) = { op: "ValueString", value: "0000 0000 0000 0000" }];
          }
        rootMessageName: Payment
      - op: ProtobufTransform
      - op: SerializeProtobuf
```

`intoRecordKey` and `intoTimestamp` follow the same `from`/`apply` shape and, if omitted, default
to passing the original key/timestamp through unchanged.

For a worked end-to-end example (including topic/direction matching and the default-passthrough
behaviour), see
[`RecordManipulationFilterTest`](src/test/java/io/kroxylicious/filter/record/manipulation/filter/RecordManipulationFilterTest.java)
(JSON),
[`RecordManipulationFilterAvroTest`](src/test/java/io/kroxylicious/filter/record/manipulation/filter/RecordManipulationFilterAvroTest.java)
(Avro), and
[`RecordManipulationFilterProtobufTest`](src/test/java/io/kroxylicious/filter/record/manipulation/filter/RecordManipulationFilterProtobufTest.java)
(Protobuf). There are also real-cluster equivalents,
[`RecordManipulationFilterIT`](../../kroxylicious-integration-tests/src/test/java/io/kroxylicious/it/filter/manipulation/RecordManipulationFilterIT.java),
[`RecordManipulationFilterAvroIT`](../../kroxylicious-integration-tests/src/test/java/io/kroxylicious/it/filter/manipulation/RecordManipulationFilterAvroIT.java),
and
[`RecordManipulationFilterProtobufIT`](../../kroxylicious-integration-tests/src/test/java/io/kroxylicious/it/filter/manipulation/RecordManipulationFilterProtobufIT.java).
These can be run from the repo root with:

```shell
mvn install -pl :kroxylicious-record-manipulation
mvn verify -pl kroxylicious-integration-tests -Dit.test=RecordManipulationFilterIT
mvn verify -pl kroxylicious-integration-tests -Dit.test=RecordManipulationFilterAvroIT
mvn verify -pl kroxylicious-integration-tests -Dit.test=RecordManipulationFilterProtobufIT
```

(the first command rebuilds this module's jar from source so the second and third pick up local
changes; `-Dtest` combined with `-am` in a single command fails because it's applied
reactor-wide, including to upstream modules that don't have a matching test)

## Known limitations

- JSON, Avro (binary encoding), and Protobuf (binary encoding) only. Avro's own JSON encoding is
  implemented at the engine level but not yet wired into the filter's config or `apply`
  resolution.
- One topic and one direction per filter instance.
- No schema registry integration - the record value is assumed to be plain JSON/Avro/Protobuf, not
  prefixed with a registry schema ID.
- `Delete`/insert of JSON object properties is supported; array element insert/delete is not.
  Avro and Protobuf have no equivalent field insert/delete at all, since every field declared by a
  schema must be present in any conforming record/message (Protobuf's implicit-presence fields
  aside, which a mask never manufactures presence for - see `ProtobufMessages`).
- Avro `map` schemas (as a root or a field type) aren't supported yet - see
  `AvroFunction.buildStructural`'s validation. Protobuf `map` fields and message-level `apply` (as
  opposed to field-level) aren't supported yet either - see `ProtobufFunction`'s validation for both.
- Javadoc coverage and a handful of pre-existing SpotBugs findings are known debt, not yet
  addressed.
