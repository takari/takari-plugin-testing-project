package io.takari.maven.testing;

import java.io.File;

import org.apache.maven.DefaultMaven;
import org.apache.maven.Maven;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.DefaultMavenExecutionResult;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequestPopulationException;
import org.apache.maven.execution.MavenExecutionRequestPopulator;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingRequest;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystemSession;

import com.google.inject.Module;

class Maven311Runtime extends Maven30xRuntime {

  public Maven311Runtime(Module[] modules) throws Exception {
    super(modules);
  }

  @Override
  public MavenProject readMavenProject(File basedir) throws Exception {
    File pom = new File(basedir, "pom.xml");
    MavenExecutionRequest request = new DefaultMavenExecutionRequest();
    request.setBaseDirectory(basedir);
    ProjectBuildingRequest configuration = request.getProjectBuildingRequest();
    configuration.setRepositorySession(new DefaultRepositorySystemSession());
    return container.lookup(ProjectBuilder.class).build(getPomFile(pom), configuration).getProject();
  }

  @SuppressWarnings("deprecation")
  @Override
  public MavenSession newMavenSession() throws MavenExecutionRequestPopulationException, ComponentLookupException {
    MavenExecutionRequest request = new DefaultMavenExecutionRequest();
    MavenExecutionResult result = new DefaultMavenExecutionResult();

    request.setLocalRepositoryPath(properties.getLocalRepository());
    request.setUserSettingsFile(properties.getUserSettings());
    request.setOffline(properties.getOffline());
    request.setUpdateSnapshots(properties.getUpdateSnapshots());

    request = container.lookup(MavenExecutionRequestPopulator.class).populateDefaults(request);
    RepositorySystemSession repositorySession = ((DefaultMaven) container.lookup(Maven.class)).newRepositorySession(request);

    return new MavenSession(container, repositorySession, request, result);
  }

}
