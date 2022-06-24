/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

/**
 * <h3>NetFilter state machine</h3>
 *
 * <p>The process of establishing a proxy channel (which is composed of two Netty Channels) to an upstream cluster
 * can be described at a high level using the following state machine:</p>
 * <pre><code>
 *   START
 *   │   │
 *   │   ↓
 *   │  TLS
 *   │   │
 *   ╰───╮
 *       │
 * ╭────┬┴────────╮
 * │    ↓         ↓
 * │  PROXY ──→ KAFKA
 * ╰───→╰─────────╰───→ UPSTREAM_CONNECT
 * </code></pre>
 *
 * <dl>
 *     <dt>TLS</dt>
 *     <dd>TLS protocol handshake (ClientHello etc).
 *         See {@link io.netty.handler.ssl.SslHandler}.
 *         This can propagate SNI information to installed net filters.
 *         </dd>
 *
 *     <dt>PROXY</dt>
 *     <dd>This refers to the HAProxy PROXY protocol, which is optionally supported
 *         for when this Kafka proxy is deployed behind an HAProxy layer.
 *         See {@link io.kroxylicious.proxy.internal.PROXYNetHandler}.
 *         This can propagate client connection information to installed net filters.
 *         </dd>
 *
 *     <dt>KAFKA</dt>
 *     <dd>This is used when establishing an proxy channel requires interpreting some of the
 *         Kafka protocol frames to perform Kafka SASL authentication.
 *         See {@link io.kroxylicious.proxy.internal.KafkaAuthnHandler}.
 *         This can propagate client version, and authorized id information to installed net filters.
 *         </dd>
 *
 *     <dt>UPSTREAM_CONNECT</dt>
 *     <dd>Using the information obtained from any previous states in the state machine
 *         a connection to an upstream Kafka cluster(s) is made.
 *         See {@link io.kroxylicious.proxy.internal.NetHandler}.</dd>
 * </dl>
 */
package io.kroxylicious.proxy.internal;