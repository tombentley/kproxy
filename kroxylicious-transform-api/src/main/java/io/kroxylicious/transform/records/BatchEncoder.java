/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.transform.records;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.util.List;

import io.kroxylicious.kafka.common.compress.Compression;
import io.kroxylicious.kafka.common.header.Header;
import io.kroxylicious.kafka.common.record.TimestampType;
import io.kroxylicious.kafka.common.record.internal.CompressionType;
import io.kroxylicious.kafka.common.record.internal.DefaultRecord;
import io.kroxylicious.kafka.common.record.internal.DefaultRecordBatch;
import io.kroxylicious.kafka.common.record.internal.MutableRecordBatch;
import io.kroxylicious.kafka.common.record.internal.RecordBatch;
import io.kroxylicious.kafka.common.utils.BufferSupplier;
import io.kroxylicious.kafka.common.utils.ByteBufferOutputStream;
import io.kroxylicious.transform.records.BatchEdit.TimestampMode;
import io.kroxylicious.transform.records.RecordFrames.InternalEdit;

/**
 * <p>The bespoke, byte-splicing encoder for a single (data) record batch. Given the records' immutable
 * {@link RecordView}s and the per-record {@link InternalEdit}s already evaluated in a single pass, it writes the
 * batch into {@code out} using the cheapest correct strategy:</p>
 *
 * <ul>
 *   <li><b>Header-only</b> — a uniform {@linkplain TimestampMode#SHIFT timestamp shift} with no record edits, no
 *       drops and no compression change: the whole batch is copied verbatim (no decompression, even when
 *       compressed) and only {@code BaseTimestamp}/{@code MaxTimestamp} and the CRC are rewritten.</li>
 *   <li><b>Block reassembly</b> — otherwise: unedited records are spliced into the output byte-for-byte, only
 *       edited records are re-encoded, dropped records are omitted, and the block is (re)compressed only if the
 *       target is compressed.</li>
 *   <li><b>Empty retention</b> — if every record is dropped, a retained empty batch header is written, preserving
 *       the batch's offset/sequence range (Kafka {@code RETAIN_EMPTY} semantics).</li>
 * </ul>
 *
 * <p>All v2 byte-exactness (header field offsets, attribute packing, CRC-32C) is delegated to the public
 * {@link DefaultRecordBatch#writeHeader} / {@link DefaultRecordBatch#writeEmptyHeader} and
 * {@link DefaultRecord#writeTo}; this class only decides what to copy versus re-encode. Legacy (magic &lt; v2)
 * batches are copied through verbatim (the bespoke encoder is v2-only for now).</p>
 */
final class BatchEncoder {

    private BatchEncoder() {
    }

    /**
     * Encode the (possibly edited) batch into {@code out}.
     *
     * @param out the output stream to append the encoded batch to
     * @param batch the source batch
     * @param batchEdit the batch-level edit (compression / timestamp transform)
     * @param views the records' views, in batch order
     * @param edits the per-record edits, aligned with {@code views}
     */
    static void encode(ByteBufferOutputStream out,
                       MutableRecordBatch batch,
                       BatchEdit batchEdit,
                       List<RecordView> views,
                       List<InternalEdit> edits) {
        byte magic = batch.magic();
        if (magic < RecordBatch.MAGIC_VALUE_V2) {
            // The bespoke encoder only understands v2; copy legacy batches through verbatim.
            batch.writeTo(out);
            return;
        }
        DefaultRecordBatch source = (DefaultRecordBatch) batch;

        TimestampMode tsMode = batchEdit.timestampMode();
        boolean deleteHorizonSet = source.deleteHorizonMs().isPresent();
        if (deleteHorizonSet && tsMode != TimestampMode.KEEP) {
            // BaseTimestamp encodes the delete horizon here, not a record time — moving it would corrupt the batch.
            throw new IllegalStateException("timestamp transforms are not supported on delete-horizon batches");
        }

        boolean anyFieldEdited = false;
        int survivors = 0;
        for (InternalEdit edit : edits) {
            if (edit.drop()) {
                continue;
            }
            survivors++;
            if (!edit.fields().isKeep()) {
                anyFieldEdited = true;
            }
        }
        boolean anyDropped = survivors < edits.size();

        CompressionType sourceType = source.compressionType();
        CompressionType targetType = batchEdit.compression() != null ? batchEdit.compression() : sourceType;
        boolean compressionChanged = targetType != sourceType;

        long baseOffset = source.baseOffset();
        int lastOffsetDelta = (int) (source.lastOffset() - baseOffset);
        TimestampType timestampType = source.timestampType();
        int partitionLeaderEpoch = source.partitionLeaderEpoch();
        long producerId = source.producerId();
        short producerEpoch = source.producerEpoch();
        int baseSequence = source.baseSequence();
        boolean isTransactional = source.isTransactional();
        // KEEP returns the originals; SHIFT adds the delta to both; SET collapses both to the target.
        long newBaseTimestamp = batchEdit.transformTimestamp(source.baseTimestamp());
        long newMaxTimestamp = batchEdit.transformTimestamp(source.maxTimestamp());

        // Header-only fast path: a uniform shift leaves every record's bytes (and timestampDelta) untouched.
        if (tsMode == TimestampMode.SHIFT && !anyFieldEdited && !anyDropped && !compressionChanged) {
            int headerPos = out.position();
            batch.writeTo(out);
            int size = source.sizeInBytes();
            ByteBuffer buffer = out.buffer();
            buffer.position(headerPos);
            DefaultRecordBatch.writeHeader(buffer, baseOffset, lastOffsetDelta, size, magic, sourceType,
                    timestampType, newBaseTimestamp, newMaxTimestamp, producerId, producerEpoch, baseSequence,
                    isTransactional, false, deleteHorizonSet, partitionLeaderEpoch, source.countOrNull());
            buffer.position(headerPos + size);
            return;
        }

        int headerPos = out.position();

        if (survivors == 0) {
            // Every record dropped: retain an empty batch header to preserve the offset/sequence range.
            out.position(headerPos + DefaultRecordBatch.RECORD_BATCH_OVERHEAD);
            ByteBuffer buffer = out.buffer();
            buffer.position(headerPos);
            DefaultRecordBatch.writeEmptyHeader(buffer, magic, producerId, producerEpoch, baseSequence,
                    baseOffset, source.lastOffset(), partitionLeaderEpoch, timestampType, newMaxTimestamp,
                    isTransactional, false);
            buffer.position(headerPos + DefaultRecordBatch.RECORD_BATCH_OVERHEAD);
            return;
        }

        // Block reassembly: splice unedited records, re-encode edited ones, (re)compress only if needed.
        out.position(headerPos + DefaultRecordBatch.RECORD_BATCH_OVERHEAD);
        boolean compress = targetType != CompressionType.NONE;
        ByteBuffer block = decompressedBlock(source, sourceType);
        int blockLen = block.remaining();

        int appended;
        try {
            DataOutputStream dos = new DataOutputStream(Compression.of(targetType).build().wrapForOutput(out, magic));
            byte[] scratch = null;
            int cursor = 0;
            for (int i = 0; i < views.size(); i++) {
                RecordView view = views.get(i);
                InternalEdit edit = edits.get(i);
                int len = view.sizeInBytes();
                int recStart = cursor;
                cursor += len;
                if (edit.drop()) {
                    continue;
                }
                if (edit.fields().isKeep() && tsMode != TimestampMode.SET) {
                    // Unedited record under KEEP/SHIFT: its on-wire bytes are still valid, so splice verbatim.
                    if (compress) {
                        scratch = writeSliceToStream(dos, block, recStart, len, scratch);
                    }
                    else {
                        ByteBuffer slice = block.duplicate();
                        slice.position(recStart).limit(recStart + len);
                        out.write(slice);
                    }
                }
                else {
                    FieldEdits fields = edit.fields();
                    ByteBuffer key = fields.resolveKey(view);
                    ByteBuffer value = fields.resolveValue(view);
                    Header[] headers = fields.resolveHeaders(view);
                    int offsetDelta = (int) (view.offset() - baseOffset);
                    long timestampDelta = batchEdit.transformTimestamp(view.timestamp()) - newBaseTimestamp;
                    DefaultRecord.writeTo(dos, offsetDelta, timestampDelta, key, value, headers);
                }
            }
            dos.close();
            if (cursor != blockLen) {
                throw new IllegalStateException("record sizes (" + cursor + ") do not sum to the block length (" + blockLen + ")");
            }
            appended = survivors;
        }
        catch (IOException e) {
            throw new UncheckedIOException("while encoding a record batch", e);
        }

        int endPos = out.position();
        int size = endPos - headerPos;
        ByteBuffer buffer = out.buffer();
        buffer.position(headerPos);
        DefaultRecordBatch.writeHeader(buffer, baseOffset, lastOffsetDelta, size, magic, targetType, timestampType,
                newBaseTimestamp, newMaxTimestamp, producerId, producerEpoch, baseSequence, isTransactional, false,
                deleteHorizonSet, partitionLeaderEpoch, appended);
        buffer.position(endPos);
    }

    private static ByteBuffer decompressedBlock(DefaultRecordBatch source, CompressionType sourceType) {
        if (sourceType == CompressionType.NONE) {
            // Zero-copy: the batch's record block is already the decompressed bytes.
            return source.recordsBuffer();
        }
        try (InputStream in = source.recordInputStream(BufferSupplier.create())) {
            return ByteBuffer.wrap(in.readAllBytes());
        }
        catch (IOException e) {
            throw new UncheckedIOException("while decompressing a record batch", e);
        }
    }

    private static byte[] writeSliceToStream(DataOutputStream dos, ByteBuffer block, int start, int len, byte[] scratch)
            throws IOException {
        if (block.hasArray()) {
            dos.write(block.array(), block.arrayOffset() + start, len);
            return scratch;
        }
        byte[] buf = (scratch == null || scratch.length < len) ? new byte[len] : scratch;
        ByteBuffer slice = block.duplicate();
        slice.position(start).limit(start + len);
        slice.get(buf, 0, len);
        dos.write(buf, 0, len);
        return buf;
    }
}
