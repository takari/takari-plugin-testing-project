package guicescopes;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;

@Mojo(name = "guicescopes")
public class GuiceScopesMojo extends AbstractMojo {

  @Component
  private MojoScopedComponent component;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    if (component == null) {
      throw new NullPointerException();
    }
  }

}
