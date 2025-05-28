/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.proxy.router;

import java.util.concurrent.CompletionStage;

import org.apache.kafka.common.message.RequestHeaderData;
import org.apache.kafka.common.protocol.ApiKeys;
import org.apache.kafka.common.protocol.ApiMessage;

public interface Router {

    CompletionStage<RoutingResult> onClientRequest(short apiVersion,
                                                   ApiKeys apiKey,
                                                   RequestHeaderData header,
                                                   ApiMessage request,
                                                   RoutingContext context);

//    // TODO routing responses???
//    onResponse(String fromRoute)

}