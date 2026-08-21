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
  - Deletion and insertion of an object property are supported: `apply: [{delete: true}]` removes an
    existing property, and a generator-shaped `apply` entry (`value`/`random`/`choose`) on a property
    absent from the data inserts it (see `MaskPipelineTest`'s delete/insert tests). Insertion works at any
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

## Termination

Masking a record, or generating one, must be guaranteed to terminate — a malformed or adversarial record
should never be able to hang the proxy. For the literal question "does it ever halt", that guarantee rests
on exactly two conditions, and they are both necessary and sufficient:

1. **None of the functions this module defines contain an unbounded loop or unbounded recursion.** Every
   primitive in `common` either does fixed-size work (a single HMAC/cipher operation) or loops a number of
   times bounded by a config-declared, finite quantity (`random`'s `min`/`max`/`minLength`/`maxLength`,
   `choose`'s finite set, `Pipeline`'s fixed-size stage list). The only *recursion* anywhere is
   `JacksonFunction`/`JacksonSupplier` following the `SchemaConfig` tree's own `properties`/`items`
   structure, and `ObjectNodes`/`ArrayNodes` iterating the data actually present at each node — both bounded
   by whatever they're recursing over, never by anything unbounded. This has to stay true for every future
   operation added to `common`: a new op must never take a config-declared parameter that could drive an
   unbounded internal loop.
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
there is no key management integration yet. See `kroxylicious-record-encryption` for the project's existing
KMS integration (`kroxylicious-kms`) if/when this module needs real key management.
