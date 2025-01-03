/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.proxy.config.secret;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A reference to the file containing a plain text password in UTF-8 encoding.  If the password file
 * contains more than one line, only the characters of the first line are taken to be the password,
 * excluding the line ending.  Subsequent lines are ignored.
 * <br>
 * Note that the {@code filePath} alias was deprecated at 0.5.0.
 *
 * @param passwordFile file containing the password.
 */
public record FilePassword(@JsonProperty(required = true) @JsonAlias("filePath") String passwordFile) implements PasswordProvider {

    public FilePassword {
        Objects.requireNonNull(passwordFile);
    }

    @Override
    public String toString() {
        return "FilePassword[" +
                "passwordFile=" + passwordFile + ']';
    }

}
