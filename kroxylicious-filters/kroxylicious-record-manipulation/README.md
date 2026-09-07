# Record Manipulation Filter

A Kroxylicious filter that transforms Kafka record data in flight. Masking/redacting fields
(e.g. replacing a credit card number with zeros, hashing a name) is the motivating use case, but
the design generalizes to synthetic data generation and other ad-hoc transforms.

**Status:** wired into the real Kroxylicious filter framework - `RecordManipulation` is a
`FilterFactory`, discoverable via `META-INF/services`, and `RecordManipulationFilter` transforms a
record's key/value/timestamp on produce (`IN`) or fetch (`OUT`) traffic for one topic. **JSON and
Avro (binary encoding) are wired up today.** Avro's own JSON encoding and Protobuf masking exist
under `format/avro` and `format/protobuf` but aren't reachable from the filter yet - see "Known
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
`SerializeAvro`. Since Avro binary data isn't self-describing, each op takes the same Avro schema
(as JSON text) - the `apply` chain for a field sits as a sibling of its own `type`, the same way it
sits alongside a JSON Schema field's `type` above:

```yaml
topic: my-topic
direction: IN
recordTransform:
  intoRecordValue:
    from: RecordValue
    apply:
      - op: DeserializeAvro
        schema: &schema |
          {"type": "record", "name": "Payment", "fields": [
              {"name": "creditCardNumber", "type": "string", "apply": [
                  {"op": "ValueString", "value": "0000 0000 0000 0000"}
              ]}
          ]}
      - op: AvroTransform
        schema: *schema
      - op: SerializeAvro
        schema: *schema
```

`intoRecordKey` and `intoTimestamp` follow the same `from`/`apply` shape and, if omitted, default
to passing the original key/timestamp through unchanged.

For a worked end-to-end example (including topic/direction matching and the default-passthrough
behaviour), see
[`RecordManipulationFilterTest`](src/test/java/io/kroxylicious/filter/record/manipulation/filter/RecordManipulationFilterTest.java)
(JSON) and
[`RecordManipulationFilterAvroTest`](src/test/java/io/kroxylicious/filter/record/manipulation/filter/RecordManipulationFilterAvroTest.java)
(Avro). There are also real-cluster equivalents,
[`RecordManipulationFilterIT`](../../kroxylicious-integration-tests/src/test/java/io/kroxylicious/it/filter/manipulation/RecordManipulationFilterIT.java)
and
[`RecordManipulationFilterAvroIT`](../../kroxylicious-integration-tests/src/test/java/io/kroxylicious/it/filter/manipulation/RecordManipulationFilterAvroIT.java).
These can be run from the repo root with:

```shell
mvn install -pl :kroxylicious-record-manipulation
mvn verify -pl kroxylicious-integration-tests -Dit.test=RecordManipulationFilterIT
mvn verify -pl kroxylicious-integration-tests -Dit.test=RecordManipulationFilterAvroIT
```

(the first command rebuilds this module's jar from source so the second and third pick up local
changes; `-Dtest` combined with `-am` in a single command fails because it's applied
reactor-wide, including to upstream modules that don't have a matching test)

## Known limitations

- JSON and Avro (binary encoding) only. Avro's own JSON encoding and Protobuf masking are
  implemented at the engine level but not yet wired into the filter's config or `apply`
  resolution. `AvroTransform` is also scoped to record-shaped schemas - Avro permits other schema
  shapes (array, map, a bare scalar, ...) as a serialization root, but `AvroBinaryDeserializer`/
  `AvroBinarySerializer` (which `DeserializeAvro`/`SerializeAvro` wrap) only support records today.
- One topic and one direction per filter instance.
- No schema registry integration - the record value is assumed to be plain JSON/Avro, not
  JSON/Avro/Protobuf with a registry-ID prefix.
- `Delete`/insert of JSON object properties is supported; array element insert/delete is not.
  Avro has no equivalent field insert/delete at all, since every field declared by a schema must
  be present in any conforming record.
- Javadoc coverage and a handful of pre-existing SpotBugs findings (mostly in the Avro/Protobuf
  code) are known debt, not yet addressed.
