package basic;

import io.takari.maven.testing.TestResources;
import io.takari.maven.testing.executor.MavenRuntime;
import io.takari.maven.testing.executor.MavenRuntime.MavenRuntimeBuilder;
import io.takari.maven.testing.executor.MavenVersions;
import io.takari.maven.testing.executor.junit.MavenJUnitTestRunner;

import java.io.File;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(MavenJUnitTestRunner.class)
@MavenVersions({"3.2.5", "3.2.6-SNAPSHOT"})
public class IntegrationTest {

  @Rule
  public final TestResources resources = new TestResources();

  public final MavenRuntime maven;

  public IntegrationTest(MavenRuntimeBuilder builder) throws Exception {
    maven = builder.build();
  }

  @Test
  public void testBasic() throws Exception {
    File basedir = resources.getBasedir("basic");
    maven.forProject(basedir).execute("validate").assertErrorFreeLog();
    TestResources.assertFilesPresent(basedir, "target/output.txt");
  }

}
