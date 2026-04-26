package com.github.codewithtamim.dpt.gradle;

import org.gradle.api.Project;

import java.io.File;

final class PropertyMerge {

    private PropertyMerge() {}

    static boolean mergeBoolean(Project project, String key, boolean extensionValue) {
        Object p = project.findProperty(key);
        if (p == null) {
            return extensionValue;
        }
        if (p instanceof Boolean) {
            return (Boolean) p;
        }
        return Boolean.parseBoolean(p.toString());
    }

    /**
     * Project property wins when non-null and non-empty string.
     */
    static String mergePath(Project project, String key, String extensionPath) {
        Object p = project.findProperty(key);
        if (p != null) {
            String s = p.toString().trim();
            if (!s.isEmpty()) {
                return s;
            }
        }
        return extensionPath;
    }

    static File resolveOutputDirectory(Project project, DptExtension ext, String variantName) {
        Object p = project.findProperty("dpt.output");
        if (p != null) {
            String s = p.toString().trim();
            if (!s.isEmpty()) {
                return new File(s);
            }
        }
        if (ext.getOutputDirectory().isPresent()) {
            return ext.getOutputDirectory().get().getAsFile();
        }
        return new File(project.getLayout().getBuildDirectory().getAsFile().get(), "outputs/dpt/" + variantName);
    }

    static String resolveRulesFile(Project project, DptExtension ext) {
        String fromProp = mergePath(project, "dpt.rulesFile", null);
        if (fromProp != null) {
            return fromProp;
        }
        if (ext.getRulesFile().isPresent()) {
            return ext.getRulesFile().get().getAsFile().getAbsolutePath();
        }
        return null;
    }

    static String resolveProtectConfig(Project project, DptExtension ext) {
        String fromProp = mergePath(project, "dpt.protectConfig", null);
        if (fromProp != null) {
            return fromProp;
        }
        if (ext.getProtectConfig().isPresent()) {
            return ext.getProtectConfig().get().getAsFile().getAbsolutePath();
        }
        return null;
    }

    static String resolveExcludeAbi(Project project, DptExtension ext) {
        Object p = project.findProperty("dpt.excludeAbi");
        if (p != null) {
            String s = p.toString().trim();
            if (!s.isEmpty()) {
                return s;
            }
        }
        String fromExt = ext.getExcludeAbi().getOrElse("");
        return fromExt == null ? "" : fromExt.trim();
    }
}
