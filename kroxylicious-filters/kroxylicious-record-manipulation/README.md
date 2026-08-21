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
  value is a *list* of operations (`value`, `random`, `choose`, `hmac`, `encrypt`, `decrypt` — see the
  `common` package), composed in declared order via
  [`common/Pipeline`](src/main/java/io/kroxylicious/filter/record/manipulation/common/Pipeline.java).

These have to be kept apart because JSON Schema's own keywords are validation *predicates* — they're ANDed
together, side-effect-free, and order-independent by design (a value either satisfies all of them or it
doesn't). A sequence of transformations has none of those properties: encrypting then hashing a value gives
a different result than hashing then encrypting it. Folding "what operation to run" into the same keyword
that decides "does this schema match" doesn't generalise to that, so `apply` is its own explicitly-ordered,
composable list, kept distinct from the structural keywords that select where it runs.

For JSON, this split is visible directly in the config model:
[`config/SchemaConfig`](src/main/java/io/kroxylicious/filter/record/manipulation/config/SchemaConfig.java)
represents the schema-shaped part (`type`/`properties`/`items`/`apply`), and tolerates *any* other real JSON
Schema keyword (`pattern`, `contains`, `minLength`, ...) via a `@JsonAnySetter`/`@JsonAnyGetter` catch-all
instead of failing to parse — the goal is that an existing JSON Schema document can have `apply` added to it
directly, not that this module has to model JSON Schema's entire vocabulary.
[`config/ApplyConfig`](src/main/java/io/kroxylicious/filter/record/manipulation/config/ApplyConfig.java) is
just the flat operation vocabulary that goes inside `apply`'s list. It deliberately doesn't carry `type`,
`properties`, or `items` itself, so an operation can't be forced into a type-specific shape that would
foreclose a field ever having a JSON Schema type union (`type: [string, number]`).

Each data format still gets its own config model and its own code to walk it — there isn't one grammar
shared across formats, since each format has its own type system (see `avro/AvroUse.java`, sketch only).
What *is* shared is the small set of primitive transformations in `common`, and the pattern of building a
`Function<Node, Node>` (to mask/transform existing data) or a `Supplier<Node>` (to generate data from
nothing) from the config tree. Format-specific adapters (see `jackson/Jackson.java`) bridge the
format-agnostic `common` primitives onto the format's native node types (e.g. Jackson's `JsonNode`).

Stages built this way are given their own named types —
[`jackson/JacksonFunction`](src/main/java/io/kroxylicious/filter/record/manipulation/jackson/JacksonFunction.java)
(`Function<JsonNode, JsonNode>`) and
[`jackson/JacksonSupplier`](src/main/java/io/kroxylicious/filter/record/manipulation/jackson/JacksonSupplier.java)
(`Supplier<JsonNode>`) — rather than being plain `Function<JsonNode, JsonNode>`/`Supplier<JsonNode>` values.
That matters for `Pipeline`, which validates and runs a chain of stages by reflecting on each stage's
*concrete* generic type: a lambda assigned directly to `Function<JsonNode, JsonNode>` erases its type
arguments at runtime, whereas one assigned to a named subinterface with the type arguments fixed does not,
since the parameterization lives on the interface declaration rather than the lambda. `Pipeline` is used at
two levels: `Use.java` composes a `JacksonDeserializer`, a built mask/unmask `JacksonFunction`, and a
`JacksonSerializer` into one whole-record `Pipeline`; `JacksonFunction.buildMask` also builds a smaller,
per-field `Pipeline` out of a field's own `apply` list, so `apply: [encrypt, hmac]` on one field genuinely
composes two `common` classes (`EncryptStringFunction`, `HmacStringFunction`) in the declared order.

## Current state

- **JSON** (`Use.java`, `jackson/`, `config/`): `SchemaConfig`/`ApplyConfig`-driven mask/generator builders
  (`JacksonFunction.buildMask`, `JacksonSupplier.buildGenerator`). A field can now compose more than one
  operation via `apply` (see `MaskPipelineTest`'s composed-chain tests) — this was the main gap in the
  previous design, where a field picked exactly one of `value`/`random`/`choose`/`hmac`/`encrypt`/`decrypt`.
  Still open:
  - `apply` is mechanically available at object/array nodes too, not just leaves, but there's no
    object/array-level operation implemented in `common` yet, so it fails loudly rather than doing
    something silent and wrong.
  - Generation (`JacksonSupplier`) only consumes the *first* `apply` entry — composing multiple operations
    while generating from nothing (e.g. generate a random string, then hash it) is a real, separate
    enhancement, not yet done.
  - `SchemaConfig.type` is still a plain `String`; JSON Schema's type-union syntax (`type: [string,
    number]`) isn't supported, though the `SchemaConfig`/`ApplyConfig` split was chosen partly so that
    adding it later wouldn't require reshaping `apply` again.
  - `Pipeline`'s own composition check is currently *vacuous* for `apply` chains — every operation that
    exists today is type-preserving by construction (`hmac`/`encrypt`/`decrypt`: string→string;
    `random`/`choose`/`value`: produce their own field's type), so there's no way to build a chain that
    fails the check. This is an accepted simplification, not a gap to fix speculatively — it starts doing
    real work the day an operation that changes type is added.
  - Deleting or inserting a property, and `patternProperties`/`additionalProperties` selection (and what
    order they'd run in relative to `properties`, given operations are order-sensitive), are still open;
    neither has a config representation yet.
- **Avro** (`avro/`): sketch only. No config model or builder yet — `AvroUse.java` just explores what the
  mask syntax might look like. Avro's requirement that data stay decodable under a schema, and its built-in
  union/nullable types, mean the open questions above will need a proper Avro-specific answer, not just a
  JSON-shaped one.
- **`common`**: format-agnostic primitives (suppliers/functions for constant, random, and choose-from-a-set
  values across `String`/`int`/`long`/`double`, plus `HmacStringFunction`/`EncryptStringFunction`/
  `DecryptStringFunction`), plus `Pipeline`, which validates that a list of functions compose and then runs
  them as a chain. The HMAC/encrypt/decrypt operations are each their own small, concrete
  `Function<String, String>` class (rather than one bundled utility) specifically so they can be used
  directly as `Pipeline` stages — `Pipeline` needs each stage's *concrete* generic type to reflect on, which
  a named class reliably provides and a bundled method returning a lambda does not. This is the part of the
  module with the most unit test coverage so far.

## Key management

`HmacStringFunction`/`EncryptStringFunction`/`DecryptStringFunction` use a raw key passed in by the caller —
there is no key management integration yet. See `kroxylicious-record-encryption` for the project's existing
KMS integration (`kroxylicious-kms`) if/when this module needs real key management.
