package com.github.codewithtamim.dpt.gradle;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

public class DptVersionTask extends DefaultTask {

    @TaskAction
    public void printVersion() {
        Path extractDir = getProject().getLayout().getBuildDirectory().dir("dpt-shell/runtime").get().getAsFile().toPath();
        try {
            DptRuntimeExtractor.extractTo(extractDir);
        } catch (IOException e) {
            throw new GradleException("Failed to extract dpt runtime", e);
        }
        String root = extractDir.toAbsolutePath().toString();
        final File javaExecutable = JavaExecutable.resolve();
        getProject().exec(spec -> {
            spec.setExecutable(javaExecutable.getAbsolutePath());
            spec.setWorkingDir(new File(root));
            spec.args("-D" + DptCliNames.EXECUTABLE_ROOT_PROPERTY + "=" + root, "-jar", "dpt.jar", "--version");
        });
    }
}
