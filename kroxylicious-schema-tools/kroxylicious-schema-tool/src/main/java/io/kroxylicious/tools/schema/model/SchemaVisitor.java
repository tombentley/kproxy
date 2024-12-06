/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.tools.schema.model;

import java.net.URI;
import java.util.Objects;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;

public class SchemaVisitor {

    public static class Context implements Reporting {
        private final Object parent;
        private final String keyword;
        private final String path;
        private final Reporting diagnostics;

        private Context(
                Context parent,
                String keyword,
                String path) {
            this.parent = Objects.requireNonNull(parent);
            this.diagnostics = parent.diagnostics;
            this.keyword = Objects.requireNonNull(keyword);
            this.path = Objects.requireNonNull(path);
        }

        Context(
                Reporting diagnostics,
                URI base) {
            this.diagnostics = Objects.requireNonNull(diagnostics);
            this.parent = Objects.requireNonNull(base);
            this.keyword = "";
            this.path = "";
        }

        Context sub(String keyword,
                    String path) {
            return new SchemaVisitor.Context(this, keyword, path);
        }

        public URI base() {
            if (parent instanceof URI uri) {
                return uri;
            }
            else {
                return ((Context) parent).base();
            }
        }

        public @Nullable Context parent() {
            if (parent instanceof URI uri) {
                return null;
            }
            else {
                return ((Context) parent);
            }
        }

        public String keyword() {
            return keyword;
        }

        public String fullPath() {
            // TODO rename this method to pointer() and the `path` field to `pointerSegment`???
            if (parent instanceof Context parentContext) {
                // TODO quoting according to JSON pointer
                return parentContext.fullPath() + "/" + path;
            }
            else {
                return path;
            }
        }

        public boolean isRootSchema() {
            return parent instanceof URI;
        }

        @Override
        public String toString() {
            return "Context{" +
                    (parent instanceof URI ? "base=" + parent + ", " : "") +
                    "keyword='" + keyword + '\'' +
                    ", fullPath='" + fullPath() + '\'' +
                    '}';
        }

        @Override
        public void reportFatal(
                String message,
                Object... arguments
        ) {
            diagnostics.reportFatal(message, arguments);
        }

        @Override
        public void reportError(
                String message,
                Object... arguments
        ) {
            diagnostics.reportError(message, arguments);
        }

        @Override
        public void reportWarning(
                String message,
                Object... arguments
        ) {
            diagnostics.reportWarning(message, arguments);
        }
    }

    public void enterSchema(Context context, @NonNull SchemaObject schema) {
        // default behaviour is no-op
    }

    public void exitSchema(Context context, @NonNull SchemaObject schema) {
        // default behaviour is no-op
    }
}
