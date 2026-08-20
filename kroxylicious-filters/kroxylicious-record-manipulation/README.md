# Record Manipulation Filter

**Status: experimental.** This module is not yet wired into the Kroxylicious filter framework (no
`Filter`/`FilterFactory`, no `META-INF/services` entry). It is a set of building blocks, driven by
`main()`-based demos, for a future filter that transforms record data — masking/redaction being the
motivating use case, but the design is meant to be more general (e.g. synthetic data generation).

## Idea

A mask is described in a syntax that borrows the *shape* of the data format's own schema language:
JSON-Schema-like keywords (`type`, `properties`, `items`) for JSON ([`Use.java`](src/main/java/io/kroxylicious/filter/record/manipulation/Use.java)),
and Avro schema syntax for Avro ([`avro/AvroUse.java`](src/main/java/io/kroxylicious/filter/record/manipulation/avro/AvroUse.java), sketch only).

Each data format has its own type system, so there isn't one grammar shared across formats — each format
gets its own config model and its own code to walk it. What *is* shared is the small set of primitive
transformations (constant value, random value, choose-from-a-set, HMAC, encrypt/decrypt — see the
`common` package) and the pattern of building a `Function<Node, Node>` (to mask/transform existing data)
or a `Supplier<Node>` (to generate data from nothing) from the config tree, then applying it. Format-specific
adapters (see `jackson/Jackson.java`) bridge the format-agnostic `common` primitives onto the format's
native node types (e.g. Jackson's `JsonNode`).

Stages built this way are given their own named types — [`jackson/JacksonFunction`](src/main/java/io/kroxylicious/filter/record/manipulation/jackson/JacksonFunction.java)
(`Function<JsonNode, JsonNode>`) and [`jackson/JacksonSupplier`](src/main/java/io/kroxylicious/filter/record/manipulation/jackson/JacksonSupplier.java)
(`Supplier<JsonNode>`) — rather than being plain `Function<JsonNode, JsonNode>`/`Supplier<JsonNode>`
values. That matters for [`common/Pipeline`](src/main/java/io/kroxylicious/filter/record/manipulation/common/Pipeline.java),
which validates and runs a chain of stages by reflecting on each stage's *concrete* generic type: a lambda
assigned directly to `Function<JsonNode, JsonNode>` erases its type arguments at runtime, whereas one
assigned to a named subinterface with the type arguments fixed does not, since the parameterization lives
on the interface declaration rather than the lambda. `Use.java` demonstrates this end to end: a
`JacksonDeserializer`, a built mask/unmask `JacksonFunction`, and a `JacksonSerializer` are composed into
one `Pipeline`, so `pipeline.apply(bytes)` deserializes, masks, and re-serializes a record in one call.

## Current state

- **JSON** (`Use.java`, `jackson/`, `config/`): the only format with a working config model
  (`MaskConfig`) and mask/generator builders (`JacksonFunction.buildMask`, `JacksonSupplier.buildGenerator`).
  Still rough — see the `TODO`s in `Use.java` for open questions, in particular how `type` should behave
  when a field could take more than one JSON type, and how composing *multiple* operations on one field
  should work (today a field's mask config picks exactly one of `value`/`random`/`choose`/`hmac`/`encrypt`/`decrypt`;
  `Pipeline` composes whole stages of a record's processing, not multiple operations on a single field).
- **Avro** (`avro/`): sketch only. No config model or builder yet — `AvroUse.java` just explores what
  the mask syntax might look like.
- **`common`**: format-agnostic primitives (suppliers/functions for constant, random, and choose-from-a-set
  values across `String`/`int`/`long`/`double`, plus HMAC/encrypt/decrypt via `Strings`), plus `Pipeline`,
  which validates that a list of functions compose and then runs them as a chain. This is the only part of
  the module with unit test coverage so far.

## Key management

`Strings`' HMAC/encrypt/decrypt use a raw key passed in by the caller — there is no key management
integration yet. See `kroxylicious-record-encryption` for the project's existing KMS integration
(`kroxylicious-kms`) if/when this module needs real key management.
