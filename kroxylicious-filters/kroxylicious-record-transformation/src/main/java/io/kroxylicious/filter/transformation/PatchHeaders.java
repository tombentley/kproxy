/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeader;

/**
 * Simple transformations on record headers, using a given <a href="https://datatracker.ietf.org/doc/html/rfc6902">JSON Patch</a>-style
 * sequence of operations.
 */
class PatchHeaders implements HeadersTransformation {

    sealed interface HeaderOperation permits TestFirst, RemoveFirst, AddLast, ReplaceFirst, MoveFirst, CopyFirst {
        String key();

        default boolean matches(String key) {
            return key().equals(key);
        }
    }

    record TestFirst(String key, byte[] value) implements HeaderOperation {
    }

    record RemoveFirst(String key) implements HeaderOperation {
    } // RemoveLast, RemoveAll

    record AddLast(String key, byte[] value) implements HeaderOperation {
    } // Prepend

    record ReplaceFirst(String key, byte[] value) implements HeaderOperation {
    }// ReplaceLast

    record MoveFirst(String from, String key) implements HeaderOperation {
    }

    record CopyFirst(String from, String key) implements HeaderOperation {
    }

    private final List<HeaderOperation> operations;

    PatchHeaders(List<HeaderOperation> operations) {
        this.operations = operations;
    }

    public List<Header> transformHeaders(List<Header> originalHeaders) {
        List<Header> currentHeaders = originalHeaders;
        for (var operation : operations) {
            var result = new ArrayList<Header>();
            if (operation instanceof TestFirst) {
                for (Header header : currentHeaders) {
                    if (operation.matches(header.key())) {
                        if (!Arrays.equals(header.value(), header.value())) {
                            return originalHeaders; // return the original headers
                        }
                    }
                }
            }
            else if (operation instanceof AddLast addLast) {
                result.addAll(currentHeaders);
                result.add(new RecordHeader(addLast.key(), addLast.value()));
            }
            else if (operation instanceof RemoveFirst) {
                boolean done = false;
                for (Header header : currentHeaders) {
                    if (!done && operation.matches(header.key())) {
                        done = true;
                    }
                    else {
                        result.add(header);
                    }
                }
            }
            else if (operation instanceof ReplaceFirst replaceFirst) {
                boolean done = false;
                for (Header header : currentHeaders) {
                    if (!done && operation.matches(header.key())) {
                        result.add(new RecordHeader(header.key(), replaceFirst.value()));
                        done = true;
                    }
                    else {
                        result.add(header);
                    }
                }
            }
            else if (operation instanceof MoveFirst move) {
                boolean done = false;
                for (Header header : currentHeaders) {
                    if (!done && move.from().matches(header.key())) {
                        result.add(new RecordHeader(move.key(), header.value()));
                        done = true;
                    }
                    else {
                        result.add(header);
                    }
                }
            }
            else if (operation instanceof CopyFirst copy) {
                boolean done = false;
                for (Header header : currentHeaders) {
                    if (!done && copy.from().matches(header.key())) {
                        result.add(header);
                        result.add(new RecordHeader(copy.key(), header.value()));
                        done = true;
                    }
                    else {
                        result.add(header);
                    }
                }
            }
            currentHeaders = result;
        }
        return currentHeaders;
    }
}
