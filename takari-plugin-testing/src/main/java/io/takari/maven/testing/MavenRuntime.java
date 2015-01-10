package io.takari.maven.testing;

import java.io.File;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;

interface MavenRuntime {

  void shutdown();

  MavenProject readMavenProject(File basedir) throws Exception;

  MavenSession newMavenSession() throws Exception;

  MojoExecution newMojoExecution(String goal);

  void executeMojo(MavenSession session, MavenProject project, MojoExecution execution) throws Exception;

  Mojo lookupConfiguredMojo(MavenSession session, MojoExecution execution) throws Exception;

  DefaultPlexusContainer getContainer();

  <T> T lookup(Class<T> role) throws ComponentLookupException;

}
