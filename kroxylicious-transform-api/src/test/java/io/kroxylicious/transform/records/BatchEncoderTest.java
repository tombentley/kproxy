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
import io.kroxylicious.kafka.common.record.internal.DefaultRecordBatch;
import io.kroxylicious.kafka.common.record.internal.MemoryRecords;
import io.kroxylicious.kafka.common.record.internal.MutableRecordBatch;
import io.kroxylicious.kafka.common.record.internal.Record;
import io.kroxylicious.kafka.common.record.internal.SimpleRecord;
import io.kroxylicious.kafka.common.utils.ByteBufferOutputStream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the byte-splicing {@link BatchEncoder}, focused on the properties that distinguish it from a
 * decode-and-rebuild encoder: unedited records are copied byte-for-byte, a uniform timestamp shift is a
 * header-only rewrite, every record is dropped retains an empty batch, and the CRC is valid for batches written
 * at a non-zero output offset.
 */
class BatchEncoderTest {

    static Stream<Compression> compressions() {
        return Stream.of(
                Compression.NONE,
                Compression.gzip().build(),
                Compression.snappy().build(),
                Compression.lz4().build(),
                Compression.zstd().build());
    }

    @Test
    void uneditedRecordsAreSplicedByteForByte() {
        // Given
        MemoryRecords input = MemoryRecords.withRecords(0L, Compression.NONE,
                new SimpleRecord(100L, "k0".getBytes(UTF_8), "v0".getBytes(UTF_8)),
                new SimpleRecord(101L, "k1".getBytes(UTF_8), "v1".getBytes(UTF_8)),
                new SimpleRecord(102L, "k2".getBytes(UTF_8), "v2".getBytes(UTF_8)));

        // When
        MemoryRecords output = ProducerRecords.of(input).applyProduce(
                (batch, record) -> record.offset() == 1L ? ProducerRecordEdit.value(bb("EDITED")) : ProducerRecordEdit.keep(),
                newBuffer());

        // Then
        List<byte[]> in = perRecordBytes(input);
        List<byte[]> out = perRecordBytes(output);
        assertThat(out).hasSize(3);
        assertThat(out.get(0)).isEqualTo(in.get(0));
        assertThat(out.get(2)).isEqualTo(in.get(2));
        assertThat(out.get(1)).isNotEqualTo(in.get(1));
        assertAllValid(output);
    }

    @ParameterizedTest
    @MethodSource("compressions")
    void uniformTimestampShiftLeavesRecordBlockByteIdentical(Compression compression) {
        // Given
        MemoryRecords input = MemoryRecords.withRecords(0L, compression,
                new SimpleRecord(100L, "k0".getBytes(UTF_8), "v0".getBytes(UTF_8)),
                new SimpleRecord(250L, "k1".getBytes(UTF_8), "v1".getBytes(UTF_8)));

        // When
        MemoryRecords output = ProducerRecords.of(input).applyProduce(
                batch -> BatchEdit.builder().shiftTimestamps(1000L).build(),
                (batch, record) -> ProducerRecordEdit.keep(),
                newBuffer());

        // Then
        assertThat(recordBlockBytes(output)).isEqualTo(recordBlockBytes(input));
        List<Record> out = records(output);
        assertThat(out.get(0).timestamp()).isEqualTo(1100L);
        assertThat(out.get(1).timestamp()).isEqualTo(1250L);
        assertAllValid(output);
    }

    @Test
    void setTimestampsSetsEveryRecordTimestamp() {
        // Given
        MemoryRecords input = MemoryRecords.withRecords(0L, Compression.NONE,
                new SimpleRecord(100L, "k0".getBytes(UTF_8), "v0".getBytes(UTF_8)),
                new SimpleRecord(250L, "k1".getBytes(UTF_8), "v1".getBytes(UTF_8)));

        // When
        MemoryRecords output = ProducerRecords.of(input).applyProduce(
                batch -> BatchEdit.builder().setTimestamps(777L).build(),
                (batch, record) -> ProducerRecordEdit.keep(),
                newBuffer());

        // Then
        List<Record> out = records(output);
        assertThat(out).allSatisfy(r -> assertThat(r.timestamp()).isEqualTo(777L));
        assertThat(str(out.get(0).value())).isEqualTo("v0");
        assertThat(str(out.get(1).value())).isEqualTo("v1");
        assertThat(out.get(0).offset()).isEqualTo(0L);
        assertThat(out.get(1).offset()).isEqualTo(1L);
        assertAllValid(output);
    }

    @Test
    void droppingEveryRecordRetainsEmptyBatch() {
        // Given
        MemoryRecords input = MemoryRecords.withRecords(10L, Compression.NONE,
                new SimpleRecord(1L, "k0".getBytes(UTF_8), "v0".getBytes(UTF_8)),
                new SimpleRecord(2L, "k1".getBytes(UTF_8), "v1".getBytes(UTF_8)),
                new SimpleRecord(3L, "k2".getBytes(UTF_8), "v2".getBytes(UTF_8)));
        int baseSequence = single(input).baseSequence();

        // When
        MemoryRecords output = ConsumerRecords.of(input).applyFetch(
                (batch, record) -> ConsumerRecordEdit.drop(), newBuffer());

        // Then
        MutableRecordBatch batch = single(output);
        assertThat(batch.countOrNull()).isZero();
        assertThat(batch.baseOffset()).isEqualTo(10L);
        assertThat(batch.lastOffset()).isEqualTo(12L);
        assertThat(batch.baseSequence()).isEqualTo(baseSequence);
        assertThat(batch.isValid()).isTrue();
        assertThat(records(output)).isEmpty();
    }

    @ParameterizedTest
    @MethodSource("compressions")
    void editsAcrossMultipleBatchesProduceValidCrcs(Compression compression) {
        // Given
        MemoryRecords input = concat(
                MemoryRecords.withRecords(0L, compression,
                        new SimpleRecord(10L, "a".getBytes(UTF_8), "a0".getBytes(UTF_8)),
                        new SimpleRecord(11L, "a".getBytes(UTF_8), "a1".getBytes(UTF_8))),
                MemoryRecords.withRecords(2L, compression,
                        new SimpleRecord(20L, "b".getBytes(UTF_8), "b0".getBytes(UTF_8)),
                        new SimpleRecord(21L, "b".getBytes(UTF_8), "b1".getBytes(UTF_8))));

        // When
        MemoryRecords output = ProducerRecords.of(input).applyProduce(
                (batch, record) -> ProducerRecordEdit.value(bb("V:" + str(record.value()))), newBuffer());

        // Then
        List<MutableRecordBatch> batches = new ArrayList<>();
        output.batches().forEach(batches::add);
        assertThat(batches).hasSize(2);
        assertThat(batches).allSatisfy(b -> assertThat(b.isValid()).isTrue());
        List<Record> out = records(output);
        assertThat(out).hasSize(4);
        assertThat(str(out.get(0).value())).isEqualTo("V:a0");
        assertThat(str(out.get(3).value())).isEqualTo("V:b1");
        assertThat(out.get(2).offset()).isEqualTo(2L);
        assertThat(out.get(3).offset()).isEqualTo(3L);
    }

    @ParameterizedTest
    @MethodSource("compressions")
    void valueEditRoundTripsAcrossCompressions(Compression compression) {
        // Given
        MemoryRecords input = MemoryRecords.withRecords(5L, compression,
                new SimpleRecord(100L, "k0".getBytes(UTF_8), "v0".getBytes(UTF_8)),
                new SimpleRecord(101L, "k1".getBytes(UTF_8), "v1".getBytes(UTF_8)));

        // When
        MemoryRecords output = ProducerRecords.of(input).applyProduce(
                (batch, record) -> ProducerRecordEdit.value(bb("V:" + str(record.value()))), newBuffer());

        // Then
        assertThat(single(output).compressionType()).isEqualTo(compression.type());
        List<Record> out = records(output);
        assertThat(str(out.get(0).value())).isEqualTo("V:v0");
        assertThat(str(out.get(1).value())).isEqualTo("V:v1");
        assertThat(out.get(0).offset()).isEqualTo(5L);
        assertThat(out.get(1).offset()).isEqualTo(6L);
        assertAllValid(output);
    }

    // --- helpers ---

    private static ByteBufferOutputStream newBuffer() {
        return new ByteBufferOutputStream(1024);
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

    private static void assertAllValid(MemoryRecords records) {
        for (MutableRecordBatch batch : records.batches()) {
            assertThat(batch.isValid()).isTrue();
        }
    }

    /** The raw (possibly compressed) record-block bytes of a single-batch {@link MemoryRecords}. */
    private static byte[] recordBlockBytes(MemoryRecords records) {
        ByteBuffer block = ((DefaultRecordBatch) single(records)).recordsBuffer();
        byte[] bytes = new byte[block.remaining()];
        block.get(bytes);
        return bytes;
    }

    /** The per-record encoded bytes of a single, uncompressed batch, in order. */
    private static List<byte[]> perRecordBytes(MemoryRecords records) {
        DefaultRecordBatch batch = (DefaultRecordBatch) single(records);
        ByteBuffer block = batch.recordsBuffer();
        List<byte[]> out = new ArrayList<>();
        int cursor = 0;
        for (Record record : batch) {
            int len = record.sizeInBytes();
            byte[] bytes = new byte[len];
            ByteBuffer slice = block.duplicate();
            slice.position(cursor).limit(cursor + len);
            slice.get(bytes);
            out.add(bytes);
            cursor += len;
        }
        return out;
    }

    private static MemoryRecords concat(MemoryRecords first, MemoryRecords second) {
        ByteBuffer a = first.buffer();
        ByteBuffer b = second.buffer();
        ByteBuffer out = ByteBuffer.allocate(a.remaining() + b.remaining());
        out.put(a.duplicate());
        out.put(b.duplicate());
        out.flip();
        return MemoryRecords.readableRecords(out);
    }
}
