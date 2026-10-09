/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.filter.resolve;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;

import io.kroxylicious.filter.record.manipulation.filter.config.Direction;
import io.kroxylicious.kafka.common.header.Header;
import io.kroxylicious.kafka.common.header.internals.RecordHeader;
import io.kroxylicious.kafka.common.record.internal.MemoryRecords;
import io.kroxylicious.kafka.common.record.internal.Record;
import io.kroxylicious.kafka.common.record.internal.Records;
import io.kroxylicious.kafka.common.utils.ByteBufferOutputStream;
import io.kroxylicious.testing.filter.record.RecordTestUtils;
import io.kroxylicious.transform.records.RecordFrames;
import io.kroxylicious.transform.records.TransformOptions;
import io.kroxylicious.transform.service.BufferTransform;

import edu.umd.cs.findbugs.annotations.Nullable;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ManipulationTransform} driven directly through the {@code records} engine, covering the
 * record-granularity behaviours the filter-level tests do not exercise: header-predicate selection, value
 * clearing (tombstone), and verbatim passthrough when nothing matches.
 */
class ManipulationTransformTest {

    private static final Header[] NO_HEADERS = new Header[0];
    private static final TransformOptions OPTIONS = TransformOptions.builder().executor(Runnable::run).windowSize(64).build();

    @Test
    void appliesManipulationOnlyToRecordsMatchingHeaderPredicate() {
        // Given
        var withHeader = RecordTestUtils.record(0L, 0L, null, "a".getBytes(UTF_8), new Header[]{ new RecordHeader("trace-id", null) });
        var withoutHeader = RecordTestUtils.record(1L, 0L, null, "b".getBytes(UTF_8), NO_HEADERS);
        var input = RecordTestUtils.memoryRecords(List.of(withHeader, withoutHeader));
        var manipulation = manipulation(Direction.INTO_CLUSTER, hasHeader("trace-id"), rewriteValueTo("X"));

        // When
        var output = applyProduce(input, manipulation);

        // Then
        assertThat(values(output)).containsExactly("X", "b");
    }

    @Test
    void clearsValueToTombstoneWhenValueTransformReturnsNull() {
        // Given
        var input = RecordTestUtils.memoryRecords(List.of(RecordTestUtils.record(0L, 0L, null, "a".getBytes(UTF_8), NO_HEADERS)));
        var manipulation = manipulation(Direction.INTO_CLUSTER, null,
                record -> CompletableFuture.completedStage(null));

        // When
        var output = applyProduce(input, manipulation);

        // Then
        assertThat(hasValue(output)).containsExactly(false);
    }

    @Test
    void keepsAllRecordsWhenNoManipulationMatches() {
        // Given
        var input = RecordTestUtils.memoryRecords(List.of(
                RecordTestUtils.record(0L, 0L, null, "a".getBytes(UTF_8), NO_HEADERS),
                RecordTestUtils.record(1L, 0L, null, "b".getBytes(UTF_8), NO_HEADERS)));
        var manipulation = manipulation(Direction.OUT_OF_CLUSTER, hasHeader("absent"), rewriteValueTo("X"));

        // When
        var output = applyFetch(input, manipulation);

        // Then
        assertThat(values(output)).containsExactly("a", "b");
    }

    // --- helpers ------------------------------------------------------------------------------

    private static MemoryRecords applyProduce(MemoryRecords input, CompiledManipulation<?, ?> manipulation) {
        var out = new ByteBufferOutputStream(Math.max(input.sizeInBytes(), 1));
        return RecordFrames.of(input)
                .applyAsync(new ManipulationTransform(List.of(manipulation)), out, OPTIONS)
                .toCompletableFuture().join();
    }

    private static MemoryRecords applyFetch(MemoryRecords input, CompiledManipulation<?, ?> manipulation) {
        var out = new ByteBufferOutputStream(Math.max(input.sizeInBytes(), 1));
        return RecordFrames.of(input)
                .applyAsync(new ManipulationTransform(List.of(manipulation)), out, OPTIONS)
                .toCompletableFuture().join();
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static CompiledManipulation<?, ?> manipulation(Direction direction,
                                                           @Nullable Predicate<Header[]> headerPredicate,
                                                           BufferTransform newValue) {
        return new CompiledManipulation(direction, subject -> true, (Predicate<String>) name -> true, headerPredicate,
                null, null, null, null, null, null, null, null, newValue);
    }

    private static Predicate<Header[]> hasHeader(String key) {
        return headers -> {
            for (Header header : headers) {
                if (key.equals(header.key())) {
                    return true;
                }
            }
            return false;
        };
    }

    private static BufferTransform rewriteValueTo(String replacement) {
        byte[] bytes = replacement.getBytes(UTF_8);
        return record -> CompletableFuture.completedStage(ByteBuffer.wrap(bytes));
    }

    private static List<String> values(MemoryRecords records) {
        List<String> values = new ArrayList<>();
        for (Record record : ((Records) records).records()) {
            values.add(RecordTestUtils.recordValueAsString(record));
        }
        return values;
    }

    private static List<Boolean> hasValue(MemoryRecords records) {
        List<Boolean> present = new ArrayList<>();
        for (Record record : ((Records) records).records()) {
            present.add(record.hasValue());
        }
        return present;
    }
}
