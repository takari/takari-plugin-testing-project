/**
 * Copyright (c) 2021 Takari, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package io.takari.maven.testing.executor.junit;

import java.io.File;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

import io.takari.maven.testing.executor.MavenInstallationUtils;
import io.takari.maven.testing.executor.MavenRuntime;
import io.takari.maven.testing.executor.MavenRuntime.MavenRuntimeBuilder;

/**
 * Provides a {@link MavenRuntimeBuilder} based on {@code -Dmaven.home}
 * and optionally {@code -Dclassworlds.conf}.
 * 
 * @author Philippe Marschall
 */
final class ForcedMavenRuntimeBuilderParameterResolver implements ParameterResolver {

  ForcedMavenRuntimeBuilderParameterResolver() {
    super();
  }

  @Override
  public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
    return (parameterContext.getParameter().getType() == MavenRuntimeBuilder.class)
        && MavenHomeUtils.isForced();
  }

  @Override
  public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
      throws ParameterResolutionException {
    File forcedMavenHome = MavenInstallationUtils.getForcedMavenHome();
    File forcedClassworldsConf = MavenInstallationUtils.getForcedClassworldsConf();

    if (forcedMavenHome != null) {
      if (forcedMavenHome.isDirectory() || ((forcedClassworldsConf != null) && forcedClassworldsConf.isFile())) {
        return MavenRuntime.builder(forcedMavenHome, forcedClassworldsConf);
      }
    }
    throw new ParameterResolutionException("Invalid -Dmaven.home=" + forcedMavenHome.getAbsolutePath());
  }

}
