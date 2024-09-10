package io.takari.maven.testing.test;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import io.takari.maven.testing.TestResources5;
import io.takari.maven.testing.executor.MavenRuntime;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * <ul>
 * <li>The test project is built with maven 3.6.3.</li>
 * <li>The test project is built against specific versions of maven, as specified in individual test
 * methods</li>
 * <li>The test project is not able to resolve test harness from the reactor, hence the outer build
 * must run at least install phase.</li>
 * </ul>
 */
class JUnit5UnitTests {

    @RegisterExtension
    final TestResources5 resources = new TestResources5();

    @ParameterizedTest
    @ArgumentsSource(MavenVersionsSource.class)
    void testBasic(MavenRuntime maven, String version) throws Exception {
        File basedir = this.resources.getBasedir("basic");
        this.write(
                new File(basedir, "src/test/java/basic/TargetVersion.java"),
                "package basic; class TargetVersion { static final String VERSION = \"" + version + "\"; }");
        maven.forProject(basedir) //
                .withCliOptions("-B", "-e", "-DmavenVersion=" + version) //
                .execute("package") //
                .assertErrorFreeLog();
    }

    private void write(File file, String string) throws IOException {
        Files.write(file.toPath(), string.getBytes(UTF_8));
    }

    @ParameterizedTest
    @ArgumentsSource(MavenVersionsSource.class)
    void testGuiceScopes(MavenRuntime maven, String version) throws Exception {
        // scopes were introduced in 3.2.1 https://issues.apache.org/jira/browse/MNG-5530
        assumeFalse(version.startsWith("3.0") || version.startsWith("3.1"));

        File basedir = this.resources.getBasedir("guicescopes");
        maven.forProject(basedir) //
                .withCliOptions("-B", "-e", "-DmavenVersion=" + version) //
                .execute("package") //
                .assertErrorFreeLog();
    }

    @ParameterizedTest
    @ArgumentsSource(MavenVersionsSource.class)
    void testPomConfig(MavenRuntime maven, String version) throws Exception {
        File basedir = this.resources.getBasedir("pomconfig");
        maven.forProject(basedir) //
                .withCliOptions("-B", "-e", "-DmavenVersion=" + version) //
                .execute("package") //
                .assertErrorFreeLog();
    }

    @ParameterizedTest
    @ArgumentsSource(MavenVersionsSource.class)
    void testUnitTestHarnessHonoursUserSettings(MavenRuntime maven, String version) throws Exception {
        File basedir = this.resources.getBasedir("settings");
        maven.forProject(basedir) //
                .withCliOptions("-B", "-e", "-DmavenVersion=" + version) //
                .execute("test") //
                .assertErrorFreeLog();
    }

    static final class MavenVersionsSource implements ArgumentsProvider {

        private List<String> getMavenVersions() {
            return Arrays.asList("3.6.3", "3.8.8", "3.9.9");
        }

        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) throws Exception {
            List<String> mavenVersions = this.getMavenVersions();
            List<Arguments> arguments = new ArrayList<>(mavenVersions.size());
            for (String version : mavenVersions) {
                File mavenHome = new File("target/maven-installation/apache-maven-" + version);
                MavenRuntime maven =
                        MavenRuntime.builder(mavenHome, null).forkedBuilder().build();
                arguments.add(Arguments.of(maven, version));
            }
            return arguments.stream();
        }
    }
}
