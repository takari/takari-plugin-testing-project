/*
 * Copyright (c) 2014-2024 Takari, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 */
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

    MavenSession newMavenSession(File baseir) throws Exception;

    MojoExecution newMojoExecution(String goal);

    Mojo executeMojo(MavenSession session, MavenProject project, MojoExecution execution) throws Exception;

    Mojo lookupConfiguredMojo(MavenSession session, MojoExecution execution) throws Exception;

    DefaultPlexusContainer getContainer();

    <T> T lookup(Class<T> role) throws ComponentLookupException;
}
