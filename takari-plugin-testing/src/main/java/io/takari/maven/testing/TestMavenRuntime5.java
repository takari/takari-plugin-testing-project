/**
 * Copyright (c) 2021 Takari, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package io.takari.maven.testing;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import com.google.inject.Module;

/**
 * Like {@link TestMavenRuntime} but for JUnit 5.
 * 
 * @author Philippe Marschall
 */
public class TestMavenRuntime5 extends AbstractTestMavenRuntime implements BeforeEachCallback, AfterEachCallback {

  public TestMavenRuntime5() {
    super();
  }

  public TestMavenRuntime5(Module... modules) {
    super(modules);
  }

  @Override
  public void beforeEach(ExtensionContext context) throws Exception {
    createMavenRuntime();
  }

  @Override
  public void afterEach(ExtensionContext context) {
    shutDownMavenRuntime();
  }

}
