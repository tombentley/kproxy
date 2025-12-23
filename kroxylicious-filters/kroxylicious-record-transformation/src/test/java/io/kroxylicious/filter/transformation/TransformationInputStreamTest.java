/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TransformationInputStreamTest {

    @Test
    void read() throws IOException {
        try (var inputStream = new TransformationInputStream(ByteBuffer.wrap(new byte[]{1, 2, 3, 4}))) {
            assertThat(inputStream.available()).isEqualTo(4);
            assertThat(inputStream.read()).isEqualTo(1);
            assertThat(inputStream.available()).isEqualTo(3);
            assertThat(inputStream.read()).isEqualTo(2);
            assertThat(inputStream.available()).isEqualTo(2);
            assertThat(inputStream.read()).isEqualTo(3);
            assertThat(inputStream.available()).isEqualTo(1);
            assertThat(inputStream.read()).isEqualTo(4);
            assertThat(inputStream.available()).isZero();
            assertThat(inputStream.read()).isEqualTo(-1);
        }
    }

    @Test
    void readArray() throws IOException {
        try (var inputStream = new TransformationInputStream(ByteBuffer.wrap(new byte[]{1, 2, 3, 4}))) {
            assertThat(inputStream.available()).isEqualTo(4);
            var b = new byte[4];
            assertThat(inputStream.read(b, 0, 2)).isEqualTo(2);
            assertThat(inputStream.available()).isEqualTo(2);
            assertThat(inputStream.read(b, 2, 2)).isEqualTo(2);
            assertThat(inputStream.available()).isZero();
            assertThat(inputStream.read(b, 0, b.length)).isEqualTo(-1);
            assertThat(b).isEqualTo(new byte[]{1, 2, 3, 4});
        }
    }

    @Test
    void readByte() throws IOException {
        try (var inputStream = new TransformationInputStream(ByteBuffer.wrap(new byte[]{1, 2, 3, 4}))) {
            assertThat(inputStream.available()).isEqualTo(4);
            assertThat(inputStream.readByte()).isEqualTo((byte) 1);
            assertThat(inputStream.available()).isEqualTo(3);
            assertThat(inputStream.readByte()).isEqualTo((byte) 2);
            assertThat(inputStream.available()).isEqualTo(2);
            assertThat(inputStream.readByte()).isEqualTo((byte) 3);
            assertThat(inputStream.available()).isEqualTo(1);
            assertThat(inputStream.readByte()).isEqualTo((byte) 4);
            assertThat(inputStream.available()).isZero();
            assertThatThrownBy(inputStream::readByte).isExactlyInstanceOf(EOFException.class);
        }
    }

    static List<ByteOrder> byteOrders() {
        return List.of(ByteOrder.BIG_ENDIAN, ByteOrder.LITTLE_ENDIAN);
    }

    @ParameterizedTest
    @MethodSource("byteOrders")
    void readInt(ByteOrder byteOrder) throws IOException {
        ByteBuffer wrap = ByteBuffer.wrap(new byte[]{ 0, 0, 0, 1, 0, 0, 0, 2, 0, 0, 0, 3, 0, 0, 0, 4 });
        wrap.order(byteOrder);
        try (var inputStream = new TransformationInputStream(wrap)) {
            assertThat(inputStream.available()).isEqualTo(16);
            assertThat(inputStream.readInt()).isEqualTo(1);
            assertThat(inputStream.available()).isEqualTo(12);
            assertThat(inputStream.readInt()).isEqualTo(2);
            assertThat(inputStream.available()).isEqualTo(8);
            assertThat(inputStream.readInt()).isEqualTo(3);
            assertThat(inputStream.available()).isEqualTo(4);
            assertThat(inputStream.readInt()).isEqualTo(4);
            assertThat(inputStream.available()).isZero();
            assertThatThrownBy(inputStream::readInt).isExactlyInstanceOf(EOFException.class);
        }
    }

    @ParameterizedTest
    @MethodSource("byteOrders")
    void readLong(ByteOrder byteOrder) throws IOException {
        ByteBuffer wrap = ByteBuffer.wrap(new byte[]{ 0, 0, 0, 0, 0, 0, 0, 1,
                0, 0, 0, 0, 0, 0, 0, 2,
                0, 0, 0, 0, 0, 0, 0, 3,
                0, 0, 0, 0, 0, 0, 0, 4 });
        wrap.order(byteOrder);
        try (var inputStream = new TransformationInputStream(wrap)) {
            assertThat(inputStream.available()).isEqualTo(32);
            assertThat(inputStream.readLong()).isEqualTo(1);
            assertThat(inputStream.available()).isEqualTo(24);
            assertThat(inputStream.readLong()).isEqualTo(2);
            assertThat(inputStream.available()).isEqualTo(16);
            assertThat(inputStream.readLong()).isEqualTo(3);
            assertThat(inputStream.available()).isEqualTo(8);
            assertThat(inputStream.readLong()).isEqualTo(4);
            assertThat(inputStream.available()).isZero();
            assertThatThrownBy(inputStream::readLong).isExactlyInstanceOf(EOFException.class);
        }
    }

    @Test
    void mark() throws IOException {
        try (var inputStream = new TransformationInputStream(ByteBuffer.wrap(new byte[]{1, 2, 3, 4}))) {
            assertThatThrownBy(inputStream::reset).isInstanceOf(IOException.class);
            assertThat(inputStream.markSupported()).isTrue();
            assertThat(inputStream.read()).isEqualTo(1);
            inputStream.mark(1);
            assertThat(inputStream.read()).isEqualTo(2);
            inputStream.reset();
            assertThat(inputStream.read()).isEqualTo(2);
            inputStream.reset();
            assertThat(inputStream.read()).isEqualTo(2);
            assertThat(inputStream.read()).isEqualTo(3);
            assertThatThrownBy(inputStream::reset).isExactlyInstanceOf(IOException.class);
            assertThat(inputStream.read()).isEqualTo(4);
            assertThat(inputStream.read()).isEqualTo(-1);
        }
    }

}