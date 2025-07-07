/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.proxy.authentication;

import java.security.Principal;
import java.util.Objects;

public class SaslPrincipal implements Principal {

    private final String type;
    private final String name;

    public SaslPrincipal(String type, String name) {
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.name = Objects.requireNonNull(name, "name must not be null");
    }

    public SaslPrincipal(String name) {
        this("user", name);
    }

    public String getType() {
        return this.type;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof SaslPrincipal that)) {
            return false;
        }
        return Objects.equals(type, that.type) && Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, name);
    }

    @Override
    public String toString() {
        return "SaslPrincipal{" +
                "type='" + type + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
}
