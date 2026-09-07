/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.kafka;

import edu.umd.cs.findbugs.annotations.Nullable;

/**
 * transformedRecord:
 * - into: value
 *   from:
 *     - Key k
 *     - Value v
 *   applying:
 *     - op: JsonObjectTemplate
 *       value: '{"key": $k, "value": $v}'
 *
 *
 * Example:
 * <pre>{@code
 * transformedRecord:
 *   intoTimestamp:
 *     from: Timestamp # override an existing timestamp with a constant
 *     apply:
 *       - op: Value
 *         value: 1970-01-01T00:00:00.000
 *   intoRecordKey: # identity transformation on record keys (same as if `toRecordKey` were omitted)
 *     from: RecordKey
 *   intoRecordValue:
 *     from: RecordValue
 *     apply:
 *       - op: ValidateSignature # optional: We can validate the Json
 *         #...
 *       - op: DeserializeJson
 *       - op: ValidateJsonSchema # optional: We can validate the Json
 *         schema:
 *           type: object
 *           required:
 *             - firstName
 *             - surname
 *             - dob
 *           properties:
 *             firstName:
 *               type: string
 *             surname:
 *               type: string
 *             dob:
 *               type: string
 *               format: date
 *       - op: Transform
 *         schema:
 *           type: object
 *           properties:
 *             firstName:
 *               type: string
 *               apply:
 *               - op: Value
 *                 value: REDACTED
 *             surname:
 *               type: string
 *               apply:
 *               - op: Value
 *                 value: REDACTED
 *             dob:
 *               type: string
 *               apply:
 *               - op: ReplaceRegex # map every date of birth to the 1st of January in the same year
 *                 pattern: ([0-9]{4})-[0-9]{2}-[0-9]{2}
 *                 replacement: $1-01-01
 *       - op: SerializeJson
 *       - op: Sign # optionally sign the value
 * }</pre>
 */
public record RecordTransformConfig(
                                    @Nullable PipelineConfig intoTimestamp,
                                    @Nullable PipelineConfig intoRecordKey,
                                    @Nullable PipelineConfig intoRecordValue) {

}
