package basic;

import io.takari.maven.testing.TestProperties;
import io.takari.maven.testing.TestResources;
import io.takari.maven.testing.executor.MavenRuntime;

import java.io.File;

import org.junit.Rule;
import org.junit.Test;

public class IntegrationTest {

  @Rule
  public final TestResources resources = new TestResources();

  public final TestProperties properties = new TestProperties();

  @Test
  public void testBasic() throws Exception {
    File basedir = resources.getBasedir("basic");
    String mavenVersion = properties.get("mavenVersion");
    File mavenHome = new File("target/apache-maven-" + mavenVersion).getCanonicalFile();
    MavenRuntime maven = MavenRuntime.builder(mavenHome, null).build();
    maven.forProject(basedir).execute("validate").assertErrorFreeLog();
    TestResources.assertFilesPresent(basedir, "target/output.txt");
  }

}
