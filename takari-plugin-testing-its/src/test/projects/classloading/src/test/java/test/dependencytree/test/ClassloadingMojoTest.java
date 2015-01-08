package test.dependencytree.test;

import io.takari.maven.testing.TestResources;
import io.takari.maven.testing.executor.MavenExecutionResult;
import io.takari.maven.testing.executor.MavenRuntime;
import io.takari.maven.testing.executor.MavenRuntime.MavenRuntimeBuilder;
import io.takari.maven.testing.executor.MavenVersions;
import io.takari.maven.testing.executor.junit.MavenJUnitTestRunner;

import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.VersionRange;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(MavenJUnitTestRunner.class)
@MavenVersions({"3.0.5", "3.2.5", "3.2.6-SNAPSHOT"})
public class ClassloadingMojoTest {

  @Rule
  public final TestResources resources = new TestResources();

  private final MavenRuntime maven;

  public ClassloadingMojoTest(MavenRuntimeBuilder builder) throws Exception {
    this.maven = builder.withCliOptions("-B", "-U").build();
    // a workaround
    // this.maven = builder.forkedBuilder().withCliOptions("-B", "-U").build();
  }

  @Test
  public void testClassloading() throws Exception {
    MavenExecutionResult result =
        maven.forProject(resources.getBasedir("basic")).execute("validate");

    DefaultArtifactVersion mavenVersion = new DefaultArtifactVersion(maven.getMavenVersion());

    VersionRange MAVEN30 = VersionRange.createFromVersionSpec("[3.0,3.1)");
    VersionRange MAVEN31 = VersionRange.createFromVersionSpec("[3.1,)");

    result.assertErrorFreeLog();

    if (MAVEN30.containsVersion(mavenVersion)) {
      result.assertLogText("classloader: org.sonatype.aether");
      result.assertNoLogText("classloader: org.eclipse.aether");
    } else if (MAVEN31.containsVersion(mavenVersion)) {
      result.assertLogText("classloader: org.eclipse.aether");
      result.assertNoLogText("classloader: org.sonatype.aether");
    } else {
      throw new AssertionError("Unsupported maven version: " + mavenVersion);
    }
  }
}
