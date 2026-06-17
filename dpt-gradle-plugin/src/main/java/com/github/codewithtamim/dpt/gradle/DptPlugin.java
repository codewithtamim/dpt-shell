package com.github.codewithtamim.dpt.gradle;

import com.android.build.api.artifact.SingleArtifact;
import com.android.build.api.variant.ApplicationAndroidComponentsExtension;
import org.gradle.api.NonNullApi;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.file.Directory;
import org.gradle.api.file.RegularFile;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.TaskProvider;

@NonNullApi
public class DptPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        project.getExtensions().create("dpt", DptExtension.class);
        registerVariants(project);
        project.afterEvaluate(p -> {
            p.getTasks().register("dptVersion", DptVersionTask.class, t -> {
                t.setGroup("help");
                t.setDescription("Prints dpt.jar version bundled with the Gradle plugin.");
            });
        });
    }

    private static void registerVariants(Project project) {
        Runnable register = () -> {
            ApplicationAndroidComponentsExtension androidComponents =
                    project.getExtensions().findByType(ApplicationAndroidComponentsExtension.class);
            if (androidComponents == null) {
                return;
            }
            androidComponents.onVariants(
                    androidComponents.selector().all(),
                    v -> {
                        DptExtension ext = project.getExtensions().getByType(DptExtension.class);
                        configureVariantForApk(project, ext, v);
                        configureVariantForAab(project, ext, v);
                    });
        };
        if (project.getPlugins().hasPlugin("com.android.application")) {
            register.run();
        } else {
            project.getPlugins().withId("com.android.application", p -> register.run());
        }
    }

    private static void configureVariantForApk(
            Project project, DptExtension ext, com.android.build.api.variant.ApplicationVariant variant) {
        if (!PropertyMerge.mergeBoolean(project, "dpt.enabled", ext.getEnabled().get())) {
            return;
        }
        String buildType = variant.getBuildType();
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
        String vn = variant.getName();
        String taskName = "dptProtect" + capitalize(vn);
        String assembleTaskName = "assemble" + capitalize(vn);
        if (project.getTasks().findByName(assembleTaskName) == null) {
            return;
        }
        TaskProvider<DptProtectTask> provider = project.getTasks().register(taskName, DptProtectTask.class, task -> {
            task.setGroup("build");
            task.setDescription("Runs dpt protection on APK(s) for variant " + vn + ".");
            task.setVariantName(vn);
            Provider<Directory> apkDir = variant.getArtifacts().get(SingleArtifact.APK.INSTANCE);
            task.getApks().from(apkDir.map(d -> d.getAsFileTree()));
        });
        TaskProvider<Task> assembleTask = project.getTasks().named(assembleTaskName);
        assembleTask.configure(assemble -> assemble.finalizedBy(provider));
    }

    private static void configureVariantForAab(
            Project project, DptExtension ext, com.android.build.api.variant.ApplicationVariant variant) {
        if (!PropertyMerge.mergeBoolean(project, "dpt.enabled", ext.getEnabled().get())) {
            return;
        }
        if (!PropertyMerge.mergeBoolean(project, "dpt.applyToBundle", ext.getApplyToBundle().get())) {
            return;
        }
        String buildType = variant.getBuildType();
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
        String vn = variant.getName();
        String bundleTaskName = "bundle" + capitalize(vn);
        if (project.getTasks().findByName(bundleTaskName) == null) {
            return;
        }
        String taskName = "dptProtectBundle" + capitalize(vn);
        TaskProvider<DptProtectTask> provider =
                project.getTasks().register(taskName, DptProtectTask.class, task -> {
                    task.setGroup("build");
                    task.setDescription("Runs dpt protection on App Bundle (.aab) for variant " + vn + ".");
                    task.setVariantName(vn);
                    Provider<RegularFile> bundleFile = variant.getArtifacts().get(SingleArtifact.BUNDLE.INSTANCE);
                    task.getApks().from(bundleFile);
                });
        TaskProvider<Task> bundleTask = project.getTasks().named(bundleTaskName);
        bundleTask.configure(bundle -> bundle.finalizedBy(provider));
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
