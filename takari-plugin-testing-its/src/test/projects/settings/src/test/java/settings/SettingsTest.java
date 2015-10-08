package settings;

import java.io.File;
import java.util.List;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.project.MavenProject;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import io.takari.maven.testing.TestMavenRuntime;
import io.takari.maven.testing.TestResources;

public class SettingsTest {
  @Rule
  public final TestResources resources = new TestResources();

  @Rule
  public final TestMavenRuntime maven = new TestMavenRuntime();

  @Test
  public void testSettings() throws Exception {
    File basedir = resources.getBasedir();
    MavenProject project = maven.readMavenProject(basedir);

    List<ArtifactRepository> repositories = project.getRemoteArtifactRepositories();
    Assert.assertEquals(1, repositories.size());

    ArtifactRepository repository = repositories.get(0);
    Assert.assertEquals("central", repository.getId());
    Assert.assertEquals("https://test/test/", repository.getUrl());
  }
}
