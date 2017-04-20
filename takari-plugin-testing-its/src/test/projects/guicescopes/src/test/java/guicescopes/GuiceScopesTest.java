package guicescopes;

import io.takari.maven.testing.TestMavenRuntime;
import io.takari.maven.testing.TestResources;

import java.io.File;

import org.junit.Rule;
import org.junit.Test;

public class GuiceScopesTest {

  @Rule
  public final TestResources resources = new TestResources();

  @Rule
  public final TestMavenRuntime maven = new TestMavenRuntime();

  @Test
  public void test() throws Exception {
    File basedir = resources.getBasedir();
    // blows up if @MojoExecutionScoped component can't be injected
    maven.executeMojo(basedir, "guicescopes");
  }

  @Test
  public void test_jsr330() throws Exception {
    File basedir = resources.getBasedir();
    // blows up if @MojoExecutionScoped component can't be injected
    maven.executeMojo(basedir, "guicescopes-jsr330");
  }
}
