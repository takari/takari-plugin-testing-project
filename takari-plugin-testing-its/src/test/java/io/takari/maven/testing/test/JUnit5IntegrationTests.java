package io.takari.maven.testing.test;

import static java.nio.charset.StandardCharsets.UTF_8;

import io.takari.maven.testing.TestResources5;
import io.takari.maven.testing.executor.MavenInstallations;
import io.takari.maven.testing.executor.MavenRuntime;
import io.takari.maven.testing.executor.MavenRuntime.MavenRuntimeBuilder;
import io.takari.maven.testing.executor.junit.MavenPluginTest;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.extension.RegisterExtension;

@MavenInstallations({
    "target/maven-installation/apache-maven-3.6.3",
    "target/maven-installation/apache-maven-3.8.8",
    "target/maven-installation/apache-maven-3.9.9",
    "target/maven-installation/apache-maven-4.0.0-rc-3"
})
public class JUnit5IntegrationTests {

    @RegisterExtension
    final TestResources5 resources = new TestResources5();

    private final MavenRuntime maven;

    private final String version;

    JUnit5IntegrationTests(MavenRuntimeBuilder builder) throws Exception {
        this.maven = builder.withCliOptions("-B", "-e").build();
        this.version = this.maven.getMavenVersion();
    }

    @MavenPluginTest
    void testBasic() throws Exception {
        File basedir = this.resources.getBasedir("basic");
        this.write(
                new File(basedir, "src/test/java/basic/TargetVersion.java"),
                "package basic; class TargetVersion { static final String VERSION = \"" + this.version + "\"; }");

        this.maven
                .forProject(basedir)
                .withCliOption("-e") //
                .execute("package") //
                .assertErrorFreeLog();
    }

    private void write(File file, String string) throws IOException {
        Files.write(file.toPath(), string.getBytes(UTF_8));
    }

    @MavenPluginTest
    void testGuiceScopes() throws Exception {
        // scopes were introduced in 3.2.1 https://issues.apache.org/jira/browse/MNG-5530
        Assumptions.assumeFalse(this.version.startsWith("3.0") || this.version.startsWith("3.1"));

        File basedir = this.resources.getBasedir("guicescopes");
        this.maven
                .forProject(basedir) //
                .execute("package") //
                .assertErrorFreeLog();
    }

    @MavenPluginTest
    void testPomConfig() throws Exception {
        File basedir = this.resources.getBasedir("pomconfig");
        this.maven
                .forProject(basedir) //
                .execute("package") //
                .assertErrorFreeLog();
    }

    @MavenPluginTest
    void testUnitTestHarnessHonoursUserSettings() throws Exception {
        File basedir = this.resources.getBasedir("settings");
        this.maven
                .forProject(basedir) //
                .execute("test") //
                .assertErrorFreeLog();
    }
}
