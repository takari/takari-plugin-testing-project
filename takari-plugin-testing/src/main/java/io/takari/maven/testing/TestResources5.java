/*
 * Copyright (c) 2014-2024 Takari, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 */
package io.takari.maven.testing;

import java.lang.reflect.Method;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * JUnit 5 extension to extract and assert test resources.
 */
public class TestResources5 extends AbstractTestResources implements BeforeEachCallback {

    public TestResources5() {
        super();
    }

    public TestResources5(String projectsDir, String workDir) {
        super(projectsDir, workDir);
    }

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        String methodName = context.getTestMethod().map(Method::getName).orElse(null);
        starting(context.getRequiredTestClass(), methodName);
    }

    @Override
    String getRequiredAnnotationClassName() {
        return "org.junit.jupiter.api.extension.RegisterExtension";
    }
}
