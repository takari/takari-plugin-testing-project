package io.takari.maven.testing.test;

import io.takari.maven.testing.TestResources;
import io.takari.maven.testing.executor.MavenRuntime;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

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
public class BasicIntegrationTest {

  @Parameters(name = "{0}")
  public static List<Object[]> versions() {
    List<Object[]> parameters = new ArrayList<>();
    parameters.add(new Object[] {"3.0.5"});
    parameters.add(new Object[] {"3.1.1"});
    // parameters.add(new Object[] {"3.2.1"}); see https://jira.codehaus.org/browse/MNG-5591
    parameters.add(new Object[] {"3.2.2"});
    parameters.add(new Object[] {"3.2.5"});
    parameters.add(new Object[] {"3.2.6-SNAPSHOT"});
    return parameters;
  }

  @Rule
  public final TestResources resources = new TestResources();

  public final MavenRuntime maven;

  private final String version;

  public BasicIntegrationTest(String version) throws Exception {
    this.version = version;
    File mavenHome = new File("target/apache-maven-3.2.5");
    this.maven = MavenRuntime.builder(mavenHome, null).forkedBuilder().build();
  }

  @Test
  public void test() throws Exception {
    maven.forProject(resources.getBasedir("basic")) //
        .withCliOptions("-B", "-e", "-DmavenVersion=" + version) //
        .execute("package") //
        .assertErrorFreeLog();
  }

}
