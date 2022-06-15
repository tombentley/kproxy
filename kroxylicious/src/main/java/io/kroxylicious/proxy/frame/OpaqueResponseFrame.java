/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.kroxylicious.proxy.frame;

import io.netty.buffer.ByteBuf;

public class OpaqueResponseFrame extends OpaqueFrame implements ResponseFrame {
    public OpaqueResponseFrame(ByteBuf buf, int correlationId, int length) {
        super(buf, correlationId, length);
    }

    @Override
    public String toString() {
        int index = buf.readerIndex();
        try {
            var correlationId = buf.readInt();
            return getClass().getSimpleName() + "(" +
                    "length=" + length +
                    ", correlationId=" + correlationId +
                    ", buf=" + buf +
                    ')';
        }
        finally {
            buf.readerIndex(index);
        }
    }
}