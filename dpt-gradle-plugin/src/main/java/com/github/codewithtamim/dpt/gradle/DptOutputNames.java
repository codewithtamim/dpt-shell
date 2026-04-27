package com.github.codewithtamim.dpt.gradle;

/**
 * Matches {@code com.luoye.dpt.util.FileUtils#getNewFileName(String, String)} with tag {@code signed}
 * for the final protected package name.
 */
final class DptOutputNames {

    private DptOutputNames() {}

    static String signedArtifactName(String packageFileName) {
        int lastDot = packageFileName.lastIndexOf('.');
        if (lastDot < 0) {
            return packageFileName + "_signed";
        }
        String suffix = packageFileName.substring(lastDot + 1);
        return packageFileName.substring(0, lastDot) + "_signed." + suffix;
    }
}
