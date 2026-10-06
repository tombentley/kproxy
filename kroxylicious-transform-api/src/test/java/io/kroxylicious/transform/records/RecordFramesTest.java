/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.transform.records;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import io.kroxylicious.kafka.common.compress.Compression;
import io.kroxylicious.kafka.common.header.Header;
import io.kroxylicious.kafka.common.header.internals.RecordHeader;
import io.kroxylicious.kafka.common.record.internal.CompressionType;
import io.kroxylicious.kafka.common.record.internal.MemoryRecords;
import io.kroxylicious.kafka.common.record.internal.MutableRecordBatch;
import io.kroxylicious.kafka.common.record.internal.Record;
import io.kroxylicious.kafka.common.record.internal.SimpleRecord;
import io.kroxylicious.kafka.common.utils.ByteBufferOutputStream;
import io.kroxylicious.testing.filter.record.RecordTestUtils;
import io.kroxylicious.transform.records.HeaderEdits.WhenAbsent;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

class RecordFramesTest {

    static Stream<Compression> compressions() {
        return Stream.of(
                Compression.NONE,
                Compression.gzip().build(),
                Compression.snappy().build(),
                Compression.lz4().build(),
                Compression.zstd().build());
    }

    @ParameterizedTest
    @MethodSource("compressions")
    void produceKeepIsByteIdenticalCopy(Compression compression) {
        // Given
        MemoryRecords input = MemoryRecords.withRecords(5L, compression,
                new SimpleRecord(100L, "k0".getBytes(UTF_8), "v0".getBytes(UTF_8)),
                new SimpleRecord(101L, "k1".getBytes(UTF_8), "v1".getBytes(UTF_8)),
                new SimpleRecord(102L, "k2".getBytes(UTF_8), "v2".getBytes(UTF_8)));

        // When
        MemoryRecords output = RecordFrames.of(input)
                .apply((batch, record) -> RecordEdit.keep(), newBuffer());

        // Then
        assertThat(bytes(output)).isEqualTo(bytes(input));
    }

    @ParameterizedTest
    @MethodSource("compressions")
    void produceValueEditPreservesEverythingElse(Compression compression) {
        // Given
        MemoryRecords input = MemoryRecords.withRecords(5L, compression,
                new SimpleRecord(100L, "k0".getBytes(UTF_8), "v0".getBytes(UTF_8), new Header[]{ header("h", "x") }),
                new SimpleRecord(101L, "k1".getBytes(UTF_8), "v1".getBytes(UTF_8), new Header[]{ header("h", "x") }));

        // When
        MemoryRecords output = RecordFrames.of(input)
                .apply((batch, record) -> RecordEdit.value(bb("V:" + str(record.value()))), newBuffer());

        // Then
        List<Record> out = records(output);
        assertThat(out).hasSize(2);
        assertThat(single(output).compressionType()).isEqualTo(compression.type());
        assertThat(out).allSatisfy(r -> assertThat(r.headers()).singleElement()
                .satisfies(h -> assertThat(h.key()).isEqualTo("h")));
        assertThat(out.get(0)).satisfies(r -> {
            assertThat(r.offset()).isEqualTo(5L);
            assertThat(r.timestamp()).isEqualTo(100L);
            assertThat(str(r.key())).isEqualTo("k0");
            assertThat(str(r.value())).isEqualTo("V:v0");
        });
        assertThat(out.get(1)).satisfies(r -> {
            assertThat(r.offset()).isEqualTo(6L);
            assertThat(r.timestamp()).isEqualTo(101L);
            assertThat(str(r.key())).isEqualTo("k1");
            assertThat(str(r.value())).isEqualTo("V:v1");
        });
    }

    @Test
    void headerEditsComposeInCallOrderOverDuplicateKeys() {
        // Given
        MemoryRecords input = MemoryRecords.withRecords(0L, Compression.NONE,
                new SimpleRecord(1L, "k".getBytes(UTF_8), "v".getBytes(UTF_8),
                        new Header[]{ header("a", "1"), header("b", "2"), header("a", "3") }));

        // When
        MemoryRecords output = RecordFrames.of(input).apply(
                (batch, record) -> RecordEdit.builder()
                        .headers(h -> h.removeAll("a")
                                .addLast("c", "9".getBytes(UTF_8))
                                .replaceFirst("b", "B".getBytes(UTF_8), WhenAbsent.NOOP))
                        .build(),
                newBuffer());

        // Then
        Header[] headers = records(output).get(0).headers();
        assertThat(headers).hasSize(2);
        assertThat(headers[0].key()).isEqualTo("b");
        assertThat(headers[0].value()).isEqualTo("B".getBytes(UTF_8));
        assertThat(headers[1].key()).isEqualTo("c");
        assertThat(headers[1].value()).isEqualTo("9".getBytes(UTF_8));
    }

    @Test
    void replaceFirstWhenAbsentAddLastAppendsWhenKeyMissing() {
        // Given
        MemoryRecords input = MemoryRecords.withRecords(0L, Compression.NONE,
                new SimpleRecord(1L, "k".getBytes(UTF_8), "v".getBytes(UTF_8), new Header[]{ header("a", "1") }));

        // When
        MemoryRecords output = RecordFrames.of(input).apply(
                (batch, record) -> RecordEdit.builder()
                        .headers(h -> h.replaceFirst("missing", "N".getBytes(UTF_8), WhenAbsent.ADD_LAST))
                        .build(),
                newBuffer());

        // Then
        Header[] headers = records(output).get(0).headers();
        assertThat(headers).hasSize(2);
        assertThat(headers[1].key()).isEqualTo("missing");
        assertThat(headers[1].value()).isEqualTo("N".getBytes(UTF_8));
    }

    @Test
    void batchEditChangesCompressionAndPreservesRecords() {
        // Given
        MemoryRecords input = MemoryRecords.withRecords(0L, Compression.NONE,
                new SimpleRecord(1L, "k0".getBytes(UTF_8), "v0".getBytes(UTF_8)),
                new SimpleRecord(2L, "k1".getBytes(UTF_8), "v1".getBytes(UTF_8)));

        // When
        MemoryRecords output = RecordFrames.of(input).apply(
                batch -> BatchEdit.builder().compression(CompressionType.ZSTD).build(),
                (batch, record) -> RecordEdit.keep(),
                newBuffer());

        // Then
        assertThat(single(output).compressionType()).isEqualTo(CompressionType.ZSTD);
        List<Record> out = records(output);
        assertThat(out).hasSize(2);
        assertThat(str(out.get(0).value())).isEqualTo("v0");
        assertThat(str(out.get(1).value())).isEqualTo("v1");
        assertThat(out.get(0).offset()).isEqualTo(0L);
        assertThat(out.get(1).offset()).isEqualTo(1L);
    }

    @Test
    void batchEditShiftTimestampsAddsDeltaToEveryRecord() {
        // Given
        MemoryRecords input = MemoryRecords.withRecords(0L, Compression.NONE,
                new SimpleRecord(100L, "k0".getBytes(UTF_8), "v0".getBytes(UTF_8)),
                new SimpleRecord(200L, "k1".getBytes(UTF_8), "v1".getBytes(UTF_8)));

        // When
        MemoryRecords output = RecordFrames.of(input).apply(
                batch -> BatchEdit.builder().shiftTimestamps(1000L).build(),
                (batch, record) -> RecordEdit.keep(),
                newBuffer());

        // Then
        List<Record> out = records(output);
        assertThat(out.get(0).timestamp()).isEqualTo(1100L);
        assertThat(out.get(1).timestamp()).isEqualTo(1200L);
        assertThat(str(out.get(0).value())).isEqualTo("v0");
        assertThat(str(out.get(1).value())).isEqualTo("v1");
    }

    @Test
    void fetchDropRemovesRecordAndPreservesSurvivorOffsets() {
        // Given
        MemoryRecords input = MemoryRecords.withRecords(10L, Compression.NONE,
                new SimpleRecord(1L, "k0".getBytes(UTF_8), "v0".getBytes(UTF_8)),
                new SimpleRecord(2L, "k1".getBytes(UTF_8), "v1".getBytes(UTF_8)),
                new SimpleRecord(3L, "k2".getBytes(UTF_8), "v2".getBytes(UTF_8)));

        // When
        MemoryRecords output = RecordFrames.of(input).apply(
                (batch, record) -> record.offset() == 11L ? RecordEdit.drop() : RecordEdit.keep(),
                newBuffer());

        // Then
        assertThat(single(output).lastOffset()).isEqualTo(12L);
        List<Record> out = records(output);
        assertThat(out).hasSize(2);
        assertThat(out.get(0).offset()).isEqualTo(10L);
        assertThat(str(out.get(0).value())).isEqualTo("v0");
        assertThat(out.get(1).offset()).isEqualTo(12L);
        assertThat(str(out.get(1).value())).isEqualTo("v2");
    }

    @Test
    void controlBatchIsCopiedThroughVerbatim() {
        // Given
        MutableRecordBatch controlBatch = RecordTestUtils.abortTransactionControlBatch(0);
        MemoryRecords input = RecordTestUtils.memoryRecords(controlBatch);

        // When
        MemoryRecords output = RecordFrames.of(input).apply(
                (batch, record) -> RecordEdit.value(bb("should-not-be-applied")), newBuffer());

        // Then
        assertThat(bytes(output)).isEqualTo(bytes(input));
    }

    // --- helpers ---

    private static ByteBufferOutputStream newBuffer() {
        return new ByteBufferOutputStream(1024);
    }

    private static Header header(String key, String value) {
        return new RecordHeader(key, value.getBytes(UTF_8));
    }

    private static ByteBuffer bb(String s) {
        return ByteBuffer.wrap(s.getBytes(UTF_8));
    }

    private static String str(ByteBuffer buffer) {
        return StandardCharsets.UTF_8.decode(buffer.duplicate()).toString();
    }

    private static MutableRecordBatch single(MemoryRecords records) {
        List<MutableRecordBatch> batches = new ArrayList<>();
        records.batches().forEach(batches::add);
        assertThat(batches).hasSize(1);
        return batches.get(0);
    }

    private static List<Record> records(MemoryRecords records) {
        List<Record> out = new ArrayList<>();
        for (MutableRecordBatch batch : records.batches()) {
            for (Record record : batch) {
                out.add(record);
            }
        }
        return out;
    }

    private static byte[] bytes(MemoryRecords records) {
        ByteBuffer buffer = records.buffer();
        byte[] array = new byte[buffer.remaining()];
        buffer.get(array);
        return array;
    }
}
