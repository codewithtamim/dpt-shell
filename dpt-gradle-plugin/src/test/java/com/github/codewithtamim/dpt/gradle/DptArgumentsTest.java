package com.github.codewithtamim.dpt.gradle;

import org.gradle.testfixtures.ProjectBuilder;
import org.gradle.api.Project;
import org.junit.Test;

import java.io.File;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DptArgumentsTest {

    @Test
    public void projectPropertyOverridesExtensionForVerifySign() {
        Project project = ProjectBuilder.builder().build();
        DptExtension ext = project.getExtensions().create("dpt", DptExtension.class);
        ext.getVerifySign().set(true);
        project.getExtensions().getExtraProperties().set("dpt.verifySign", "false");
        List<String> args = DptArguments.build(project, ext, new File("/tmp/fake.apk"), "release");
        assertFalse(args.contains("--verify-sign"));
    }

    @Test
    public void noSignAddsFlagWhenTrue() {
        Project project = ProjectBuilder.builder().build();
        DptExtension ext = project.getExtensions().create("dpt", DptExtension.class);
        ext.getNoSign().set(true);
        List<String> args = DptArguments.build(project, ext, new File("/x/app.apk"), "debug");
        assertTrue(args.contains("--no-sign"));
        assertTrue(args.contains("--package-file"));
        assertTrue(args.contains("/x/app.apk"));
    }

    @Test
    public void excludeAbiFromProperty() {
        Project project = ProjectBuilder.builder().build();
        DptExtension ext = project.getExtensions().create("dpt", DptExtension.class);
        project.getExtensions().getExtraProperties().set("dpt.excludeAbi", "x86");
        List<String> args = DptArguments.build(project, ext, new File("/a.apk"), "release");
        assertTrue(args.contains("--exclude-abi"));
        assertTrue(args.contains("x86"));
    }
}
