package com.github.codewithtamim.dpt.gradle;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

public class DptProtectTask extends DefaultTask {

    private final ConfigurableFileCollection apks;
    private String variantName;

    @Inject
    public DptProtectTask() {
        this.apks = getProject().getObjects().fileCollection();
    }

    @InputFiles
    public ConfigurableFileCollection getApks() {
        return apks;
    }

    @Input
    public String getVariantName() {
        return variantName;
    }

    public void setVariantName(String variantName) {
        this.variantName = variantName;
    }

    @TaskAction
    public void protect() {
        DptExtension ext = getProject().getExtensions().getByType(DptExtension.class);
        String protectConfigPath = DptProtectConfigWriter.resolve(getProject(), ext, getVariantName());
        Path extractDir = getProject().getLayout().getBuildDirectory().dir("dpt-shell/runtime").get().getAsFile().toPath();
        try {
            DptRuntimeExtractor.extractTo(extractDir);
        } catch (IOException e) {
            throw new GradleException("Failed to extract dpt runtime", e);
        }
        String root = extractDir.toAbsolutePath().toString();
        final File javaExecutable = JavaExecutable.resolve();
        for (File apk : getApks().getFiles()) {
            String n = apk.getName();
            if (!n.endsWith(".apk") && !n.endsWith(".aab")) {
                continue;
            }
            if (!apk.isFile()) {
                getLogger().warn("dpt: skip missing package path {}", apk);
                continue;
            }
            List<String> appArgs =
                    DptArguments.build(getProject(), ext, apk, getVariantName(), protectConfigPath);
            getProject().exec(spec -> {
                spec.setExecutable(javaExecutable.getAbsolutePath());
                spec.setWorkingDir(new File(root));
                spec.args("-D" + DptCliNames.EXECUTABLE_ROOT_PROPERTY + "=" + root);
                spec.args("-jar", "dpt.jar");
                spec.args(appArgs);
            });
            getLogger().lifecycle("dpt: finished protection for {}", n);
            maybeCollectProtectedArtifact(n);
        }
    }

    private void maybeCollectProtectedArtifact(String inputFileName) {
        if (!PropertyMerge.collectProtectedToRoot(getProject(), getProject().getExtensions().getByType(DptExtension.class))) {
            return;
        }
        DptExtension ext = getProject().getExtensions().getByType(DptExtension.class);
        File outDir = PropertyMerge.resolveOutputDirectory(getProject(), ext, getVariantName());
        File protectedFile = new File(outDir, DptOutputNames.signedArtifactName(inputFileName));
        if (!protectedFile.isFile()) {
            getLogger().warn("dpt: protected output not found for collect (expected {})", protectedFile);
            return;
        }
        File destDir = new File(PropertyMerge.resolveCollectOutputDirectory(getProject(), ext), getVariantName());
        destDir.mkdirs();
        Path dest = new File(destDir, protectedFile.getName()).toPath();
        try {
            Files.copy(protectedFile.toPath(), dest, StandardCopyOption.REPLACE_EXISTING);
            getLogger().lifecycle("dpt: collected protected artifact to {}", dest);
        } catch (IOException e) {
            throw new GradleException("Failed to copy protected artifact to " + dest, e);
        }
    }
}
