/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.common;

import java.util.Random;

/**
 * Ambient information a transformation may need beyond the value it's transforming - a source of
 * randomness, and key material - supplied at invocation time rather than baked into the transformation at
 * build time. This lets one built transformation tree be reused with a different {@code Context} later: a
 * fresh {@link Random} per record (without rebuilding the tree), or a different key per record (e.g. a
 * different key for different tenants sharing one topic) - or, for the common case, just the same values
 * every time.
 * @param random the source of randomness
 * @param key the raw key material used for HMAC/encrypt/decrypt operations
 */
// Record array components get identity-based equals/hashCode instead of content-based, same as the
// byte[] key already accepted (without a record) on HmacStringFunction/EncryptStringFunction/
// DecryptStringFunction - nothing in this module compares two Contexts for equality, so this is fine.
@SuppressWarnings("ArrayRecordComponent")
public record Context(Random random, byte[] key) {}
