/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.encryption;

import java.nio.ByteBuffer;

import org.apache.kafka.common.record.Record;

/**
 * A request to encrypt some buffer of {@code plaintext} associated with a given {@code kafkaRecord}
 *
 * @param kafkaRecord The record.
 * @param plaintextSize The size of the {@code plaintext}.
 * @param plaintext The plaintext to be encrypted. In general this might be the record's value, or it's key, or any of its headers.
 */
public record RecordEncryptionRequest(
                                      Record kafkaRecord,
                                      int plaintextSize,
                                      ByteBuffer plaintext) {}
