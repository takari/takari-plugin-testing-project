package io.takari.maven.testing;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.execution.MojoExecutionEvent;
import org.apache.maven.execution.MojoExecutionListener;
import org.apache.maven.execution.scope.internal.MojoExecutionScope;
import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;

import com.google.inject.Module;

class Maven321Runtime extends Maven311Runtime {

  public Maven321Runtime(Module[] modules) throws Exception {
    super(modules);
  }

  @Override
  public void executeMojo(MavenSession session, MavenProject project, MojoExecution execution) throws Exception {
    Object sessionScope = container.lookup("org.apache.maven.SessionScope");
    try {
      enter(sessionScope);
      seed(sessionScope, MavenSession.class, session);

      MojoExecutionScope executionScope = container.lookup(MojoExecutionScope.class);
      try {
        executionScope.enter();

        executionScope.seed(MavenProject.class, project);
        executionScope.seed(MojoExecution.class, execution);

        Mojo mojo = lookupConfiguredMojo(session, execution);
        mojo.execute();

        MojoExecutionEvent event = new MojoExecutionEvent(session, project, execution, mojo);
        for (MojoExecutionListener listener : container.lookupList(MojoExecutionListener.class)) {
          listener.afterMojoExecutionSuccess(event);
        }
      } finally {
        executionScope.exit();
      }
    } finally {
      exit(sessionScope);
    }
  }

  private static void enter(Object scope) throws Exception {
    scope.getClass().getMethod("enter").invoke(scope);
  }

  private static void seed(Object scope, Class type, Object instance) throws Exception {
    scope.getClass().getMethod("seed", Class.class, Object.class).invoke(scope, type, instance);
  }

  private static void exit(Object scope) throws Exception {
    scope.getClass().getMethod("exit").invoke(scope);
  }
}
