/**
 * Copyright (c) 2014 Takari, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package io.takari.maven.testing;

import org.junit.Rule;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * Junit4 test {@link Rule} to extract and assert test resources.
 */
public class TestResources extends AbstractTestResources implements TestRule {

  public TestResources() {
    super();
  }

  public TestResources(String projectsDir, String workDir) {
    super(projectsDir, workDir);
  }

  @Override
  public Statement apply(Statement base, Description d) {
    return new Statement() {
      @Override
      public void evaluate() throws Throwable {
        starting(d.getTestClass(), d.getMethodName());
        base.evaluate();
      }
    };
  }

  @Override
  String getRequiredAnnotationClassName() {
    return "org.junit.Rule";
  }

}
