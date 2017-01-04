package io.takari.maven.testing;

import java.io.File;

import org.apache.maven.execution.DefaultMavenExecutionResult;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.MojoExecutionConfigurator;
import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.PluginParameterExpressionEvaluator;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.configurator.ComponentConfigurator;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluator;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.codehaus.plexus.configuration.xml.XmlPlexusConfiguration;
import org.eclipse.aether.RepositorySystemSession;

import com.google.inject.Module;

class Maven331Runtime extends Maven325Runtime {

  public Maven331Runtime(Module[] modules) throws Exception {
    super(modules);
  }

  @Override
  public Mojo lookupConfiguredMojo(MavenSession session, MojoExecution execution) throws Exception {
    MavenProject project = session.getCurrentProject();
    MojoDescriptor mojoDescriptor = execution.getMojoDescriptor();

    Mojo mojo = container.lookup(Mojo.class, mojoDescriptor.getRoleHint());

    ExpressionEvaluator evaluator = new PluginParameterExpressionEvaluator(session, execution);
    mojoExecutionConfigurator(execution).configure(project, execution, true);
    finalizeMojoConfiguration(execution);
    PlexusConfiguration mojoConfiguration = new XmlPlexusConfiguration(execution.getConfiguration());

    String configuratorHint = "basic";
    if (mojoDescriptor.getComponentConfigurator() != null) {
      configuratorHint = mojoDescriptor.getComponentConfigurator();
    }

    ComponentConfigurator configurator = container.lookup(ComponentConfigurator.class, configuratorHint);

    configurator.configureComponent(mojo, mojoConfiguration, evaluator, container.getContainerRealm());

    return mojo;
  }

  private MojoExecutionConfigurator mojoExecutionConfigurator(MojoExecution mojoExecution) throws Exception {
    String configuratorId = mojoExecution.getMojoDescriptor().getComponentConfigurator();
    if (configuratorId == null) {
      configuratorId = "default";
    }
    return container.lookup(MojoExecutionConfigurator.class, configuratorId);
  }

  @SuppressWarnings("deprecation")
  @Override
  public MavenSession newMavenSession(File basedir) throws Exception {
    MavenExecutionRequest request = newExecutionRequest();
    request.setMultiModuleProjectDirectory(basedir);
    RepositorySystemSession repositorySession = newRepositorySession(request);

    MavenExecutionResult result = new DefaultMavenExecutionResult();
    return new MavenSession(container, repositorySession, request, result);
  }
}
