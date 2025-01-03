/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.proxy.config.secret;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

import edu.umd.cs.findbugs.annotations.NonNull;

public class SecretUtils {
    private SecretUtils() {
    }

    @NonNull
    static String readPasswordFile(String passwordFile) {
        try (var fr = new BufferedReader(new FileReader(passwordFile, StandardCharsets.UTF_8))) {
            return fr.readLine();
        }
        catch (IOException e) {
            throw new UncheckedIOException("Exception reading " + passwordFile, e);
        }
    }

    public static String getProvidedPassword(FilePassword filePassword) {
        return readPasswordFile(filePassword.passwordFile());
    }

    public static String getProvidedPassword(InlinePassword inlinePassword) {
        return inlinePassword.password();
    }

    public static String getProvidedPassword(PasswordProvider passwordProvider) {
        if (passwordProvider instanceof FilePassword fp) {
            return getProvidedPassword(fp);
        }
        else if (passwordProvider instanceof InlinePassword inlinePassword) {
            return getProvidedPassword(inlinePassword);
        }
        throw new IllegalArgumentException();
    }
}
