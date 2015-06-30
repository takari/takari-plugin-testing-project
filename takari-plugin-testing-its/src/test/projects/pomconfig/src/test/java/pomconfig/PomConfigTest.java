package pomconfig;

import io.takari.maven.testing.TestMavenRuntime;
import io.takari.maven.testing.TestResources;

import java.io.File;

import org.junit.Rule;
import org.junit.Test;

public class PomConfigTest {

  @Rule
  public final TestResources resources = new TestResources();

  @Rule
  public final TestMavenRuntime maven = new TestMavenRuntime();

  @Test
  public void testPomConfig() throws Exception {
    File basedir = resources.getBasedir("basic");
    maven.executeMojo(basedir, "pomconfig");
    TestResources.assertFileContents(basedir, "output.txt-expected", "target/output.txt");
  }
}
