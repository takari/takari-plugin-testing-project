package guicescopes;

import javax.inject.Inject;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;

@Mojo(name = "guicescopes-jsr330")
public class GuiceScopesJSR330Mojo extends AbstractMojo {

  @Inject
  private MojoScopedComponent component;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    if (component == null) {
      throw new NullPointerException();
    }
  }

}
