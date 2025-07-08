/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.proxy.filter;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletionStage;

import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.message.ProduceRequestData;
import org.apache.kafka.common.message.RequestHeaderData;
import org.apache.kafka.common.record.MemoryRecords;
import org.apache.kafka.common.record.Record;
import org.apache.kafka.common.record.RecordBatch;

import io.kroxylicious.kafka.transform.RecordStream;
import io.kroxylicious.kafka.transform.RecordTransform;
import io.kroxylicious.proxy.authentication.ClientConnectionAware;

import edu.umd.cs.findbugs.annotations.Nullable;

public class ClientTlsAwareContract implements ClientConnectionAware, ProduceRequestFilter  {

    public static final String HEADER_KEY_CLIENT_CONNECTION_TLS = "clientConnectionTls";
    public static final String HEADER_KEY_CLIENT_PRINCIPAL_NAME = "clientPrincipalName";
    public static final String HEADER_KEY_PROXY_PRINCIPAL_NAME = "proxyPrincipalName";
    private long threadId;
    private @Nullable String clientPrincipalName;
    private boolean clientConnectionTls;
    private @Nullable String proxyPrincipalName;

    @Override
    public void onClientConnection(Context context) {
        this.threadId = Thread.currentThread().threadId();
        this.clientConnectionTls = context.isClientConnectionTls();
        X509Certificate x509Certificate = context.clientCertificate();
        this.clientPrincipalName = x509Certificate == null ? null : x509Certificate.getSubjectX500Principal().getName();
        X509Certificate x509Certificate1 = context.proxyServerCertificate();
        this.proxyPrincipalName = x509Certificate1 == null ? null : x509Certificate1.getSubjectX500Principal().getName();
    }

    @Override
    public CompletionStage<RequestFilterResult> onProduceRequest(short apiVersion,
                                                                 RequestHeaderData header,
                                                                 ProduceRequestData request,
                                                                 FilterContext context) {
        if (this.threadId != Thread.currentThread().threadId()) {
            throw new IllegalStateException(this.threadId + " != " + Thread.currentThread().threadId());
        }
        for (ProduceRequestData.TopicProduceData topicDatum : request.topicData()) {
            for (ProduceRequestData.PartitionProduceData partitionDatum : topicDatum.partitionData()) {
                MemoryRecords records = (MemoryRecords) partitionDatum.records();
                partitionDatum.setRecords(RecordStream.ofRecords(records)
                        .toMemoryRecords(
                                context.createByteBufferOutputStream(records.sizeInBytes()),
                                new RecordTransform<Void>() {
                                    @Override
                                    public void initBatch(RecordBatch batch) {

                                    }

                                    @Override
                                    public void init(@Nullable Void state, Record record) {

                                    }

                                    @Override
                                    public void resetAfterTransform(Void state, Record record) {

                                    }

                                    @Override
                                    public long transformOffset(Record record) {
                                        return record.offset();
                                    }

                                    @Override
                                    public long transformTimestamp(Record record) {
                                        return record.timestamp();
                                    }

                                    @Nullable
                                    @Override
                                    public ByteBuffer transformKey(Record record) {
                                        return record.key();
                                    }

                                    @Nullable
                                    @Override
                                    public ByteBuffer transformValue(Record record) {
                                        return record.value();
                                    }

                                    @Nullable
                                    @Override
                                    public Header[] transformHeaders(Record record) {
                                        return getHeaders(record);
                                    }
                                }));
            }
        }
        return context.forwardRequest(header, request);
    }

    private Header[] getHeaders(Record record) {
        List<Header> headers = new ArrayList<>(Arrays.asList(record.headers()));
        headers.add(new RecordHeader(HEADER_KEY_CLIENT_CONNECTION_TLS,
                new byte[]{clientConnectionTls ? (byte) 1 : (byte) 0}));
        headers.add(new RecordHeader(HEADER_KEY_CLIENT_PRINCIPAL_NAME,
                clientPrincipalName == null ? null : clientPrincipalName.getBytes(StandardCharsets.UTF_8)));
        headers.add(new RecordHeader(HEADER_KEY_PROXY_PRINCIPAL_NAME,
                proxyPrincipalName == null ? null : proxyPrincipalName.getBytes(StandardCharsets.UTF_8)));
        return headers.toArray(new Header[0]);
    }
}

