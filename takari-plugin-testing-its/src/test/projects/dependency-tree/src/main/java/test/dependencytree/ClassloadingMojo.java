package test.dependencytree;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;

@Mojo(name = "classloading")
public class ClassloadingMojo extends AbstractMojo {

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    System.out.println("maven: " + (isMaven31() ? "maven31+" : isMaven2x() ? "maven2x" : "maven30x"));
  }

  protected static boolean isMaven2x() {
    return !canFindCoreClass("org.apache.maven.project.DependencyResolutionRequest");
  }

  protected static boolean isMaven31() {
    return canFindCoreClass("org.eclipse.aether.artifact.Artifact");
  }

  private static boolean canFindCoreClass(String className) {
    try {
      Thread.currentThread().getContextClassLoader().loadClass(className);

      return true;
    } catch (ClassNotFoundException e) {
      return false;
    }
  }

}
