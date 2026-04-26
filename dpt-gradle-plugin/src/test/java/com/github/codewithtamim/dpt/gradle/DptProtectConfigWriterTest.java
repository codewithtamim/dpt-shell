package com.github.codewithtamim.dpt.gradle;

import org.gradle.testfixtures.ProjectBuilder;
import org.gradle.api.Project;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class DptProtectConfigWriterTest {

    @Rule public TemporaryFolder tmp = new TemporaryFolder();

    @Test
    public void noInlineNoFileReturnsNull() throws Exception {
        Project project = ProjectBuilder.builder().withProjectDir(tmp.newFolder()).build();
        DptExtension ext = project.getExtensions().create("dpt", DptExtension.class);
        assertNull(DptProtectConfigWriter.resolve(project, ext, "release"));
    }

    @Test
    public void inlineSignatureGeneratesJsonWithRandomShell() throws Exception {
        Project project = ProjectBuilder.builder().withProjectDir(tmp.newFolder()).build();
        DptExtension ext = project.getExtensions().create("dpt", DptExtension.class);
        File ks = new File(project.getProjectDir(), "ks.jks");
        assertTrue(ks.createNewFile());
        ext.getProtect().getSignature().getKeystore().set(ks);
        ext.getProtect().getSignature().getAlias().set("a1");
        ext.getProtect().getSignature().getStorePassword().set("s");
        ext.getProtect().getSignature().getKeyPassword().set("k");

        String path = DptProtectConfigWriter.resolve(project, ext, "release");
        assertTrue(path.endsWith("dpt-protect-release.json"));
        String json = new String(Files.readAllBytes(new File(path).toPath()), StandardCharsets.UTF_8);
        assertTrue(json.contains("\"shellPkgName\":\"<random>\""));
        assertTrue(json.contains("\"keystore\":\""));
        assertTrue(json.contains("\"alias\":\"a1\""));
        assertTrue(json.contains("\"storepass\":\"s\""));
        assertTrue(json.contains("\"keypass\":\"k\""));
    }

    @Test
    public void explicitProtectConfigFileWinsOverInline() throws Exception {
        Project project = ProjectBuilder.builder().withProjectDir(tmp.newFolder()).build();
        DptExtension ext = project.getExtensions().create("dpt", DptExtension.class);
        Files.write(
                project.getLayout().getProjectDirectory().file("explicit.json").getAsFile().toPath(),
                "{}".getBytes(StandardCharsets.UTF_8));
        ext.getProtectConfig().set(project.getLayout().getProjectDirectory().file("explicit.json"));
        ext.getProtect()
                .getSignature()
                .getKeystore()
                .set(project.getLayout().getProjectDirectory().file("explicit.json"));

        String path = DptProtectConfigWriter.resolve(project, ext, "release");
        assertTrue(path.replace('\\', '/').endsWith("explicit.json"));
    }
}
