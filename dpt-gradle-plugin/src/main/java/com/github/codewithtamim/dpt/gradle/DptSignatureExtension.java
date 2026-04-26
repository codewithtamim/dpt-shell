package com.github.codewithtamim.dpt.gradle;

import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;

/**
 * Maps to the {@code signature} object in {@code dpt}'s protect JSON (keystore, alias, storepass, keypass).
 */
public abstract class DptSignatureExtension {

    public abstract RegularFileProperty getKeystore();

    public abstract Property<String> getAlias();

    public abstract Property<String> getStorePassword();

    public abstract Property<String> getKeyPassword();
}
