# Design Exploration: A purpose-built record-manipulation API to replace `MemoryRecords`

## Context

Kroxylicious manipulates Kafka record batches found in `ProduceRequestData`, `FetchResponseData` and
`ShareFetchResponseData`. Today it does so through `MemoryRecords` (inherited from Apache Kafka) and a
thin abstraction on top, `RecordStream`
(`kroxylicious-kafka-message-tools/.../transform/RecordStream.java`).

Both are awkward:

- `MemoryRecords`/`MemoryRecordsBuilder` were designed for the *broker*, not a proxy. They carry
  concerns we don't have — a parallel `FileRecords` hierarchy, zero-copy-to-disk, v0/v1 up-conversion,
  log-compaction/delete-horizon logic, KRaft control-record builders. The proxy never reads or writes a
  log, so none of that serves us.
- `RecordStream` is synchronous and, more subtly, *lossy about intent*: its `RecordTransform` forces
  every transform to return an offset, timestamp, key, value **and** headers for every record (see
  `RecordStream.toMemoryRecords` at lines 187–192 and `RecordTransform`), so a transform that only
  rewrites a value still reports "new" offset/timestamp/key/headers. The writer therefore rebuilds every
  record from scratch and can never tell that a record — or a whole batch — was untouched.
- The real driver (captured in `PLAN.md`) is **async**: transforms increasingly need per-record lookups
  (schema from a registry, DEK from a KMS) that are naturally asynchronous. Today that's handled by
  multi-pass orchestration (`ManipulationOrchestrator`), which materialises decompressed data across
  passes.

Kafka's original constraint — two parallel `Records` implementations and zero-copy from network to
disk — does not apply to us. This doc explores whether writing our own representation (reusing only the
battle-tested byte-level codecs) yields a materially better API, and recommends a shape.

**Decision recorded with the user:** this session produces a *design exploration doc* (not an
implementation plan); the recommended strategy is a *new model that reuses low-level codecs*; and the
async model should be *bounded-window concurrency* (configurable window size), whose CPU/memory/parallelism
tradeoffs are called out below.

---

## Part 1 — What the record *format* actually constrains (vs. what the API imposes)

Separating the wire format's invariants from `MemoryRecords`' ergonomics is the whole point. The v2
format (KIP-98) is:

**Batch header** (fields, in order). CRC-32C covers everything **from `Attributes` onward**:

```
BaseOffset        int64   ┐ NOT covered by CRC
BatchLength       int32   │ (derived)
PartLeaderEpoch   int32   ┘ broker-owned; deliberately outside CRC (KIP-98)
Magic             int8      (=2; also outside CRC)
CRC               uint32    (derived, CRC-32C over Attributes..end)
Attributes        int16   ┐ compression(3b) | tsType(1b) | txn(1b) | control(1b) | deleteHorizon(1b)
LastOffsetDelta   int32   │
BaseTimestamp     int64   │
MaxTimestamp      int64   │ covered
ProducerId        int64   │ by
ProducerEpoch     int16   │ CRC
BaseSequence      int32   │
RecordCount       int32   ┘
Records           [Record]  (the whole block is optionally compressed as one unit)
```

**Record** (within the record block):

```
Length         varint   (derived)
Attributes     int8     (unused today, 0)
TimestampDelta varlong  = timestamp - BaseTimestamp
OffsetDelta    varint   = offset   - BaseOffset
KeyLen+Key     (KeyLen = -1 ⇒ null)
ValueLen+Value (ValueLen = -1 ⇒ null)
Headers[]      (count varint, then [keyLen,key,valueLen,value]*)
```

### The format-imposed couplings (these are *real*, independent of any API)

1. **Delta encoding couples records to the batch header.**
   `offset = BaseOffset + OffsetDelta`, `timestamp = BaseTimestamp + TimestampDelta`,
   and `sequence = BaseSequence + OffsetDelta`. You cannot change `BaseOffset`/`BaseTimestamp`/
   `BaseSequence` without rewriting every record's deltas. **But** keys, values and headers are stored
   as *raw, absolute* bytes — not delta-encoded — so rewriting them is completely decoupled from the
   header.
2. **CRC covers `Attributes..end`.** Any change to record bytes, counts, compression, timestamps or
   attributes forces a CRC recompute. Changes to `BaseOffset`, `BatchLength`, `Magic` and
   `PartitionLeaderEpoch` do **not** (KIP-98 put `partitionLeaderEpoch` outside the CRC on purpose so
   brokers can stamp it cheaply).
3. **Compression is batch-level, over the record block as a unit.** To read any record you must
   decompress the whole block; to change any record you must recompress the whole block. There is no
   per-record compression.
4. **EOS / idempotence invariants.** `ProducerId`, `ProducerEpoch`, `BaseSequence` must be preserved
   exactly, and the *range* of sequence numbers a batch spans must be preserved even if records are
   removed (why Kafka's compaction path calls `overrideLastOffset`). Renumbering sequences/offsets
   breaks broker duplicate-detection and producer fencing.
5. **Control batches/records are opaque and order-sensitive.** They carry transaction markers
   (`ABORT`/`COMMIT`) and KRaft messages. The proxy has no business transforming them and must not
   reorder them relative to data records.
6. **Legacy magic v0/v1** exist (no headers, wrapper-record compression). Modern clients use v2; the
   proxy must at least pass these through or reject them cleanly.

### The payoff: the proxy's *safe mutation surface* is tiny

Reducing the above, the only things a proxy legitimately changes are:

| Operation                         | Format-safe? | Cost                                   |
|-----------------------------------|:------------:|----------------------------------------|
| Replace record **value**          | ✅           | re-encode that record; recompress block if compressed; recompute CRC |
| Replace record **key**            | ✅           | as above                               |
| Add/replace/remove **headers**    | ✅           | as above                               |
| Change batch **compression**      | ✅           | recompress block; recompute CRC        |
| **Uniform timestamp shift** (every record +Δ) | ✅ header-only | rewrite `BaseTimestamp` & `MaxTimestamp` + CRC; **record block byte-identical — no decompression even when compressed** |
| **Uniform timestamp set** (every record = T) | ✅         | each record's `timestampDelta` → 0 (re-encode block; recompress if compressed) + CRC; key/value/headers untouched |
| Drop / add records                | ⚠️ constrained | must preserve sequence range in EOS batches; shifts offsets |
| offset / **arbitrary per-record** timestamp / producerId / epoch / sequence / control batches / partitionLeaderEpoch | ❌ pass through verbatim | — |

**This is the central insight.** Because the mutable surface is so small and everything else is
"preserve verbatim", we don't need `MemoryRecordsBuilder`'s full generality. We can model a batch as
*immutable framing + a sequence of records whose key/value/headers are individually replaceable*, and
make the illegal mutations **unrepresentable** (no setter for offset/sequence/producer fields). The
EOS/control invariants then hold *by construction* rather than by developer discipline.

### Timestamps: a narrow but useful band of safe transforms

Because `timestampDelta = timestamp − BaseTimestamp`, two *batch-wide* timestamp transforms are safe with
known encodings, while arbitrary per-record retiming is not:

- **Uniform shift (every record +Δ):** set `BaseTimestamp += Δ` and `MaxTimestamp += Δ`. Every per-record
  delta is unchanged, so the **record block is byte-identical** — a *header-only* mutation needing no
  decompression even for a compressed batch (just a CRC recompute). It is the cheapest possible
  non-trivial edit the format allows.
- **Uniform set (every record = T):** every delta becomes `0`; set `BaseTimestamp = MaxTimestamp = T`.
  This re-encodes the record block (each `timestampDelta` varlong → `0`) but never touches
  key/value/headers; for an uncompressed batch it is a per-record splice.

Anything else (a different Δ per record, clamping, bucketing, …) implies arbitrary deltas and is treated
as out of scope / unsafe. Crucially these are **batch-level** operations (they move the batch's
`BaseTimestamp`), *not* per-record edits — which drives the API shape in Part 3. Caveat: do not move
`BaseTimestamp` when the `deleteHorizon` attribute bit is set (there it encodes the delete horizon, not a
record time) — though the proxy should not see that on normal produce/fetch data batches.

### The efficiency opportunity the current model throws away

Delta encoding cuts in our favour: **if only key/value/headers change, `OffsetDelta`, `TimestampDelta`
and record `Attributes` are unchanged.** So:

- A record that isn't edited can be **copied byte-for-byte** (its whole encoded form is reusable).
- A batch in which **no** record is edited (and compression is unchanged) can be **memcpy'd wholesale** —
  header, CRC and all — for *zero* decode/encode/CRC work. Filters that match only some topics/records
  (e.g. record-manipulation's `NO_MATCH` fast path, multitenant) leave most batches untouched.
- Even in an edited uncompressed batch, unchanged records are byte-range splices; only edited records
  are re-encoded, and only CRC/length/count are recomputed.

`RecordStream`+`MemoryRecordsBuilder` cannot do any of this because the transform contract discards the
"unchanged" signal and the builder always rebuilds from exploded `Record`s.

---

## Part 2 — What we actually *do* with records (from the codebase)

Observed operations, roughly by frequency (callers: `kroxylicious-record-encryption`,
`kroxylicious-record-manipulation`, `kroxylicious-simple-transform`, the filter archetype):

- **Iterate** batches/records; read key/value/headers/offset/timestamp — ubiquitous.
- **Replace value** — the dominant edit (encryption, decryption, manipulation, simple-transform).
- **Add a header** (encryption stamps an encryption-version header) / **remove a header** (decryption,
  manipulation strip it).
- **Replace key** — occasional.
- **Change compression** — wanted, rarely exercised.
- **Per-record async dependency** before the edit — the hard case:
  - encryption/decryption: resolve DEK/EDEK via KMS (`InBandDecryptionManager` does a gather pass then a
    transform pass);
  - manipulation: resolve Avro/Proto schema by `GlobalId`/`ContentId` from a registry
    (`ManipulationOrchestrator`, 4 stages: gather → resolve → match → transform);
  - the 3-pass case in `PLAN.md`: schema → extract a field → pick KEK → encrypt.
- **Drop records** — not currently done through the stream API (and EOS-constrained).

Non-functional requirements the API must respect:
- Minimise decompress/recompress (CPU) and avoid materialising all decompressed/deserialised records at
  once (heap) — `PLAN.md`'s multi-pass approach costs ~4× for decompressed, more for deserialised.
- Reuse per-record scratch buffers (encryption already does this via `resetAfterTransform`).
- Everything hangs off the proxy's already-async filter pipeline, so the terminal operation returning a
  `CompletionStage` is natural.

---

## Part 3 — Recommended direction: a new model over reused codecs

**Build new:** the in-memory model, the lazy reader, the intent-preserving writer, and the async driver.
**Reuse (don't reinvent):** the fiddly, broker/client-exact byte machinery:

- `Crc32C`
- varint/varlong zig-zag (`ByteUtils`)
- compression codecs (`Compression#wrapForInput/wrapForOutput`: gzip/snappy/lz4/zstd)
- batch/record header *parsing* constants and logic (`DefaultRecordBatch`, `DefaultRecord`) for the
  read path.

These already live under `io.kroxylicious.kafka.common.record.internal` (an *internal* package), so
reusing them from a new public API is appropriate and keeps us byte-compatible with real brokers/clients.

### Sketch of the API shape (illustrative, not final)

The guiding principles: **framing is immutable except a closed set of safe batch-level transforms**
(compression, uniform timestamp shift/set), **record edits default to "keep"** (so the writer knows
what's pristine), **illegal mutations unrepresentable** (no offset/sequence/producer setters),
**lazy/zero-copy reads**, **async-capable terminal**.

```java
// Top-level handle replacing MemoryRecords: a lazily-parsed view over a buffer of batches.
interface RecordFrames {
    Iterable<BatchView> batches();          // lazy; control & empty batches included
}

// Immutable view of one batch. No setters for framing → EOS invariants safe by construction.
interface BatchView {
    long baseOffset(); long baseTimestamp(); long maxTimestamp();
    long producerId(); short producerEpoch(); int baseSequence(); int lastOffsetDelta();
    Compression compression(); boolean isControl(); boolean isTransactional();
    int partitionLeaderEpoch();
    Iterable<RecordView> records();         // decode on demand (decompresses lazily)
}

// Immutable, zero-copy view of a record: key()/value() are slices of the source buffer.
interface RecordView {
    long offset(); long timestamp(); int sequence();
    @Nullable ByteBuffer key(); @Nullable ByteBuffer value(); Header[] headers();
}

// A RecordEdit is an immutable, COMPOSABLE description of which fields change. Each field defaults to
// "isKeep the original"; you override only what you touch, so edits compose by chaining builder calls:
RecordEdit edit = RecordEdit.builder()
        .key(newKey)                              // replace key    (omit ⇒ isKeep original key)
        .value(newValue)                          // replace value  (omit ⇒ isKeep original value)
        .addHeader("encryption", versionBytes)    // header mutations accumulate...
        .removeHeader("schema-id")                // ...applied over the original headers in order
        .build();

RecordEdit.keep();                               // identity edit (nothing set) → enables byte copy
RecordEdit.builder().value(v).build();           // value-only (the common case)
// Nullability is explicit, so null never silently means "isKeep":
//   .value(buf) = replace · .clearValue() = tombstone (null) · (omit) = isKeep original
// Header ops compose too: .addHeader(..) · .removeHeader(name) · .headers(Header[]) (replace all)
// Crucially: NO setter for offset/sequence/producer fields (EOS-safe by construction).
// (isDrop()/append records are "advanced", EOS-gated — see Open Questions.)

// Timestamps are batch-level (they move BaseTimestamp), so they live on a BatchEdit, not RecordEdit:
BatchEdit batchEdit = BatchEdit.builder()
        .shiftTimestamps(deltaMillis)            // header-only when no record is edited (Part 1)
        .compression(Compression.ZSTD)           // or .setTimestamps(T)
        .build();

// Transform contracts — sync and async, over the same edit model:
interface RecordTransform      { RecordEdit transform(BatchView b, RecordView r); }
interface AsyncRecordTransform { CompletionStage<RecordEdit> transform(BatchView b, RecordView r); }
interface BatchTransform       { BatchEdit  transform(BatchView b); }   // optional; defaults to isKeep()

// Terminal operations. The async form drives a bounded window (Part 4) and returns a stage.
MemoryRecordsReplacement apply(RecordTransform t, ByteBufferOutputStream out);
CompletionStage<MemoryRecordsReplacement> applyAsync(AsyncRecordTransform t,
                                                     TransformOptions opts);  // opts.windowSize, ...
```

### Full builder surface (`RecordEdit` / `BatchEdit`)

```java
public final class RecordEdit {                      // immutable, composable; every field defaults to "isKeep"

    // ---- Factories ----
    public static RecordEdit keep();                 // identity → whole-record byte copy
    public static RecordEdit value(ByteBuffer v);    // shorthand for builder().value(v).build()
    public static RecordEdit drop();                 // EOS-gated sentinel — see Open Questions
    public static Builder builder();

    // ---- Introspection (lets the writer pick fast paths; handy in tests) ----
    public boolean isKeep();
    public boolean isDrop();
    public boolean editsKey();
    public boolean editsValue();
    public boolean editsHeaders();

    public static final class Builder {
        // KEY     — omit = isKeep · key(buf) = replace · clearKey() = set null
        public Builder key(ByteBuffer newKey);
        public Builder clearKey();

        // VALUE   — omit = isKeep · value(buf) = replace · clearValue() = tombstone (null)
        public Builder value(ByteBuffer newValue);
        public Builder clearValue();

        // HEADERS — omit = isKeep. Ops apply in call order over the original header list;
        //           setHeaders/clearHeaders reset the working list, add/remove patch it.
        public Builder addHeader(String key, @Nullable ByteBuffer value);
        public Builder addHeader(Header header);
        public Builder removeHeader(String key);     // removes ALL headers with this key
        public Builder setHeaders(List<Header> headers);
        public Builder clearHeaders();               // == setHeaders(List.of())

        public RecordEdit build();
    }
}

public final class BatchEdit {                       // immutable; batch framing is otherwise fixed

    public static BatchEdit keep();
    public static Builder builder();

    public boolean isKeep();
    public boolean editsCompression();
    public boolean editsTimestamps();

    public static final class Builder {
        // COMPRESSION — omit = isKeep source codec; set to recompress the block on write
        public Builder compression(Compression compression);

        // TIMESTAMPS  — omit = isKeep. shift/set are mutually exclusive (build() rejects both).
        public Builder shiftTimestamps(long deltaMillis);   // header-only when all records isKeep()
        public Builder setTimestamps(long timestampMillis); // every record's delta → 0

        public BatchEdit build();
    }
}
```

**Semantics worth pinning down (future-proposal territory, but decided here in principle):**

- **Field default = keep, everywhere.** Only explicitly-set fields change — this is what preserves the
  writer's per-field copy fast paths and makes `keep()` the trivially-detected identity.
- **Nullability is never overloaded to mean "keep".** Keep vs. replace-with-null are distinct calls
  (`clearKey`/`clearValue` vs. omission).
- **Header ops are ordered and patch-over-original.** `removeHeader(key)` drops *all* matching headers
  (Kafka permits duplicate keys); `addHeader` appends; `setHeaders`/`clearHeaders` reset the working list
  so later `add`/`remove` compose predictably.
- **Timestamp shift vs. set are mutually exclusive**, and `build()` rejects supplying both.
- **Buffer ownership/lifecycle.** A `RecordEdit` *references* the buffers it is given; they must stay
  valid until the record is serialised. In the async window (Part 4) a pooled buffer therefore cannot be
  recycled until its record is emitted — the framework owns that signal, replacing today's
  `RecordTransform.resetAfterTransform` hook.
- **Combining batch + record edits.** The terminal operation takes an (optional) `BatchTransform` *and* a
  `RecordTransform`; batch-level and per-record edits are independent axes that compose (e.g. recompress
  the batch *and* rewrite each value).

The writer implements the fast paths from Part 1: whole-batch memcpy when every record returns `keep()`
and compression is unchanged; a batch-level uniform `shiftTimestamps` with all-`keep()` records collapses
to a header-only rewrite (`BaseTimestamp`/`MaxTimestamp` + CRC) with the record block — compressed or not —
copied untouched; per-record byte copy for `keep()` records; and because edits are *field-granular*, even
a partial edit (e.g. value-only) copies the untouched field byte-ranges and re-encodes only the changed
field plus the record-length varint. Re-compression happens only for compressed, edited batches. Control
batches are always copied verbatim and never handed to the transform.

This directly fixes `RecordStream`'s two biggest problems: the **"keep" default** restores the
pristine-copy optimisation, and the model is **async-native** without forcing callers into multi-pass
orchestration.

---

## Part 4 — The async dimension: bounded-window concurrency (and its unavoidable tradeoffs)

Per the prior session's conclusion, async support should **not** be naïve (neither "all records
concurrently" nor "strictly one at a time"). Instead: a **configurable window of `W` records** may have
in-flight async transforms at once, while output is still assembled in strict offset/batch order.

How it works: the driver pulls record views, hands each to `AsyncRecordTransform`, and keeps at most `W`
futures outstanding. Completed edits are reassembled **in order** (an edit for record N cannot be emitted
until N-1 is emitted). Records whose transform returns `keep()` (or resolve synchronously from a warm
cache) don't meaningfully occupy a slot — they stream straight through / copy — so the window only
"fills" with records that genuinely need async work.

### The tradeoffs to surface (these feel fundamental, not implementation artifacts)

- **Memory ∝ W.** Single-pass async already beats multi-pass (hold ~W decoded records + W in-flight
  buffers, instead of materialising *all* decompressed/deserialised records). `W` makes that bound
  **explicit and predictable**. Larger `W` ⇒ more heap held concurrently.
- **Parallelism / external-service load ∝ W.** `W` caps how many concurrent registry/KMS lookups can be
  outstanding. Larger `W` ⇒ more overlap (better throughput when cache-cold / calls dominate) but more
  concurrent load on those services and more CPU for concurrent (de)serialisation.
- **Latency.** Larger `W` hides per-record latency better when cold; when **cache-warm (the common,
  amortised case)** transforms complete ~synchronously and `W` costs almost nothing — a key design goal:
  *the async path must not penalise the hot path.*
- **Batching vs. single-pass is recoverable.** The one thing multi-pass does better is *batching*
  external calls (resolve 1000 schema IDs in one request). A single-pass window can recover most of this
  if the shared resolver/cache **coalesces** the ≤`W` concurrent cache-misses into batched backend calls
  — then `W` doubles as the max external-batch size. Worth designing the cache/resolver seam for this.
- **Ordering & errors.** The window may span batch boundaries for throughput but must emit batches in
  order. A failed async transform (timeout) needs an explicit policy (fail the whole response, pass the
  record through untransformed, or surface a Kafka error) — a real decision, not a default.

The bespoke model and the window compose well: pristine/`keep()` records bypass the window entirely, so
`W` is spent only where it's needed.

---

## Part 5 — Scope, risks, and open questions

**What stays reused (low risk):** CRC-32C, varints, compression, header parsing — so we don't risk
broker/client byte-incompatibility on the parts that are hard to get exactly right.

**Risks / things to prove:**
- Byte-exactness of the *writer* vs. Kafka for identical edits (especially compressed batches and
  attribute/CRC bytes).
- Legacy v0/v1 handling: decide pass-through vs. reject vs. up-convert (reusing existing up-conversion if
  needed).
- Interop with the existing `kroxylicious-transform-api` dependency framework
  (`DependencyKey`/`ResolvedEntities`/`BufferTransform`): the window model should *subsume* it, but the
  migration of `ManipulationOrchestrator` needs thought.
- `ShareFetchResponseData` quirk already seen in the code (ImplicitLinkedHashCollection link fields).

**Open questions for later (not blocking this doc):**
- Do we support `drop()`/`append` records at all, and if so how do we keep EOS sequence ranges valid
  (mirror Kafka's `overrideLastOffset`)?
- Where does the new API live, and **governance**: this is public API for filter/plugin authors, so per
  `.claude/rules/api-changes.md` it will require a **formal design proposal** in
  `kroxylicious/design` before implementation.
- Default `W`, and whether it's per-filter, per-cluster, or global config.
- Buffer pooling strategy for the window (reuse the existing `BufferSupplier`/`ByteBufferOutputStream`
  seams).

---

## Part 6 — How to validate the design (before committing to a full build)

1. **Thin read-path prototype**: implement `RecordFrames`/`BatchView`/`RecordView` as lazy zero-copy
   views over the existing parser, and run the current `RecordStream` call sites' *read* operations
   through it (encryption gather pass, manipulation gather pass) to confirm ergonomics.
2. **Writer byte-compatibility property test**: parse real batches, apply a known edit (e.g. value →
   value) with both the new writer and the current `BatchAwareMemoryRecordsBuilder`/`MemoryRecordsBuilder`
   path, and assert byte-equality (or, where we intentionally differ e.g. whole-batch copy, assert the
   broker/client still accepts it). Cover: uncompressed, each compression codec, empty batches, control
   batches, transactional/idempotent batches.
3. **Pristine-copy correctness**: assert that `keep()`-only transforms reproduce the input bytes exactly
   (whole-batch and per-record copy paths).
4. **Async window harness**: a mock registry/KMS with controllable latency + a warm/cold cache; verify
   ordered output, bounded in-flight count = `W`, and coalescing of concurrent misses.
5. **JMH benchmarks** (new, under `kroxylicious-microbenchmarks`): compare against current `RecordStream`
   for (a) no-op pass-through, (b) value-only transform uncompressed, (c) value-only transform compressed,
   (d) encryption-like transform with a warm cache. Track allocations, not just throughput.
6. **End-to-end**: run the existing record-encryption and record-manipulation integration/system tests
   against a prototype adapter to confirm no semantic regressions with a real broker + real clients.
