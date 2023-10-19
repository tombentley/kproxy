/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.kms.service;

import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serializer;

/**
 * A service interface for {@link DekGenerator}
 * @param <C> The type of config
 * @param <Kr> The type of Key ref
 * @param <Ed> The type of edek
 */
public interface DekGeneratorService<C, Kr, Ed> {

    DekGenerator<Kr, Ed> dekGenerator();

    Deserializer<Kr> keyRefDeserializer();

    Serializer<Ed> edekSerializer();

}
