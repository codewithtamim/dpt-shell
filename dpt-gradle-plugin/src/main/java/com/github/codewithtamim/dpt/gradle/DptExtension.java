package com.github.codewithtamim.dpt.gradle;

import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;

import javax.inject.Inject;

public abstract class DptExtension {

    @Inject
    public DptExtension(ObjectFactory objects) {
        getEnabled().convention(false);
        getApplyToRelease().convention(true);
        getApplyToDebug().convention(false);
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

    public abstract RegularFileProperty getProtectConfig();

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
