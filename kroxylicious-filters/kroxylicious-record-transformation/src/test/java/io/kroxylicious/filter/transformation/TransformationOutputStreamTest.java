/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TransformationOutputStreamTest {

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2})
    void write(int numClose) throws IOException {
        var output = new TransformationOutputStream(4);
        output.write(1);
        output.write(2);
        output.write(3);
        output.write(4);
        output.flush();
        output.write(5);
        for (int i = 0; i < numClose; i++) {
            output.close();
        }
        if (numClose > 0) {
            assertThatThrownBy(() -> output.write(6)).isExactlyInstanceOf(IOException.class);
        }
        assertThat(output.toByteBuffer().mismatch(ByteBuffer.wrap(new byte[]{1, 2, 3, 4, 5}))).isEqualTo(-1);
        assertThat(output.toByteBuffer().mismatch(ByteBuffer.wrap(new byte[]{1, 2, 3, 4, 5}))).isEqualTo(-1);

        TransformationInputStream flip = output.flip();
        assertThat(flip.read()).isEqualTo(1);
        assertThat(flip.read()).isEqualTo(2);
        assertThat(flip.read()).isEqualTo(3);
        assertThat(flip.read()).isEqualTo(4);
        assertThat(flip.read()).isEqualTo(5);
        assertThat(flip.read()).isEqualTo(-1);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2})
    void writeInt(int numClose) throws IOException {
        var output = new TransformationOutputStream(4);
        output.writeInt(1);
        output.writeInt(2);
        output.writeInt(3);
        output.writeInt(4);
        output.flush();
        output.writeInt(5);
        for (int i = 0; i < numClose; i++) {
            output.close();
        }
        if (numClose > 0) {
            assertThatThrownBy(() -> output.writeInt(6)).isExactlyInstanceOf(IOException.class);
        }
        assertThat(output.toByteBuffer().mismatch(ByteBuffer.wrap(new byte[]{0, 0, 0, 1, 0, 0, 0, 2, 0, 0, 0, 3, 0, 0, 0, 4, 0, 0, 0, 5}))).isEqualTo(-1);
        assertThat(output.toByteBuffer().mismatch(ByteBuffer.wrap(new byte[]{0, 0, 0, 1, 0, 0, 0, 2, 0, 0, 0, 3, 0, 0, 0, 4, 0, 0, 0, 5}))).isEqualTo(-1);

        TransformationInputStream flip = output.flip();
        assertThat(flip.readInt()).isEqualTo(1);
        assertThat(flip.readInt()).isEqualTo(2);
        assertThat(flip.readInt()).isEqualTo(3);
        assertThat(flip.readInt()).isEqualTo(4);
        assertThat(flip.readInt()).isEqualTo(5);
        assertThatThrownBy(flip::readInt).isExactlyInstanceOf(EOFException.class);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2})
    void writeLong(int numClose) throws IOException {
        var output = new TransformationOutputStream(4);
        output.writeLong(1);
        output.writeLong(2);
        output.writeLong(3);
        output.writeLong(4);
        output.flush();
        output.writeLong(5);
        for (int i = 0; i < numClose; i++) {
            output.close();
        }
        if (numClose > 0) {
            assertThatThrownBy(() -> output.writeLong(6)).isExactlyInstanceOf(IOException.class);
        }
        assertThat(output.toByteBuffer().mismatch(ByteBuffer.wrap(new byte[]{
                0, 0, 0, 0, 0, 0, 0, 1,
                0, 0, 0, 0, 0, 0, 0, 2,
                0, 0, 0, 0, 0, 0, 0, 3,
                0, 0, 0, 0, 0, 0, 0, 4,
                0, 0, 0, 0, 0, 0, 0, 5}))).isEqualTo(-1);
        assertThat(output.toByteBuffer().mismatch(ByteBuffer.wrap(new byte[]{
                0, 0, 0, 0, 0, 0, 0, 1,
                0, 0, 0, 0, 0, 0, 0, 2,
                0, 0, 0, 0, 0, 0, 0, 3,
                0, 0, 0, 0, 0, 0, 0, 4,
                0, 0, 0, 0, 0, 0, 0, 5}))).isEqualTo(-1);

        TransformationInputStream flip = output.flip();
        assertThat(flip.readLong()).isEqualTo(1);
        assertThat(flip.readLong()).isEqualTo(2);
        assertThat(flip.readLong()).isEqualTo(3);
        assertThat(flip.readLong()).isEqualTo(4);
        assertThat(flip.readLong()).isEqualTo(5);
        assertThatThrownBy(flip::readLong).isExactlyInstanceOf(EOFException.class);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2})
    void writeArray(int numClose) throws IOException {
        var output = new TransformationOutputStream(4);
        output.write(new byte[]{1, 2}, 0, 2);
        output.write(new byte[]{1, 2, 3, 4, 5}, 2, 2);
        output.flush();
        output.write(5);
        for (int i = 0; i < numClose; i++) {
            output.close();
        }
        if (numClose > 0) {
            assertThatThrownBy(() -> output.write(6)).isExactlyInstanceOf(IOException.class);
        }
        assertThat(output.toByteBuffer().mismatch(ByteBuffer.wrap(new byte[]{1, 2, 3, 4, 5}))).isEqualTo(-1);
        assertThat(output.toByteBuffer().mismatch(ByteBuffer.wrap(new byte[]{1, 2, 3, 4, 5}))).isEqualTo(-1);

        TransformationInputStream flip = output.flip();
        assertThat(flip.read()).isEqualTo(1);
        assertThat(flip.read()).isEqualTo(2);
        assertThat(flip.read()).isEqualTo(3);
        assertThat(flip.read()).isEqualTo(4);
        assertThat(flip.read()).isEqualTo(5);
        assertThat(flip.read()).isEqualTo(-1);
    }

}