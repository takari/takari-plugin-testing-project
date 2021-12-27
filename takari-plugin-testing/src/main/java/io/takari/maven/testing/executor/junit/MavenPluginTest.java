/**
 * Copyright (c) 2021 Takari, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package io.takari.maven.testing.executor.junit;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Meta-annotation for Maven plugin integration tests, registers all necessary extensions.
 * 
 * @author Philippe Marschall
 */
@Documented
@Retention(RUNTIME)
@Target({ANNOTATION_TYPE, METHOD})
@TestTemplate
@Inherited
@ExtendWith({
  ForcedMavenRuntimeBuilderParameterResolver.class,
  MavenInstallationsTestExtension.class,
  MavenVersionsTestExtension.class})
public @interface MavenPluginTest {

}
