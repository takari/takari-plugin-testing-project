/*
 * Copyright (c) 2014-2024 Takari, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 */
package io.takari.maven.testing;

import com.google.inject.Module;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

public class TestMavenRuntime extends AbstractTestMavenRuntime implements TestRule {

    public TestMavenRuntime() {
        super();
    }

    public TestMavenRuntime(Module... modules) {
        super(modules);
    }

    @Override
    public Statement apply(final Statement base, final Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                createMavenRuntime();
                try {
                    base.evaluate();
                } finally {
                    shutDownMavenRuntime();
                }
            }
        };
    }
}
