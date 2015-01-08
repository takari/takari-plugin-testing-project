package test.dependencytree;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;

@Mojo(name = "classloading")
public class ClassloadingMojo extends AbstractMojo {

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    if (isEclipseAether()) {
      System.out.println("org.eclipse.aether");
    }
    if (isSonatypeAether()) {
      System.out.println("org.sonatype.aether");
    }
  }

  protected static boolean isSonatypeAether() {
    return canFindCoreClass("org.sonatype.aether.artifact.Artifact");
  }

  protected static boolean isEclipseAether() {
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
