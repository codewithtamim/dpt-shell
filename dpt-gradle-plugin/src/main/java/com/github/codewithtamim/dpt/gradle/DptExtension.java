package com.github.codewithtamim.dpt.gradle;

import org.gradle.api.Action;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Nested;

import javax.inject.Inject;

public abstract class DptExtension {

    @Inject
    public DptExtension(ObjectFactory objects) {
        getEnabled().convention(false);
        getApplyToRelease().convention(true);
        getApplyToDebug().convention(false);
        getApplyToBundle().convention(true);
        getDebuggable().convention(false);
        getDisableAppComponentFactory().convention(false);
        getDumpCode().convention(false);
        getKeepClasses().convention(false);
        getNoisyLog().convention(false);
        getSmaller().convention(false);
        getVerifySign().convention(false);
        getNoSign().convention(false);
        getExcludeAbi().convention("");
    }

    public abstract Property<Boolean> getEnabled();

    public abstract Property<Boolean> getApplyToRelease();

    public abstract Property<Boolean> getApplyToDebug();

    /**
     * When true (default), the matching bundle task output (.aab) is passed to {@code dpt} for variants that already
     * have a {@code dptProtect*} task. Set false to only protect APKs.
     */
    public abstract Property<Boolean> getApplyToBundle();

    /**
     * Optional path to a protect JSON file. When set (or {@code -Pdpt.protectConfig}), it takes precedence over
     * the {@code protect} { } DSL.
     */
    public abstract RegularFileProperty getProtectConfig();

    /**
     * Inline protect settings (same shape as {@code dpt-protect.json}). Used when {@link #getProtectConfig()} and
     * {@code dpt.protectConfig} are not set.
     */
    @Nested
    public abstract DptProtectExtension getProtect();

    public void protect(Action<? super DptProtectExtension> action) {
        action.execute(getProtect());
    }

    public abstract Property<Boolean> getDebuggable();

    public abstract Property<Boolean> getDisableAppComponentFactory();

    public abstract Property<Boolean> getDumpCode();

    public abstract Property<String> getExcludeAbi();

    public abstract Property<Boolean> getKeepClasses();

    public abstract Property<Boolean> getNoisyLog();

    public abstract DirectoryProperty getOutputDirectory();

    public abstract RegularFileProperty getRulesFile();

    public abstract Property<Boolean> getSmaller();

    public abstract Property<Boolean> getVerifySign();

    public abstract Property<Boolean> getNoSign();
}
