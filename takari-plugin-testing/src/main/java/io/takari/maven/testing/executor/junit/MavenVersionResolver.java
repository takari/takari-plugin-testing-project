/**
 * Copyright (c) 2014 Takari, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package io.takari.maven.testing.executor.junit;

import io.takari.maven.testing.TestMavenRuntime;
import io.tesla.proviso.archive.UnArchiver;

import java.io.File;
import java.util.Collections;

import org.junit.runners.model.InitializationError;

abstract class MavenVersionResolver {
  private static final UnArchiver archiver = new UnArchiver(Collections.<String>emptyList(), Collections.<String>emptyList(), false, false);

  public void resolve(String[] versions) throws Exception {
    TestMavenRuntime maven = null;
    try {
      for (String version : versions) {
        File mavenHome = new File("target/maven-installation/apache-maven-" + version).getCanonicalFile();
        if (!mavenHome.isDirectory()) {
          if (maven == null) {
            maven = new TestMavenRuntime();
            maven.setup();
          }
          File archive = null;
          try {
            archive = maven.resolve("org.apache.maven", "apache-maven", version, "bin", "tar.gz").getFile();
          } catch (Exception e) {
            error(version, e);
          }
          if (archive != null) {
            archiver.unarchive(archive, mavenHome);
          }
        }
        if (mavenHome.isDirectory()) {
          resolved(mavenHome, version);
        }
      }
    } finally {
      if (maven != null) {
        maven.shutdown();
      }
    }
  }

  protected abstract void error(String version, Exception e);

  protected abstract void resolved(File mavenHome, String version) throws InitializationError;
}
