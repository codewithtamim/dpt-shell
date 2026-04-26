package com.github.codewithtamim.dpt.gradle;

import org.gradle.api.GradleException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

final class DptRuntimeExtractor {

    static final String RUNTIME_ZIP_RESOURCE = "dpt-runtime.zip";

    private DptRuntimeExtractor() {}

    static void extractTo(Path targetDir) throws IOException {
        Files.createDirectories(targetDir);
        try (InputStream in = DptRuntimeExtractor.class.getClassLoader().getResourceAsStream(RUNTIME_ZIP_RESOURCE)) {
            if (in == null) {
                throw new GradleException(
                    "Missing " + RUNTIME_ZIP_RESOURCE + " on classpath; rebuild the dpt-gradle-plugin with :dpt:jar and :shell:assembleRelease outputs.");
            }
            try (ZipInputStream zis = new ZipInputStream(in)) {
                ZipEntry e;
                while ((e = zis.getNextEntry()) != null) {
                    Path dest = targetDir.resolve(e.getName()).normalize();
                    if (!dest.startsWith(targetDir.normalize())) {
                        throw new GradleException("Bad zip entry: " + e.getName());
                    }
                    if (e.isDirectory()) {
                        Files.createDirectories(dest);
                    } else {
                        Files.createDirectories(dest.getParent());
                        try (OutputStream out = Files.newOutputStream(dest)) {
                            zis.transferTo(out);
                        }
                    }
                    zis.closeEntry();
                }
            }
        }
    }
}
