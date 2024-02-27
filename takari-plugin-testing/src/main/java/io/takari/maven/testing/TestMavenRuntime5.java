/*
 * Copyright (c) 2014-2024 Takari, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 */
package io.takari.maven.testing;

import com.google.inject.Module;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

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
