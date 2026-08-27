# Record Manipulation Filter

**Status: experimental.** This module is not yet wired into the Kroxylicious filter framework (no
`Filter`/`FilterFactory`, no `META-INF/services` entry). It is a set of building blocks, driven by
`main()`-based demos, for a future filter that transforms record data — masking/redaction being the
motivating use case, but the design is meant to be more general (e.g. synthetic data generation).

## Idea

A mask is described in a syntax that borrows the *shape* of the data format's own schema language —
JSON-Schema-like keywords (`type`, `properties`, `items`) for JSON, Avro schema syntax for Avro — but
deliberately keeps two concerns separate that simply reusing the schema syntax wholesale would conflate:

- **Selection**: *where* in the document does something apply? `type`/`properties`/`items` (and, later,
  `patternProperties`/`additionalProperties`) are genuinely about navigating the document's structure, and
  reusing the schema's own vocabulary for this is natural — a schema author already thinks in these terms.
- **Transformation**: *what* happens once you're there? This is the new part: an `apply` keyword whose
  value is a *list* of named, independently pluggable operations (`RandomInt`, `HmacString`, `ValueString`,
  ...  — see [Pluggable operations](#pluggable-operations) below), composed in declared order via
  [`common/ContextPipeline`](src/main/java/io/kroxylicious/filter/record/manipulation/common/ContextPipeline.java).

These have to be kept apart because JSON Schema's own keywords are validation *predicates* — they're ANDed
together, side-effect-free, and order-independent by design (a value either satisfies all of them or it
doesn't). A sequence of transformations has none of those properties: encrypting then hashing a value gives
a different result than hashing then encrypting it. Folding "what operation to run" into the same keyword
that decides "does this schema match" doesn't generalise to that, so `apply` is its own explicitly-ordered,
composable list, kept distinct from the structural keywords that select where it runs.

For JSON, this split is visible directly in the config model:
[`jackson/SchemaConfig`](src/main/java/io/kroxylicious/filter/record/manipulation/jackson/SchemaConfig.java)
represents the schema-shaped part (`type`/`properties`/`items`/`apply`), and tolerates *any* other real JSON
Schema keyword (`pattern`, `contains`, `minLength`, ...) via a `@JsonAnySetter`/`@JsonAnyGetter` catch-all
instead of failing to parse — the goal is that an existing JSON Schema document can have `apply` added to it
directly, not that this module has to model JSON Schema's entire vocabulary.
[`config/OpConfig`](src/main/java/io/kroxylicious/filter/record/manipulation/config/OpConfig.java) is just
the shape of one entry in `apply`'s list: an `op` name plus that operation's own properties, squashed into
one flat JSON object (e.g. `{op: RandomInt, minInclusive: 0, maxExclusive: 10}`) via the same
`@JsonAnySetter` catch-all `SchemaConfig` uses. It deliberately doesn't carry `type`, `properties`, or
`items` itself, so an operation can't be forced into a type-specific shape that would foreclose a field
ever having a JSON Schema type union (`type: [string, number]`) — and, unlike a closed set of hardcoded
keywords, `op` names a plugin implementation, so third parties can add new operations without touching this
module. See [Pluggable operations](#pluggable-operations) below for how `op` gets resolved.

Each data format still gets its own config model and its own code to walk it — there isn't one grammar
shared across formats, since each format has its own type system. What *is* shared is the small set of
primitive operations in `common` (and, increasingly, the operations themselves — see
[Pluggable operations](#pluggable-operations) below), and the pattern of building a
`BiFunction<Node, Context, Node>` from the schema tree, recursing into structure first and then composing
each node's own `apply` chain. Format-specific adapters (see `jackson/Jackson.java`) bridge the
format-agnostic `common` primitives onto the format's native node types (e.g. Jackson's `JsonNode`).

Stages built this way are given their own named type per format —
[`jackson/JacksonFunction`](src/main/java/io/kroxylicious/filter/record/manipulation/jackson/JacksonFunction.java)
(`BiFunction<JsonNode, Context, JsonNode>`), with `avro/AvroFunction`/`protobuf/ProtoFunction` as its Avro/
Protobuf counterparts — rather than being plain `BiFunction<JsonNode, Context, JsonNode>` values. That
matters for [`common/ContextPipeline`](src/main/java/io/kroxylicious/filter/record/manipulation/common/ContextPipeline.java),
which validates and runs a chain of stages by reflecting on each stage's *concrete* generic type: a lambda
assigned directly to a generic `BiFunction<T, Context, T>` erases its type arguments at runtime, whereas one
assigned to a named subinterface with the type arguments fixed does not, since the parameterization lives on
the interface declaration rather than the lambda. (A single field's own `apply` chain - see
[Pluggable operations](#pluggable-operations) below - sidesteps this a different way: each operation carries
its own [`common/TypedOp`](src/main/java/io/kroxylicious/filter/record/manipulation/common/TypedOp.java)
input/output type as explicit data instead of relying on a fixed-type interface, so it can be a bare lambda.)
`common/Pipeline` is the `Context`-free sibling used one level up: `Use.java` composes a
`JacksonDeserializer`, a built mask/unmask `JacksonFunction`, and a `JacksonSerializer` into one whole-record
`Pipeline`; `JacksonFunction.buildMask` itself builds a smaller, per-field `ContextPipeline` out of a field's
own `apply` list, so `apply: [{op: EncryptString}, {op: HmacString}]` on one field genuinely composes two
independently resolved operations in the declared order.

## Pluggable operations

`apply`'s operation vocabulary is open, not a closed set of hardcoded keywords: each entry names a plugin
implementation (Kroxylicious's standard `@Plugin` mechanism, via `kroxylicious-api`) rather than one of a
fixed list of fields on a config record.

- Every operation is exposed through
  [`common/OpFactory<T, R>`](src/main/java/io/kroxylicious/filter/record/manipulation/common/OpFactory.java)
  (`TypedOp<T, R> create(Map<String, Object> config)`), parameterised directly over the operation's input
  and output type rather than routed through a bespoke, fixed-generic interface per base type (an earlier
  design had a `StringOpFactory`/`IntOpFactory`/... family, one per type, each extending a shared
  `OpFactory<Op extends BiFunction<?, Context, ?>>` shape - abandoned because it meant a new base type, or a
  future non-type-preserving operation, needed a whole new interface declaration before a single plugin
  could be written against it). A single plugin class is still monomorphic (it builds exactly one
  `OpFactory<T, R>` instantiation), matching every other plugin in the codebase and matching the
  type-suffixed primitives it usually delegates to (`RandomIntSupplier`/`RandomStringSupplier`, and so on) -
  that property doesn't depend on there being a separate interface per type, just on each plugin class
  picking concrete `T`/`R` type arguments once. Concretely: `RandomInt`/`RandomString`, `ValueInt`/
  `ValueString`, `ChooseInt`/`ChooseString`, and the String-only `HmacString`/`EncryptString`/
  `DecryptString` (see `config/` for all of them) — each with its own disjoint `Config` record, rather than
  one record's worth of fields where only one is ever populated at a time (`config/OpConfig` itself
  deliberately isn't one of these unioned records, having learned from that mistake).
  [`common/TypedOp<T, R>`](src/main/java/io/kroxylicious/filter/record/manipulation/common/TypedOp.java) is
  what `create` returns: a `BiFunction<T, Context, R>` paired with its own input/output type, carried as
  data (an `io.leangen.geantyref.TypeToken`, not a plain `Class` - a `Class` erases its own generic
  arguments, e.g. a future `List<Integer>` and `List<String>` operation would both just be `List.class`,
  whereas a `TypeToken` wraps a full `java.lang.reflect.Type` and compares accordingly) rather than left to
  be recovered by reflecting on a lambda's declared interface. That's what lets `ContextPipeline` validate
  composition and `Requirement.TYPE_PRESERVING` without every operation needing its own fixed-type marker
  interface: any lambda works, since `TypedOp.of(Class<T>, Class<R>, BiFunction<T, Context, R>)` (or the
  `TypeToken`-keyed overload, for a future non-`Class`-expressible type) attaches the type information
  explicitly at construction time.
- [`config/OpConfig`](src/main/java/io/kroxylicious/filter/record/manipulation/config/OpConfig.java)'s
  `config` is deliberately a plain `Map<String, Object>`, not a Jackson tree type (`JsonNode`) — that keeps
  the plugin-facing API surface to JDK types plus `jackson-annotations` only, so a future Jackson-major-
  version migration (expected to rename `jackson-databind`'s package, unlike `jackson-annotations`'s) can't
  ripple through every plugin implementor's method signature. Each plugin converts its own share of the map
  via a private `ObjectMapper.convertValue` call.
- Every operation implementation is registered under the one `OpFactory` interface (not partitioned per
  base type the way separate `StringOpFactory`/`IntOpFactory` interfaces would be), so plugin names are
  unique across every operation, not just within one base type - already true in practice, since
  `RandomInt`/`RandomString`/`RandomLong` etc. are already distinct names. Which input/output type an `op`
  name must resolve to depends on the primitive type of the field the operation applies to — information
  the format-specific engine only has once it's walked its own schema, not something Jackson's usual
  `@PluginImplName`/`@PluginImplConfig` polymorphic-config-resolution machinery can decide up front. So
  resolution is deliberately deferred:
  [`common/PluginLookup`](src/main/java/io/kroxylicious/filter/record/manipulation/common/PluginLookup.java)
  is a tiny lookup interface (shaped like `FilterFactoryContext.pluginInstance`, so a future real `Filter`
  integration is a drop-in swap), and each format's `buildOp` calls
  [`config/OpConfigs`](src/main/java/io/kroxylicious/filter/record/manipulation/config/OpConfigs.java)'s
  shared `resolveOp` helper to do the actual lookup-and-build — the one piece of logic that's genuinely
  identical across all three engines, rather than being copy-pasted three times. `resolveOp` also checks
  the resolved `TypedOp`'s declared input/output type against what the caller asked for, throwing
  `IllegalArgumentException` if a misconfigured/misnamed plugin doesn't produce the expected shape - the
  runtime counterpart of what a per-type interface used to catch at the (illusory, since
  `META-INF/services` registration itself was never compiler-checked either) "compile time" level.
  [`common/ServiceLoaderPluginLookup`](src/main/java/io/kroxylicious/filter/record/manipulation/common/ServiceLoaderPluginLookup.java)
  is a dependency-free `PluginLookup` (pure `java.util.ServiceLoader`, matching by simple class name) used by
  the `main()` demos and most tests, so this module's main code never has to depend on `kroxylicious-runtime`
  (where Kroxylicious's real `ServiceBasedPluginFactoryRegistry` lives) just to resolve its own bundled
  operations; a couple of tests (`PluginRegistrationTest`, `ProtoFunctionOpConfigTest`) deliberately use
  the real registry instead, as a check that these plugins would also resolve correctly once this module is
  wired into an actual `Filter`.
- `Delete` (see `DELETE_AND_INSERT_CONTENT` in `MaskPipelineTest`, and the delete-related notes under
  "Current state" below) is a reserved op *name*, not a plugin: its behaviour never varies by type (unlike
  every other operation, there's no real per-type logic to encapsulate), and whether it's legal at all is a
  property of the target format's container model (can it represent "this property is absent"?), not of the
  leaf type. A shared, name-keyed, JVM-wide plugin registry has no way to make an op resolvable from one
  format but not another, so each format's `buildOp` special-cases the literal name `Delete` itself before
  ever calling `PluginLookup` — Jackson returns the null-producing operation, Avro/Protobuf throw
  `IllegalArgumentException`.

## Current state

- **JSON** (`Use.java`, `jackson/`, `config/`): `SchemaConfig`/`OpConfig`-driven mask/generator builder
  (`JacksonFunction.buildMask` — fed a real value to mask, or `MissingNode` to generate). A field can
  compose more than one operation via `apply` (see `MaskPipelineTest`'s composed-chain tests), see
  [Pluggable operations](#pluggable-operations) above).
  Still open:
  - `apply` is mechanically available at object/array nodes too, not just leaves, but there's no
    object/array-level operation implemented in `common` yet, so it fails loudly rather than doing
    something silent and wrong.
  - Generation only consumes the *first* `apply` entry — composing multiple operations while generating
    from nothing (e.g. generate a random string, then hash it) is a real, separate enhancement, not yet
    done.
  - `SchemaConfig.type` is still a plain `String`; JSON Schema's type-union syntax (`type: [string,
    number]`) isn't supported, though the `SchemaConfig`/`OpConfig` split was chosen partly so that
    adding it later wouldn't require reshaping `apply` again.
  - `ContextPipeline`'s own composition check is currently *vacuous* for `apply` chains — every operation
    that exists today is type-preserving by construction (`HmacString`/`EncryptString`/`DecryptString`:
    string→string; `RandomString`/`ChooseString`/`ValueString` and their `Int` counterparts: produce their
    own field's type), so there's no way to build a chain that fails the check. This is an accepted
    simplification, not a gap to fix speculatively — it starts doing real work the day an operation that
    changes type is added (which the open plugin vocabulary makes more likely than when the operation set
    was closed and hand-reviewed as one file).
  - Deletion and insertion of an object property are supported: `apply: [{op: Delete}]` removes an
    existing property, and a generator-shaped `apply` entry (`ValueString`/`RandomString`/`ChooseString`,
    or their `Int` counterparts) on a property absent from the data inserts it (see `MaskPipelineTest`'s
    delete/insert tests). Insertion works at any
    depth, not just one level — a leaf several levels below an entirely-absent chain of ancestor objects
    still materializes, via `JacksonFunction.buildStructural`'s speculative recursion into a fresh empty
    object, collapsing back to absent only if nothing real came of it (so a genuinely-present object that
    ends up empty, e.g. from deleting all its properties, is never silently discarded — only a
    speculatively-materialized one is). `ObjectNodes.mapProperties` was reworked to build a fresh object
    rather than mutate in place, using Jackson's `MissingNode` as the "no value here" sentinel in both
    directions: fed to a declared-but-absent property's function (to support insertion), and returned by a
    function to mean "remove this" (to support deletion).
  - Still open: array element insertion/deletion (arrays have no per-slot generator concept to insert
    into), and `patternProperties`/`additionalProperties` selection (and what order they'd run in relative
    to `properties`, given operations are order-sensitive).
- **Avro** (`avro/`): `AvroFunction.buildMask` masks `record`/`array`/`string`/`int` values, built directly
  from a real `org.apache.avro.Schema` rather than a shadow config model — `Schema`/`Schema.Field` already
  preserve unrecognised JSON properties (`getObjectProp`), so the non-standard `apply` keyword round-trips
  through `Schema.Parser` for free (see `AvroSchemas`), exactly as `AvroUse.java` originally sketched
  ("let's just reuse the Avro schema... but add our own keywords"). `apply` sits as a sibling of a field's
  own `type`, or directly on a bare schema (e.g. an array's `items`, which is itself a schema and can carry
  its own extra properties). `AvroBinaryDeserializer`/`AvroBinarySerializer` and `AvroJsonDeserializer`/
  `AvroJsonSerializer` are the Avro equivalents of `jackson/JacksonDeserializer`/`JacksonSerializer`, for
  Avro's binary and JSON encodings respectively — unlike JSON, Avro binary data isn't self-describing, so
  both require a `Schema` up front.
  Masking only, unlike JSON's `JacksonFunction` (no generation-from-nothing): Avro requires every declared
  field to be present in a conforming record, so there's no "absent" starting point equivalent to Jackson's
  `MissingNode` to generate from — that needs Avro's union/default mechanism first, which is also why
  `Delete` isn't supported yet here (rejected as soon as it's named — see
  [Pluggable operations](#pluggable-operations) above — rather than silently producing a record that no
  longer conforms to its schema).
  Still open:
  - Unions and nullable fields (`type: [..., "null"]`) — `buildStructural`/`buildApplyChain` only handle a
    single concrete `Schema.Type` per node, the same simplification JSON's `SchemaConfig.type` currently
    makes for type-unions.
  - Every other Avro type: `map`, `enum`, `fixed`, `bytes`, `boolean`, `long`, `float`, `double`.
  - Generation and delete/insert, once union/default support exists to make them meaningful — or, an
    alternative to union/default support entirely: see "Divergent output schemas" below, which would make
    `Delete` meaningful without either.
- **Protobuf** (`protobuf/`): `ProtoFunction.buildMask` masks `message`/`repeated`/`string`/`int32` values,
  built from a `Descriptors.Descriptor` obtained from raw `.proto` IDL text via `ProtoSchemaParser`, which
  reuses `io.apicurio:apicurio-registry-protobuf-schema-utilities` (already a dependency of
  `kroxylicious-record-validation`, for the same "turn `.proto` text into a real descriptor" problem) rather
  than writing a `.proto` parser of our own — see `ProtoSchemaParser`'s javadoc for why depending on that
  over Square Wire directly, or writing a custom ANTLR grammar, was the better tradeoff here. Unlike Avro,
  Apicurio's conversion doesn't carry a custom option like `apply` through to the built descriptor (it only
  translates a fixed list of well-known protobuf option names), so `ProtoSchemaParser` separately walks the
  same parsed AST itself to read `apply` off a field's/message's `option (apply) = {...}` declaration,
  keeping the result alongside the descriptor in a `ParsedProtoSchema`.
  Protobuf's `repeated` fields have no separate node to hang a per-element `apply` chain off the way Avro's
  array `items` schema does (repeated-ness and element type live on one `FieldDescriptor`), so `apply` on a
  repeated field is deliberately interpreted as per-element, not whole-list — a Protobuf-specific choice
  forced by its schema shape, documented on `ProtoFunction`.
  A real, non-obvious gotcha worth knowing before extending this: `DynamicMessage.getField(FieldDescriptor)`
  is checked against the exact `Descriptor` build a `FieldDescriptor` came from, unlike Avro's
  name-based `GenericRecord.get(String)` — a deserializer and the mask function it feeds must be built from
  the *same* `ParsedProtoSchema`, even when two schemas are structurally identical (e.g. a mask schema and
  its `encrypt`→`decrypt` unmask counterpart), or every field access throws `IllegalArgumentException`
  ("FieldDescriptor does not match message type"). See `ProtoUse`'s comment for a worked example.
  Masking only, like Avro, and for the same underlying reason once you look past the surface difference:
  Protobuf fields *do* track presence (`FieldDescriptor.hasPresence()`/`DynamicMessage.hasField()`) far more
  naturally than Avro's always-required fields do, so `ProtoMessages` already carries an absent field through
  as absent rather than manufacturing a false presence — but `Delete` is rejected here too, the same as Avro
  (see [Pluggable operations](#pluggable-operations) above for why), since removing a field isn't meaningful
  without deciding what that means for a required proto2/proto3 implicit-presence field.
  Still open:
  - `oneof` (individual member fields already work like ordinary optional fields, since `DynamicMessage`
    doesn't distinguish oneof membership at the reflection API level used here — but nothing yet models the
    "exactly one of" semantics as a concept), `map<K,V>` (desugars to a synthetic `repeated MapEntry`
    message at the descriptor level, so this is "don't special-case it away" more than new plumbing),
    every other scalar type (`int64`, `bool`, `bytes`, `double`, `float`, fixed variants), enums,
    `google.protobuf.Any`/well-known wrapper types, extensions, multi-file `import` (Apicurio's utilities
    support a `dependencies` map for this; unused so far), the newer "Editions" syntax (not supported by
    Apicurio's parser as of this writing).
  - Delete/insert, once removing a required field has defined semantics — Protobuf's wire format (fields
    tagged by number, absence already idiomatic) makes "Divergent output schemas" below arguably even less
    friction here than for Avro.
- **`common`**: format-agnostic primitives (suppliers/functions for constant, random, and choose-from-a-set
  values across `String`/`int`/`long`/`double`, plus `HmacStringFunction`/`EncryptStringFunction`/
  `DecryptStringFunction`, and a standalone `RegexReplaceStringFunction` not yet exposed as a pluggable op —
  see "Theme: More functions" below), the `OpFactory`/`TypedOp`/`PluginLookup` machinery described under
  [Pluggable operations](#pluggable-operations) above, plus `ContextPipeline` and its `Context`-free
  sibling `Pipeline`, each of which validates that a list of functions compose and then runs them as a
  chain. The HMAC/encrypt/decrypt operations are each their own small, concrete `BiFunction<String,
  Context, String>` class (rather than one bundled utility) mainly so their crypto setup logic is
  independently reusable/testable - `ContextPipeline` itself no longer needs a stage to be a named class
  to use it as one, since `TypedOp.of(...)` attaches a stage's input/output type explicitly rather than by
  reflecting on its declared type, so a plain lambda works there too.
  This is the part of the module with the most unit test coverage so far.

## Divergent output schemas

Today, one schema does double duty: it's both the *selector* that drives `buildStructural`'s recursion and
the *write contract* the masked value must conform to. That's why `Delete` is rejected outright for Avro and
Protobuf (`AvroFunction`/`ProtoFunction`'s `buildOp`) — removing a required field would
produce a value that no longer matches the one schema doing both jobs. Splitting those two roles — letting the
*output* schema differ from the *input* schema — is worth designing towards, for two reasons: it's a more
direct route to a meaningful `Delete` than waiting on Avro union/default support, and it lets a masked view
hide a field's *existence*, not just its value, which is a materially stronger guarantee for a subject who
shouldn't know a broader dataset exists at all.

**Per format:**

- **Avro — yes, and it's the natural fit.** Avro binary isn't self-describing; a reader always needs a schema
  out-of-band (a compile-time `.avsc`, or a registry ID on the wire), and Avro's own schema-resolution
  algorithm already assumes reader and writer schema can differ, matching fields *by name* (with `aliases`).
  `GenericDatumWriter` only validates against the schema it's given, not against whatever schema produced the
  input — so building a fresh `GenericRecord` against an *output* `Schema`, populating each output field from
  the correspondingly-named input field (or a generator, or omitting it), needs no defaults or unions at all.
  The blocker today is purely that one `Schema` object is reused for both jobs, not anything about Avro's wire
  format.
- **Protobuf — yes, and arguably even more natural.** Protobuf's wire format tags every field with its number,
  so a message isn't positionally tied to one descriptor the way Avro binary is — the `DynamicMessage`
  strictness that currently forces `ProtoMessages`/`ProtoFunction` to use the *exact* input `Descriptor` (see
  the gotcha noted above) is a `DynamicMessage` API restriction, not a wire-format one. Building the output via
  a fresh `DynamicMessage.Builder` from the *output* `Descriptor`, matching fields **by number** (Protobuf's
  compatibility model is number-based, unlike Avro's name-based one), makes deleting a field trivial: proto3
  already treats absence as normal, so there's no "required field must be present" problem to solve.
- **JSON — the question doesn't really apply the same way.** There's no wire-level write schema for JSON
  today; `JacksonFunction` already doesn't enforce type preservation ("`ContextPipeline`'s own composition
  check is currently vacuous for `apply` chains", above), and `Delete`/insert are already implemented. An
  "output schema" for JSON would only matter as an optional validation/documentation artifact (e.g. what to
  register as the topic's JSON Schema afterwards), not as something the write path itself needs to conform
  to.

**Could the output schema be inferred from Java type information?** Two differently-sized versions of this
question:

- *General* Java-POJO-to-schema inference (as Avro's own `ReflectData` does) is genuinely ambiguous:
  nullability (is a nullable field a union with `null`, or non-null?), collection element types, `Map` key
  types (Avro maps require string keys), enums vs. arbitrary classes, logical types (`LocalDate`/`BigDecimal`
  need explicit annotations to disambiguate), and reflection field order being unreliable without a pinning
  annotation. This module doesn't have that problem in the general form, because record data here is never an
  arbitrary Java POJO — it's `GenericRecord`/`DynamicMessage`/`JsonNode`, already dynamically typed against a
  schema that's present.
- *Specific* to this codebase, a much smaller and already-mostly-solved version of the same idea exists:
  every operation carries its own input/output type as an explicit `TypedOp`/`TypeToken` (see
  [Pluggable operations](#pluggable-operations) above), which `ContextPipeline` already reads to validate
  composition and, optionally, type preservation - so the Java type of a field's *final* `apply`-chain
  output is already known at build time, for free. This stays true even though the operation *vocabulary*
  itself is open and pluggable, and even though nothing in the type system stops a plugin from declaring an
  arbitrary `OpFactory<T, R>`: each format engine's `buildOp` call site still only ever asks
  `OpConfigs.resolveOp` for one of a small, closed set of `Class` tokens per schema-type case (`String` for
  a `"string"` field, `Integer` for an `"integer"` field, and so on), and `resolveOp` rejects a resolved
  operation whose declared type doesn't match what was asked for - so a new operation can't actually
  introduce a new Java type into an existing field's chain, even though it technically *could* declare one.
  A small, closed mapping table (`String` → Avro `string`/proto `string`, `Integer` → `int`/`int32`, and so
  on — closed because the small set of *primitive types* an engine's `buildOp` call sites index by is
  closed, not because the set of operations is) would let the engine *detect* when a field's `apply` chain
  changes its type relative to the input schema, and derive an output schema by copying the input schema's
  fields — in their original order, minus any deleted ones — substituting types only where they diverge.
  That's a much safer starting point than open-ended Java reflection: deterministic, bounded, and built from
  machinery that already exists.

**Other considerations, not yet designed for:**

- **Wire schema identity has to be re-established.** Avro binary and registry-based Protobuf both rely on an
  out-of-band pointer to the exact schema (a Confluent/Apicurio magic-byte-prefixed schema ID, or a header —
  see `kroxylicious-record-validation`'s `AbstractSchemaBytebufValidator`, which already parses this via
  Apicurio's `IdHandler`/`HeadersHandler`, for *validation*, not rewriting). If the output schema differs from
  the one referenced on the wire, that pointer must be rewritten to point at a *registered* output schema, or
  the consumer will misdecode. This is the real prerequisite work — see "Theme: Schema Registry integration"
  below.
- **This is a per-consumer/per-policy concern, not a static one.** The motivating use case — hiding a field's
  existence from a subject who shouldn't know a broader schema exists — implies the output schema can vary by
  *who's asking* (see the client-subject-conditional Filter config sketched under "Theme: An actual Filter").
  That means potentially many output schemas need to be built/registered/cached per input schema, not one,
  with real hot-path cost implications (no blocking registry calls per record).
- **Registry compatibility modes can reject this.** If a topic's registry subject has BACKWARD/FORWARD/FULL
  compatibility configured, registering a schema that drops a field a consumer's reader schema requires
  (without a default) may simply be refused, or may silently break a consumer's decode. A masked view likely
  needs its own registry subject (or subject-naming-strategy), distinct from the source data's own evolution
  history, so masking doesn't pollute the "real" schema's compatibility lineage.
- **Determinism, if any part of the output schema is derived rather than hand-authored.** Registries key
  schemas by exact content/fingerprint; a derivation that isn't perfectly stable across restarts (e.g. relying
  on hash-map iteration order, or Java reflection field order) would register a new schema every restart even
  when nothing semantically changed. Deriving field order from the *input* schema's own declared order (both
  engines already build their field mappings via `LinkedHashMap`) rather than from any Java-side reflection
  sidesteps this.
- **Field matching strategy differs by format and mustn't be conflated.** Avro resolves reader/writer fields by
  *name* (with `aliases`); Protobuf's compatibility model is by field *number*. A shared "build output value
  from output schema + input value" helper can't reuse one matching strategy for both.
- **Delete is one-way; encrypt/hmac aren't.** The `EncryptString`→`DecryptString` unmask pattern above needs
  matching schemas on both legs. If `Delete` genuinely drops data rather than hiding it in one view, there's
  no inverse — worth stating explicitly so it isn't assumed reversible the way encryption is.
- **Termination.** The proof below rests on the recursion only ever walking one schema's own declared
  structure. A dual-schema walk needs the same argument re-made for whatever input/output matching strategy is
  chosen — still trivially true if both schemas are finite and matching is a direct lookup rather than a
  search, but the proof should say so explicitly rather than silently stop being accurate.
- **Process implications, once this leaves "experimental."** Nothing here needs a design proposal today, since
  this module isn't wired into a `Filter` yet. But per the project's API-change rules, once it is, both a new
  `outputSchema`-shaped filter YAML config surface and any new plugin-facing Java interfaces around output
  schema construction would count as public API changes requiring the `kroxylicious/design` proposal process —
  worth flagging now so it isn't a surprise later.

## Termination

Masking a record, or generating one, must be guaranteed to terminate — a malformed or adversarial record
should never be able to hang the proxy. For the literal question "does it ever halt", that guarantee rests
on exactly two conditions, and they are both necessary and sufficient:

1. **None of the functions this module defines contain an unbounded loop or unbounded recursion.** Every
   primitive in `common` either does fixed-size work (a single HMAC/cipher operation) or loops a number of
   times bounded by a config-declared, finite quantity (`RandomInt`/`RandomString`'s `minInclusive`/
   `maxExclusive`/`minLengthInclusive`/`maxLengthExclusive`, `ChooseInt`/`ChooseString`'s finite set,
   `Pipeline`/`ContextPipeline`'s fixed-size stage list). The only *recursion* anywhere is `JacksonFunction`
   following the `SchemaConfig` tree's own `properties`/`items` structure, and `ObjectNodes`/`ArrayNodes`
   iterating the data actually present at each node — both bounded by whatever they're recursing over, never
   by anything unbounded. This has to stay true for every future operation added to `common`: a new op must
   never take a config-declared parameter that could drive an unbounded internal loop. Now that the operation
   vocabulary is an open, pluggable set (see [Pluggable operations](#pluggable-operations) above) rather than
   a closed one reviewed as a single file, this is a contract on every plugin author, not something this
   module can enforce mechanically — the same way Kroxylicious already trusts filter/transform plugin authors
   generally (see the project's own security threat model around "plugin developers who might violate
   contracts").
2. **The input being walked is a genuine tree — finite, and free of cycles.** For plain JSON this is true by
   construction: the JSON grammar has no way for one part of a document to reference another, so a parsed
   `JsonNode` tree's size is always linear in its own serialized length. This is what matters for the record
   *data* this module masks, which is exactly why that data should always be parsed as plain JSON rather
   than YAML (see the caveat below) — with condition 1 already holding, a finite, acyclic input is what
   makes `buildMask`'s recursion over the schema, and the built function's later recursion over the data,
   both terminate. Nothing added for delete/insert changes this: the speculative-materialization recursion
   (`JacksonFunction.buildStructural`'s object case) only ever visits the schema's own declared properties,
   the same bound that already applied.

**Caveat: "finite and acyclic" is necessary but not automatically cheap.** YAML (used for config in this
module's demos, via `YAMLMapper`) supports anchors and aliases, which let a small amount of text expand into
an enormous — but still finite and acyclic — in-memory tree (the same "billion laughs" pattern well known
from XML). That satisfies condition 2 to the letter while defeating its purpose: the process would still
technically halt, just not within any useful time or memory. Plain JSON has no equivalent construct, so this
risk doesn't apply to record data parsed as JSON; it's specifically a YAML-authored-config concern, worth
remembering if config authoring or distribution ever becomes less trusted than "whoever operates the proxy."
A related but distinct practical concern: this is a plain recursive-descent implementation with no depth
limit or trampolining, so a finite, non-exploding but very *deeply nested* document (e.g. arrays nested tens
of thousands of levels deep) can still exhaust the JVM stack — not non-termination in the strict sense, but
the same practical failure mode.

## Key management

`HmacStringFunction`/`EncryptStringFunction`/`DecryptStringFunction` use a raw key passed in by the caller —
there is no key management integration yet. Their `HmacString`/`EncryptString`/`DecryptString` plugin
wrappers already accept a `keyId` config property (matching what a real key-management integration would
need to select a key by), but don't yet consume it for the same reason. See `kroxylicious-record-encryption`
for the project's existing KMS integration (`kroxylicious-kms`) if/when this module needs real key
management.


## TODO

### Theme: make more sympathetic to Json Schema:
- `patternProperties`/`additionalProperties` selection — the original complaint that kicked
  off this whole design exercise. Still open.
- Array element insertion/deletion — needs a design pass first; no precedent for what
  "insert into an array" means yet.
- Real type-union support on `SchemaConfig.type` (currently a plain `String`).
- Understand JsonSchema better -- e.g. `$refs`, and the way linking works.

### Theme: generalize to other schema languages
- Avro: `record`/`array`/`string`/`int` masking now works (`avro/AvroFunction`, built directly
  from a real `org.apache.avro.Schema` rather than a shadow config model — see the module
  README). Still open:
    - Unions/nullability (`type: [..., "null"]`) — needs a design pass, same as JSON's own
      still-open type-union support above.
    - Every other Avro type: `map`, `enum`, `fixed`, `bytes`, `boolean`, `long`, `float`, `double`.
    - Generation and delete/insert of fields — meaningless without union/default support first,
      so currently fail loudly rather than silently producing non-conforming records.
    - Wiring into a real `Filter` (see "Theme: An actual Filter" below) — applies equally to JSON.
- Protobuf: `message`/`repeated`/`string`/`int32` masking now works (`protobuf/ProtoFunction`, built from a
  real `com.google.protobuf.Descriptors.Descriptor` parsed from raw `.proto` text via `ProtoSchemaParser` -
  see the module README's "Current state" section for the design, including the deliberate departures from
  the Avro precedent that Protobuf's schema shape forced). Still open: `oneof`, `map`, every other scalar
  type, enums, `Any`/well-known wrappers, extensions, multi-file `import`, Editions syntax, delete/insert -
  see "Current state" for why each is deferred rather than merely unimplemented.
- Make data formats a pluggable abstraction.

### Theme: An actual Filter
- Write the code to turn this into a real `Filter`
- Figure out the Filter's configuration. This gets tricky, because once you're beyond demo-ware you need to treat masks themselves as entities, and so then you want schema registry support.
    ```yaml
    - topic:
        value: bill-events
      apply: 
      - if: 
          recordHeader: # ...
          headerKey: 
            value: my-header
          headerValue: 
            value: blah # optional
        then:
      recordKey: # ...
      - if: 
          recordValue:
          - hasSchemaId: 
              value: 123
              atLocation: prefix # optional, if we want to support apicurio headers, but default to the confluent 4 byte prefix
        then: 
          apply:
            - schemaValidation: 
            - signatureValidation
            - if: 
                recordDirection: out # in=Produce, out=Fetch (and ShareFetch), in,out=both and out,in (e.g. useful for symmetric transformation like record- and field-level encryption)
                clientSubject:
                  value: 
                    principal: User
                    name: BillingTeam
                clientId: 
                  value: billing-app
              then:     
                - mask:
            - recordEncryption:
            - compression: 
    ````

### Theme: More functions
- A general find/replace for `String` data — the primitive itself already exists
  (`common/RegexReplaceStringFunction`, supporting both `replaceAll`/`replaceFirst` and, for each, either a
  plain replacement string or another `BiFunction<String, Context, String>` applied to each captured group
  before interpolation - see
  `RegexReplaceStringFunctionTest`), but it isn't yet exposed as a pluggable `apply` op. Still open: a
  `RegexReplaceString` plugin wrapping it, following the same pattern as `HmacString`/`EncryptString`.
- A function with `try`/`catch` -like semantics.
- E.g. add an absolute/relative error to a number value
- LocalDate, LocalDateTime, LocalTime, ZonedDateTime, Instant, Duration, etc.

### Theme: Schema Registry integration

The concrete prerequisite for "Divergent output schemas" (above) to work against real Avro/Protobuf traffic:
- Registering a derived/authored output schema with the registry, and caching the resulting schema ID
  (must not be a per-record blocking call).
- Rewriting the wire-visible schema-ID pointer (magic-byte body prefix or header — see
  `kroxylicious-record-validation`'s `AbstractSchemaBytebufValidator`/Apicurio `IdHandler`/`HeadersHandler` for
  the existing parsing-side precedent) to point at the registered output schema instead of the input one.
- Deciding how a masked view's output schema is namespaced in the registry (its own subject/subject-naming
  strategy) so it doesn't get folded into the source schema's own compatibility-mode evolution history.

### Theme: Tech debt

- root-level `apply: [{op: Delete}]` semantics (undefined — "delete the whole record" is a
  bigger question),
- YAML anchor/alias blow-up guard (just don't allow YAML, only JSON)
- Recursion depth limit (avoid StackOverflowException on deeply nested data)
