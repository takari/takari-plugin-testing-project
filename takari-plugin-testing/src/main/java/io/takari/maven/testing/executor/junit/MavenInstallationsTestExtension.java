/*
 * Copyright (c) 2014-2024 Takari, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 */
package io.takari.maven.testing.executor.junit;

import io.takari.maven.testing.executor.MavenInstallations;
import io.takari.maven.testing.executor.MavenRuntime.MavenRuntimeBuilder;
import java.util.Arrays;
import java.util.stream.Stream;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContextProvider;

/**
 * Provides a {@link TestTemplateInvocationContext} with a {@link MavenRuntimeBuilder}
 * for each Maven installation configured through {@link MavenInstallations}.
 * <p>
 * Not active if Maven home is forced through {@code -Dmaven.home}.
 *
 * @author Philippe Marschall
 */
final class MavenInstallationsTestExtension implements TestTemplateInvocationContextProvider {

    @Override
    public boolean supportsTestTemplate(ExtensionContext context) {
        if (MavenHomeUtils.isForced()) {
            return false;
        }
        // @formatter:off
        return context.getTestClass()
                .map(clazz -> clazz.isAnnotationPresent(MavenInstallations.class))
                .orElse(false);
        // @formatter:on
    }

    @Override
    public Stream<TestTemplateInvocationContext> provideTestTemplateInvocationContexts(ExtensionContext context) {
        String displayName = context.getDisplayName();
        String[] installations = context.getTestClass()
                .orElseThrow()
                .getAnnotation(MavenInstallations.class)
                .value();
        return Arrays.stream(installations)
                .map(installation -> new MavenInstallationTestInvocationContext(
                        installation, new MavenInstallationDisplayNameFormatter(displayName)));
    }
}
