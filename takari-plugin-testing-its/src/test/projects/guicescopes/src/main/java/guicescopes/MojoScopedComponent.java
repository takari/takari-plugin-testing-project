package guicescopes;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.maven.execution.scope.MojoExecutionScoped;
import org.apache.maven.plugin.MojoExecution;

@Named
@MojoExecutionScoped
public class MojoScopedComponent {

  @Inject
  public MojoScopedComponent(MojoExecution execution) {}

}
