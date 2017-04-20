package io.takari.maven.testing.test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import io.takari.maven.testing.TestResources;
import io.takari.maven.testing.executor.MavenRuntime;

/**
 * <ul>
 * <li>The test project is built with maven 3.2.5.</li>
 * <li>The test project is built against specific versions of maven, as specified in individual test
 * methods</li>
 * <li>The test project is not able to resolve test harness from the reactor, hence the outer build
 * must run at least install phase.</li>
 * </ul>
 */
@RunWith(Parameterized.class)
public class IntegrationTest {

  @Parameters(name = "{0}")
  public static List<Object[]> versions() {
    List<Object[]> parameters = new ArrayList<>();
    parameters.add(new Object[] {"3.0.5"});
    // parameters.add(new Object[] {"3.1.0"}); not supported unless someone asks real nice
    parameters.add(new Object[] {"3.1.1"});
    // parameters.add(new Object[] {"3.2.1"}); see https://issues.apache.org/jira/browse/MNG-5591
    parameters.add(new Object[] {"3.2.2"});
    parameters.add(new Object[] {"3.2.3"});
    // parameters.add(new Object[] {"3.2.4"}); was never released
    parameters.add(new Object[] {"3.2.5"});
    parameters.add(new Object[] {"3.3.1"});
    // parameters.add(new Object[] {"3.3.2"}); was never released
    parameters.add(new Object[] {"3.3.3"});
    parameters.add(new Object[] {"3.3.9"});
    parameters.add(new Object[] {"3.5.0"});
    return parameters;
  }

  @Rule
  public final TestResources resources = new TestResources();

  public final MavenRuntime maven;

  private final String version;

  public IntegrationTest(String version) throws Exception {
    this.version = version;
    File mavenHome = new File("target/apache-maven-3.2.5");
    this.maven = MavenRuntime.builder(mavenHome, null).forkedBuilder().build();
  }

  @Test
  public void testBasic() throws Exception {
    File basedir = resources.getBasedir("basic");
    write(new File(basedir, "src/test/java/basic/TargetVersion.java"),
        "package basic; class TargetVersion { static final String VERSION = \"" + version + "\"; }");
    maven.forProject(basedir) //
        .withCliOptions("-B", "-e", "-DmavenVersion=" + version) //
        .execute("package") //
        .assertErrorFreeLog();
  }

  private void write(File file, String string) throws IOException {
    try (Writer w = new OutputStreamWriter(new FileOutputStream(file), "UTF-8")) {
      w.write(string);
    }
  }

  @Test
  public void testGuiceScopes() throws Exception {
    // scopes were introduced in 3.2.1 https://issues.apache.org/jira/browse/MNG-5530
    Assume.assumeFalse(version.startsWith("3.0") || version.startsWith("3.1"));

    File basedir = resources.getBasedir("guicescopes");
    maven.forProject(basedir) //
        .withCliOptions("-B", "-e", "-DmavenVersion=" + version) //
        .execute("package") //
        .assertErrorFreeLog();
  }

  @Test
  public void testPomConfig() throws Exception {
    File basedir = resources.getBasedir("pomconfig");
    maven.forProject(basedir) //
        .withCliOptions("-B", "-e", "-DmavenVersion=" + version) //
        .execute("package") //
        .assertErrorFreeLog();
  }

  @Test
  public void testUnitTestHarnessHonoursUserSettings() throws Exception {
    File basedir = resources.getBasedir("settings");
    maven.forProject(basedir) //
        .withCliOptions("-B", "-e", "-DmavenVersion=" + version) //
        .execute("test") //
        .assertErrorFreeLog();
  }
}
