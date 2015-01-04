package basic;

import io.takari.maven.testing.TestMavenRuntime;
import io.takari.maven.testing.TestResources;

import java.io.File;

import org.junit.Rule;
import org.junit.Test;

public class UnitTest {

  @Rule
  public final TestResources resources = new TestResources();

  @Rule
  public final TestMavenRuntime maven = new TestMavenRuntime();

  @Test
  public void testBasic() throws Exception {
    File basedir = resources.getBasedir("basic");
    maven.executeMojo(basedir, "basic");
    TestResources.assertFilesPresent(basedir, "target/output.txt");
  }

}
