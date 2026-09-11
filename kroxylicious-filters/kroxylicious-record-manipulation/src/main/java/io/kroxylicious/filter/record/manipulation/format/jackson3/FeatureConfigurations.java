/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.format.jackson3;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

import edu.umd.cs.findbugs.annotations.Nullable;
import tools.jackson.core.util.JacksonFeature;
import tools.jackson.databind.cfg.ConfigFeature;

class FeatureConfigurations {
    private FeatureConfigurations() {
    }

    static <E extends Enum<E> & JacksonFeature> Map<E, Boolean> asJacksonFeatureMap(@Nullable Map<String, Boolean> map, Class<E> enumClass) {
        return asFeatureMap(map, enumClass, JacksonFeature::enabledByDefault);
    }

    static <E extends Enum<E> & ConfigFeature> Map<E, Boolean> asConfigFeatureMap(@Nullable Map<String, Boolean> map, Class<E> enumClass) {
        return asFeatureMap(map, enumClass, ConfigFeature::enabledByDefault);
    }

    static <E extends Enum<E>> Map<E, Boolean> asFeatureMap(@Nullable Map<String, Boolean> map, Class<E> enumClass,
                                                            Predicate<E> enabledByDefault) {
        if (map == null) {
            return Map.of();
        }
        var result = new HashMap<E, Boolean>();
        for (Map.Entry<String, Boolean> entry : map.entrySet()) {
            String camelCase = entry.getKey();
            E e = Enum.valueOf(enumClass, camelCase);
            if ((!enabledByDefault.test(e) && entry.getValue())
                    || (enabledByDefault.test(e) && !entry.getValue())) {
                result.put(e, entry.getValue());
            }
        }
        return result;
    }

}
