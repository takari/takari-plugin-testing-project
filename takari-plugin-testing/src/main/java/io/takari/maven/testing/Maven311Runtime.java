/*
 * Copyright (c) 2014-2024 Takari, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 */
package io.takari.maven.testing;

import com.google.inject.Module;
import java.io.File;
import org.apache.maven.DefaultMaven;
import org.apache.maven.Maven;
import org.apache.maven.execution.DefaultMavenExecutionResult;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingRequest;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.eclipse.aether.RepositorySystemSession;

class Maven311Runtime extends Maven30xRuntime {

    public Maven311Runtime(Module[] modules) throws Exception {
        super(modules);
    }

    @Override
    public MavenProject readMavenProject(File basedir) throws Exception {
        File pom = new File(basedir, "pom.xml");
        MavenExecutionRequest request = newExecutionRequest();
        request.setBaseDirectory(basedir);
        ProjectBuildingRequest configuration = getProjectBuildingRequest(request);
        return container
                .lookup(ProjectBuilder.class)
                .build(getPomFile(pom), configuration)
                .getProject();
    }

    @Override
    protected ProjectBuildingRequest getProjectBuildingRequest(MavenExecutionRequest request)
            throws ComponentLookupException {
        ProjectBuildingRequest configuration = request.getProjectBuildingRequest();
        configuration.setRepositorySession(newRepositorySession(request));
        return configuration;
    }

    @SuppressWarnings("deprecation")
    @Override
    public MavenSession newMavenSession(File basedir) throws Exception {
        MavenExecutionRequest request = newExecutionRequest();
        RepositorySystemSession repositorySession = newRepositorySession(request);

        MavenExecutionResult result = new DefaultMavenExecutionResult();
        return new MavenSession(container, repositorySession, request, result);
    }

    protected RepositorySystemSession newRepositorySession(MavenExecutionRequest request)
            throws ComponentLookupException {
        return ((DefaultMaven) container.lookup(Maven.class)).newRepositorySession(request);
    }
}
