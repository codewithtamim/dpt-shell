package com.github.codewithtamim.dpt.gradle;

import com.android.build.api.artifact.SingleArtifact;
import com.android.build.api.variant.ApplicationAndroidComponentsExtension;
import com.android.build.gradle.AppExtension;
import com.android.build.gradle.api.ApplicationVariant;
import com.android.build.gradle.api.BaseVariantOutput;
import org.gradle.api.Action;
import org.gradle.api.NonNullApi;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.file.RegularFile;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.TaskProvider;

@NonNullApi
public class DptPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        project.getExtensions().create("dpt", DptExtension.class);
        registerAppBundleVariantsEarly(project);
        project.afterEvaluate(DptPlugin::configureAfterEvaluate);
    }

    private static void registerAppBundleVariantsEarly(Project project) {
        Runnable register =
                () -> {
                    ApplicationAndroidComponentsExtension androidComponents =
                            project.getExtensions().findByType(ApplicationAndroidComponentsExtension.class);
                    if (androidComponents == null) {
                        return;
                    }
                    androidComponents.onVariants(
                            androidComponents.selector().all(),
                            new Action<com.android.build.api.variant.ApplicationVariant>() {
                                @Override
                                public void execute(com.android.build.api.variant.ApplicationVariant v) {
                                    DptExtension ext = project.getExtensions().getByType(DptExtension.class);
                                    configureVariantAab(project, ext, v);
                                }
                            });
                };
        if (project.getPlugins().hasPlugin("com.android.application")) {
            register.run();
        } else {
            project.getPlugins().withId("com.android.application", p -> register.run());
        }
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
        ApplicationAndroidComponentsExtension androidComponents =
                project.getExtensions().findByType(ApplicationAndroidComponentsExtension.class);
        if (androidComponents == null) {
            project.getLogger()
                    .warn(
                            "dpt: ApplicationAndroidComponentsExtension not found; AAB protection is skipped. Use Android Gradle Plugin 8+.");
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
            task.dependsOn(variant.getAssembleProvider());
            for (BaseVariantOutput output : variant.getOutputs()) {
                task.getApks().from(output.getOutputFile());
            }
        });
        variant.getAssembleProvider().configure(assemble -> assemble.finalizedBy(provider));
    }

    /**
     * Separate from {@link #configureVariant} so {@code assemble*} does not force producing an {@code .aab}. The
     * {@code dptProtectBundle*} task is finalized by {@code bundle*} only.
     */
    private static void configureVariantAab(
            Project project, DptExtension ext, com.android.build.api.variant.ApplicationVariant variant) {
        if (!PropertyMerge.mergeBoolean(project, "dpt.enabled", ext.getEnabled().get())) {
            return;
        }
        if (!PropertyMerge.mergeBoolean(project, "dpt.applyToBundle", ext.getApplyToBundle().get())) {
            return;
        }
        boolean isDebug = "debug".equals(variant.getBuildType());
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
