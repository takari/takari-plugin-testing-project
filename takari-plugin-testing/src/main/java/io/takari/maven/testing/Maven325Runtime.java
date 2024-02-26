/*
 * Copyright (c) 2014-2024 Takari, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 */
package io.takari.maven.testing;

import com.google.inject.Module;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.execution.MojoExecutionEvent;
import org.apache.maven.execution.MojoExecutionListener;
import org.apache.maven.execution.scope.internal.MojoExecutionScope;
import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;
import org.apache.maven.session.scope.internal.SessionScope;

class Maven325Runtime extends Maven321Runtime {

    public Maven325Runtime(Module[] modules) throws Exception {
        super(modules);
    }

    @Override
    public Mojo executeMojo(MavenSession session, MavenProject project, MojoExecution execution) throws Exception {
        SessionScope sessionScope = container.lookup(SessionScope.class);
        try {
            sessionScope.enter();
            sessionScope.seed(MavenSession.class, session);

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

                return mojo;
            } finally {
                executionScope.exit();
            }
        } finally {
            sessionScope.exit();
        }
    }
}
