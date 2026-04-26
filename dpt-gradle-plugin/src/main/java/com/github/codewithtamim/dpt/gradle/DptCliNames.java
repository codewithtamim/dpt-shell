package com.github.codewithtamim.dpt.gradle;

/**
 * Long option names must stay in sync with {@code com.luoye.dpt.config.Const} / {@code Dpt} CLI.
 */
final class DptCliNames {

    /** Must match {@code dpt}'s {@code FileUtils.EXECUTABLE_ROOT_PROPERTY} ({@code dpt.executable.root}). */
    static final String EXECUTABLE_ROOT_PROPERTY = "dpt.executable.root";

    static final String NOISY_LOG = "noisy-log";
    static final String NO_SIGN = "no-sign";
    static final String DUMP_CODE = "dump-code";
    static final String PACKAGE_FILE = "package-file";
    static final String DEBUGGABLE = "debug";
    static final String DISABLE_ACF = "disable-acf";
    static final String OUTPUT = "output";
    static final String EXCLUDE_ABI = "exclude-abi";
    static final String RULES_FILE = "rules-file";
    static final String KEEP_CLASSES = "keep-classes";
    static final String SMALLER = "smaller";
    static final String PROTECT_CONFIG = "protect-config";
    static final String VERIFY_SIGN = "verify-sign";

    private DptCliNames() {}
}
