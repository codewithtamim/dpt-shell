package com.github.codewithtamim.dpt.gradle;

import org.gradle.api.GradleException;

import java.io.File;

final class JavaExecutable {

    private JavaExecutable() {}

    static File resolve() {
        String javaHome = System.getProperty("java.home");
        File unix = new File(javaHome, "bin/java");
        if (unix.isFile()) {
            return unix;
        }
        File win = new File(javaHome, "bin/java.exe");
        if (win.isFile()) {
            return win;
        }
        throw new GradleException("Could not find java executable under java.home=" + javaHome);
    }
}
