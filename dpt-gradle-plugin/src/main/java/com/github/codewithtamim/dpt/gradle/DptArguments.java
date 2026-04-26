package com.github.codewithtamim.dpt.gradle;

import org.gradle.api.Project;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

final class DptArguments {

    private DptArguments() {}

    static List<String> build(Project project, DptExtension ext, File apk, String variantName) {
        return build(
                project,
                ext,
                apk,
                variantName,
                DptProtectConfigWriter.resolve(project, ext, variantName));
    }

    static List<String> build(
            Project project, DptExtension ext, File apk, String variantName, String protectConfigPath) {
        List<String> args = new ArrayList<>();
        args.add("--" + DptCliNames.PACKAGE_FILE);
        args.add(apk.getAbsolutePath());

        if (protectConfigPath != null) {
            args.add("--" + DptCliNames.PROTECT_CONFIG);
            args.add(protectConfigPath);
        }

        if (PropertyMerge.mergeBoolean(project, "dpt.debuggable", ext.getDebuggable().get())) {
            args.add("--" + DptCliNames.DEBUGGABLE);
        }
        if (PropertyMerge.mergeBoolean(project, "dpt.disableAppComponentFactory", ext.getDisableAppComponentFactory().get())) {
            args.add("--" + DptCliNames.DISABLE_ACF);
        }
        if (PropertyMerge.mergeBoolean(project, "dpt.dumpCode", ext.getDumpCode().get())) {
            args.add("--" + DptCliNames.DUMP_CODE);
        }

        String excludeAbi = PropertyMerge.resolveExcludeAbi(project, ext);
        if (!excludeAbi.isEmpty()) {
            args.add("--" + DptCliNames.EXCLUDE_ABI);
            args.add(excludeAbi);
        }

        if (PropertyMerge.mergeBoolean(project, "dpt.keepClasses", ext.getKeepClasses().get())) {
            args.add("--" + DptCliNames.KEEP_CLASSES);
        }
        if (PropertyMerge.mergeBoolean(project, "dpt.noisyLog", ext.getNoisyLog().get())) {
            args.add("--" + DptCliNames.NOISY_LOG);
        }

        File outDir = PropertyMerge.resolveOutputDirectory(project, ext, variantName);
        args.add("--" + DptCliNames.OUTPUT);
        args.add(outDir.getAbsolutePath());

        String rules = PropertyMerge.resolveRulesFile(project, ext);
        if (rules != null) {
            args.add("--" + DptCliNames.RULES_FILE);
            args.add(rules);
        }

        if (PropertyMerge.mergeBoolean(project, "dpt.smaller", ext.getSmaller().get())) {
            args.add("--" + DptCliNames.SMALLER);
        }
        if (PropertyMerge.mergeBoolean(project, "dpt.verifySign", ext.getVerifySign().get())) {
            args.add("--" + DptCliNames.VERIFY_SIGN);
        }
        if (PropertyMerge.mergeBoolean(project, "dpt.noSign", ext.getNoSign().get())) {
            args.add("--" + DptCliNames.NO_SIGN);
        }

        return args;
    }
}
