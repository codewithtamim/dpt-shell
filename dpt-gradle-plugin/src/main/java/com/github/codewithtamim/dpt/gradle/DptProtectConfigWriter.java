package com.github.codewithtamim.dpt.gradle;

import org.gradle.api.GradleException;
import org.gradle.api.Project;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Objects;

final class DptProtectConfigWriter {

    private DptProtectConfigWriter() {}

    /**
     * Explicit file or {@code -Pdpt.protectConfig} wins. Otherwise, if {@link #hasInlineProtect(DptExtension)}, writes
     * {@code build/dpt-shell/generated/dpt-protect-<variant>.json} and returns its path.
     */
    static String resolve(Project project, DptExtension ext, String variantName) {
        String explicit = PropertyMerge.resolveExplicitProtectConfig(project, ext);
        if (explicit != null) {
            return explicit;
        }
        if (!hasInlineProtect(ext)) {
            return null;
        }
        File dir =
                project.getLayout().getBuildDirectory().dir("dpt-shell/generated").get().getAsFile();
        try {
            Files.createDirectories(dir.toPath());
        } catch (IOException e) {
            throw new GradleException("dpt: failed to create " + dir, e);
        }
        File out = new File(dir, "dpt-protect-" + variantName + ".json");
        String json = toJson(ext.getProtect());
        try {
            Files.write(out.toPath(), json.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new GradleException("dpt: failed to write " + out, e);
        }
        return out.getAbsolutePath();
    }

    static boolean hasInlineProtect(DptExtension ext) {
        DptProtectExtension p = ext.getProtect();
        if (nonBlank(p.getShellPackageName().getOrNull())) {
            return true;
        }
        if (nonBlank(p.getAppSignSha256().getOrNull())) {
            return true;
        }
        if (nonBlank(p.getDexSign().getOrNull())) {
            return true;
        }
        DptSignatureExtension s = p.getSignature();
        if (s.getKeystore().isPresent()) {
            return true;
        }
        return nonBlank(s.getAlias().getOrNull())
                || nonBlank(s.getStorePassword().getOrNull())
                || nonBlank(s.getKeyPassword().getOrNull());
    }

    private static boolean nonBlank(String s) {
        return s != null && !s.trim().isEmpty();
    }

    static String toJson(DptProtectExtension p) {
        String shell = p.getShellPackageName().getOrElse("").trim();
        if (shell.isEmpty()) {
            shell = "<random>";
        }
        StringBuilder sb = new StringBuilder();
        sb.append('{');
        sb.append("\"shellPkgName\":\"").append(jsonEscape(shell)).append('"');
        String appSha = p.getAppSignSha256().getOrElse("").trim();
        if (!appSha.isEmpty()) {
            sb.append(",\"app_sign_sha256\":\"").append(jsonEscape(appSha)).append('"');
        }
        String dexSign = p.getDexSign().getOrElse("").trim();
        if (!dexSign.isEmpty()) {
            sb.append(",\"dex_sign\":\"").append(jsonEscape(dexSign)).append('"');
        }
        appendSignature(sb, p.getSignature());
        sb.append('}');
        return sb.toString();
    }

    private static void appendSignature(StringBuilder sb, DptSignatureExtension s) {
        boolean hasKeystore = s.getKeystore().isPresent();
        boolean hasAlias = nonBlank(s.getAlias().getOrNull());
        boolean hasStore = nonBlank(s.getStorePassword().getOrNull());
        boolean hasKey = nonBlank(s.getKeyPassword().getOrNull());
        if (!hasKeystore && !hasAlias && !hasStore && !hasKey) {
            return;
        }
        sb.append(",\"signature\":{");
        boolean first = true;
        if (hasKeystore) {
            String path = s.getKeystore().getAsFile().get().getAbsolutePath();
            sb.append("\"keystore\":\"").append(jsonEscape(path)).append('"');
            first = false;
        }
        if (hasAlias) {
            if (!first) {
                sb.append(',');
            }
            sb.append("\"alias\":\"").append(jsonEscape(Objects.requireNonNull(s.getAlias().getOrNull()).trim()))
                    .append('"');
            first = false;
        }
        if (hasStore) {
            if (!first) {
                sb.append(',');
            }
            sb.append("\"storepass\":\"")
                    .append(jsonEscape(Objects.requireNonNull(s.getStorePassword().getOrNull()).trim()))
                    .append('"');
            first = false;
        }
        if (hasKey) {
            if (!first) {
                sb.append(',');
            }
            sb.append("\"keypass\":\"")
                    .append(jsonEscape(Objects.requireNonNull(s.getKeyPassword().getOrNull()).trim()))
                    .append('"');
        }
        sb.append('}');
    }

    private static String jsonEscape(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
