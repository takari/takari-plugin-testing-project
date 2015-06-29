package io.takari.maven.testing;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.execution.MojoExecutionEvent;
import org.apache.maven.execution.MojoExecutionListener;
import org.apache.maven.execution.scope.MojoExecutionScoped;
import org.apache.maven.execution.scope.internal.MojoExecutionScope;
import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;

import com.google.inject.AbstractModule;
import com.google.inject.Module;

class Maven321Runtime extends Maven311Runtime {

  private static class MojoExecutionScopeModule extends AbstractModule {
    @Override
    protected void configure() {
      MojoExecutionScope scope = new MojoExecutionScope();
      bind(MojoExecutionScope.class).toInstance(scope);

      bindScope(MojoExecutionScoped.class, scope);

      // standard scope bindings
      bind(MavenProject.class).toProvider(MojoExecutionScope.<MavenProject>seededKeyProvider()).in(scope);
      bind(MojoExecution.class).toProvider(MojoExecutionScope.<MojoExecution>seededKeyProvider()).in(scope);
    }
  }

  public static Maven321Runtime create(Module[] modules) throws Exception {
    Module[] joined = new Module[modules.length + 1];
    joined[0] = new MojoExecutionScopeModule();
    System.arraycopy(modules, 0, joined, 1, modules.length);
    return new Maven321Runtime(joined);
  }

  protected Maven321Runtime(Module[] modules) throws Exception {
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
