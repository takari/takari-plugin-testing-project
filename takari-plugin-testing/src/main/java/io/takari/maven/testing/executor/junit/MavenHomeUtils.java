/**
 * Copyright (c) 2021 Takari, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package io.takari.maven.testing.executor.junit;

import java.io.File;

import org.junit.jupiter.api.extension.ExtensionConfigurationException;

import io.takari.maven.testing.executor.MavenInstallationUtils;

final class MavenHomeUtils {

  private MavenHomeUtils() {
    throw new AssertionError("not instantiable");
  }

  static boolean isForced() {

    File forcedMavenHome = MavenInstallationUtils.getForcedMavenHome();
    File forcedClassworldsConf = MavenInstallationUtils.getForcedClassworldsConf();

    if (forcedMavenHome != null) {
      if (forcedMavenHome.isDirectory() || (forcedClassworldsConf != null && forcedClassworldsConf.isFile())) {
        return true;
      }
      throw new ExtensionConfigurationException("Invalid -Dmaven.home=" + forcedMavenHome.getAbsolutePath());
    }
    return false;
  }

}
