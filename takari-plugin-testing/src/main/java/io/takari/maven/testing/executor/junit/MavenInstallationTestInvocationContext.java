/*
 * Copyright (c) 2014-2024 Takari, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 */
package io.takari.maven.testing.executor.junit;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionConfigurationException;
import org.junit.jupiter.api.extension.TestTemplateInvocationContext;

final class MavenInstallationTestInvocationContext implements TestTemplateInvocationContext {

    private final MavenInstallationDisplayNameFormatter formatter;
    private final String installation;

    MavenInstallationTestInvocationContext(String installation, MavenInstallationDisplayNameFormatter formatter) {
        this.installation = installation;
        this.formatter = formatter;
    }

    @Override
    public String getDisplayName(int invocationIndex) {
        return this.formatter.format(this.installation);
    }

    @Override
    public List<Extension> getAdditionalExtensions() {
        File mavenHome;
        try {
            mavenHome = new File(this.installation).getCanonicalFile();
        } catch (IOException e) {
            throw new ExtensionConfigurationException("could not access maven installation: " + this.installation, e);
        }
        if (mavenHome.isDirectory()) {
            return Collections.singletonList(new MavenRuntimeBuilderParameterResolver(mavenHome));
        } else {
            throw new ExtensionConfigurationException("Invalid maven installation location " + installation);
        }
    }
}
