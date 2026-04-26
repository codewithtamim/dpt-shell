package com.github.codewithtamim.dpt.gradle;

import com.android.build.gradle.AppExtension;
import com.android.build.gradle.api.ApplicationVariant;
import com.android.build.gradle.api.BaseVariantOutput;
import org.gradle.api.NonNullApi;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.tasks.TaskProvider;

@NonNullApi
public class DptPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        project.getExtensions().create("dpt", DptExtension.class);
        project.afterEvaluate(DptPlugin::configureAfterEvaluate);
    }

    private static void configureAfterEvaluate(Project project) {
        if (!project.getPlugins().hasPlugin("com.android.application")) {
            project.getLogger().warn("dpt: com.android.application not applied; skipping DPT integration.");
            return;
        }
        DptExtension ext = project.getExtensions().getByType(DptExtension.class);
        boolean enabled = PropertyMerge.mergeBoolean(project, "dpt.enabled", ext.getEnabled().get());
        if (!enabled) {
            project.getLogger().lifecycle("dpt: protection disabled (set dpt { enabled = true } or -Pdpt.enabled=true).");
            return;
        }
        AppExtension android = project.getExtensions().getByType(AppExtension.class);
        android.getApplicationVariants().all(variant -> configureVariant(project, ext, variant));
        project.getTasks().register("dptVersion", DptVersionTask.class, t -> {
            t.setGroup("help");
            t.setDescription("Prints dpt.jar version bundled with the Gradle plugin.");
        });
    }

    private static void configureVariant(Project project, DptExtension ext, ApplicationVariant variant) {
        String buildType = variant.getBuildType().getName();
        boolean isDebug = "debug".equals(buildType);
        if (isDebug) {
            if (!PropertyMerge.mergeBoolean(project, "dpt.applyToDebug", ext.getApplyToDebug().get())) {
                return;
            }
        } else {
            if (!PropertyMerge.mergeBoolean(project, "dpt.applyToRelease", ext.getApplyToRelease().get())) {
                return;
            }
        }
        String taskName = "dptProtect" + capitalize(variant.getName());
        TaskProvider<DptProtectTask> provider = project.getTasks().register(taskName, DptProtectTask.class, task -> {
            task.setGroup("build");
            task.setDescription("Runs dpt protection on APK(s) for variant " + variant.getName() + ".");
            task.setVariantName(variant.getName());
            task.dependsOn(variant.getPackageApplicationProvider());
            for (BaseVariantOutput output : variant.getOutputs()) {
                task.getApks().from(output.getOutputFile());
            }
        });
        variant.getPackageApplicationProvider().configure(pkg -> pkg.finalizedBy(provider));
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
