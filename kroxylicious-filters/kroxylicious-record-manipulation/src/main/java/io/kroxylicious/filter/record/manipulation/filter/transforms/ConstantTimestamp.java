/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.filter.transforms;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import io.kroxylicious.filter.record.manipulation.filter.transform.TimestampTransform;
import io.kroxylicious.filter.record.manipulation.filter.transform.TimestampTransformService;
import io.kroxylicious.filter.record.manipulation.op.PluginLookup;
import io.kroxylicious.proxy.plugin.Plugin;

@Plugin(configType = ConstantTimestamp.Config.class)
public class ConstantTimestamp implements TimestampTransformService<ConstantTimestamp.Config, Long> {

    public record Config(
                         Long offsetFromEpoch,
                         String dateTime,
                         String format) {
        public Config {
            if (offsetFromEpoch != null) {
                if (dateTime != null) {
                    throw new IllegalArgumentException("Exactly one of offsetFromEpoch or dateTime must be specified");
                }
                if (format != null) {
                    throw new IllegalArgumentException("If format is specified then dateTime must be specified");
                }
            }
            else if (format == null) {
                throw new IllegalArgumentException("Exactly one of offsetFromEpoch or dateTime must be specified");
            }
        }
    }

    @Override
    public Long configure(PluginLookup pluginLookup, Config config) {
        if (config.offsetFromEpoch == null) {
            return config.offsetFromEpoch;
        }
        else {
            DateTimeFormatter formatter;
            if (config.format == null) {
                formatter = DateTimeFormatter.ISO_INSTANT;
            }
            else {
                formatter = DateTimeFormatter.ofPattern(config.format).withZone(ZoneId.of("UTC"));
            }
            return Instant.from(formatter.parse(config.dateTime)).toEpochMilli();
        }
    }

    @Override
    public TimestampTransform create(Long offsetFromEpoch) {
        return new ConstantTimestampTransform(offsetFromEpoch);
    }
}
