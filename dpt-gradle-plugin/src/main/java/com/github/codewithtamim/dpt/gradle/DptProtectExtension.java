package com.github.codewithtamim.dpt.gradle;

import org.gradle.api.Action;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Nested;

/**
 * Inline protect config (same data as {@code dpt-protect.json}). Used only when no {@code protectConfig} file or
 * {@code dpt.protectConfig} property is set.
 */
public abstract class DptProtectExtension {

    /** Passed through as JSON {@code shellPkgName}; use {@code "<random>"} for DPT-generated name. */
    public abstract Property<String> getShellPackageName();

    /**
     * Extra certificate SHA-256 values written as {@code app_sign_sha256}. Comma-separated. Colon-separated
     * Play Console fingerprints are accepted. When {@code verifySign} is on, the signing keystore certificate
     * is added as well. The shell stops the app if the installed certificate matches none of them.
     */
    public abstract Property<String> getAppSignSha256();

    public abstract Property<String> getDexSign();

    @Nested
    public abstract DptSignatureExtension getSignature();

    public void signature(Action<? super DptSignatureExtension> action) {
        action.execute(getSignature());
    }
}
