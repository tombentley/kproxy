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
        private final @Nullable SchemaObject parentSchema;

        private Context(
                        Context parent,
                        String keyword,
                        String path,
                        SchemaObject parentSchema) {
            this.parent = Objects.requireNonNull(parent);
            this.parentSchema = Objects.requireNonNull(parentSchema);
            this.diagnostics = parent.diagnostics;
            this.keyword = Objects.requireNonNull(keyword);
            this.path = Objects.requireNonNull(path);
        }

        Context(
                Reporting diagnostics,
                URI base) {
            this.diagnostics = Objects.requireNonNull(diagnostics);
            this.parent = Objects.requireNonNull(base);
            this.parentSchema = null;
            this.keyword = "";
            this.path = "";
        }

        Context sub(String keyword,
                    String path,
                    SchemaObject parentSchema) {
            return new SchemaVisitor.Context(this, keyword, path, parentSchema);
        }

        /**
         * @return The URI from which the root schema was loaded
         */
        public URI base() {
            if (parent instanceof URI uri) {
                return uri;
            }
            else {
                return ((Context) parent).base();
            }
        }

        /**
         * @return The context of the parent schema, or null if this is the context for the root schema
         */
        public @Nullable Context parent() {
            if (parent instanceof URI) {
                return null;
            }
            else {
                return ((Context) parent);
            }
        }

        public @Nullable SchemaObject parentSchema() {
            return parentSchema;
        }

        /**
         * @return The schema keyword (with respect to the {@link #parentSchema()}) in the context of which we're visiting this schema
         */
        public String keyword() {
            return keyword;
        }

        /**
         * @return The JSON pointer from the root schema to the schema object for this context.
         * For the root schema this will be the empty string.
         */
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

        /**
         * @return true if, and only if, this is the context for the top-level schema of the document.
         */
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
                                Object... arguments) {
            diagnostics.reportFatal(message, arguments);
        }

        @Override
        public void reportError(
                                String message,
                                Object... arguments) {
            diagnostics.reportError(message, arguments);
        }

        @Override
        public void reportWarning(
                                  String message,
                                  Object... arguments) {
            diagnostics.reportWarning(message, arguments);
        }
    }

    public enum VisitAction {
        CONTINUE,
        SKIP_SUBTREE
    }

    public VisitAction enterSchema(Context context, @NonNull SchemaObject schema) {
        // default behaviour is no-op
        return VisitAction.CONTINUE;
    }

    public void exitSchema(Context context, @NonNull SchemaObject schema) {
        // default behaviour is no-op
    }
}
