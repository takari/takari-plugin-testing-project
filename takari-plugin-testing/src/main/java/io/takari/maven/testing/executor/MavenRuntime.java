/*
 * Copyright (c) 2014-2024 Takari, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 */
package io.takari.maven.testing.executor;

import static org.eclipse.m2e.workspace.WorkspaceState2.SYSPROP_STATEFILE_LOCATION;

import io.takari.maven.testing.TestProperties;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MavenRuntime {
    private final MavenLauncher launcher;

    private final TestProperties properties;

    public static class MavenRuntimeBuilder {

        protected final TestProperties properties;

        protected final File mavenHome;

        protected final String mavenVersion;

        protected final File classworldsConf;

        protected final List<String> extensions = new ArrayList<>();

        protected final List<String> args = new ArrayList<>();

        MavenRuntimeBuilder(File mavenHome, File classworldsConf) {
            this.properties = new TestProperties();
            this.mavenHome = mavenHome;
            this.mavenVersion = MavenInstallationUtils.getMavenVersion(mavenHome, classworldsConf);
            this.classworldsConf = classworldsConf;

            StringBuilder workspaceState = new StringBuilder();
            appendLocation(workspaceState, System.getProperty(SYSPROP_STATEFILE_LOCATION));
            appendLocation(workspaceState, properties.get("workspaceStateProperties"));

            String workspaceResolver = properties.get("workspaceResolver");
            if (workspaceState.length() > 0 && isFile(workspaceResolver)) {
                if ("3.2.1".equals(mavenVersion)) {
                    throw new IllegalArgumentException(
                            "Maven 3.2.1 is not supported, see https://jira.codehaus.org/browse/MNG-5591");
                }
                args.add("-D" + SYSPROP_STATEFILE_LOCATION + "=" + workspaceState.toString());
                extensions.add(workspaceResolver);
            }
            // TODO decide if workspace resolution must be enabled and enforced
        }

        private void appendLocation(StringBuilder workspaceState, String location) {
            if (location != null) {
                if (!isFile(location)) {
                    throw new IllegalArgumentException("Not a file " + location);
                }
                if (workspaceState.length() > 0) {
                    workspaceState.append(File.pathSeparator);
                }
                workspaceState.append(location);
            }
        }

        MavenRuntimeBuilder(File mavenHome, File classworldsConf, List<String> extensions, List<String> args) {
            this.properties = new TestProperties();
            this.mavenHome = mavenHome;
            this.mavenVersion = MavenInstallationUtils.getMavenVersion(mavenHome, classworldsConf);
            this.classworldsConf = classworldsConf;
            this.extensions.addAll(extensions);
            this.args.addAll(args);
        }

        private static boolean isFile(String path) {
            return path != null && new File(path).isFile();
        }

        public MavenRuntimeBuilder withExtension(File extensionLocation) {
            assertFileExists("No such file or directory: " + extensionLocation, extensionLocation);
            extensions.add(extensionLocation.getAbsolutePath());
            return this;
        }

        public MavenRuntimeBuilder withExtensions(Collection<File> extensionLocations) {
            for (File extensionLocation : extensionLocations) {
                assertFileExists("No such file or directory: " + extensionLocation, extensionLocation);
                extensions.add(extensionLocation.getAbsolutePath());
            }
            return this;
        }

        private void assertFileExists(String message, File file) {
            if (!file.exists()) {
                throw new AssertionError("No such file or directory: " + file);
            }
        }

        public MavenRuntimeBuilder withCliOptions(String... options) {
            for (String option : options) {
                args.add(option);
            }
            return this;
        }

        public ForkedMavenRuntimeBuilder forkedBuilder() {
            return new ForkedMavenRuntimeBuilder(mavenHome, classworldsConf, extensions, args);
        }

        public MavenRuntime build() throws Exception {
            Embedded3xLauncher launcher =
                    Embedded3xLauncher.createFromMavenHome(mavenHome, classworldsConf, extensions, args);
            return new MavenRuntime(launcher, properties);
        }
    }

    public static class ForkedMavenRuntimeBuilder extends MavenRuntimeBuilder {

        private Map<String, String> environment;
        private final List<String> jvmArgs = new ArrayList<>();

        ForkedMavenRuntimeBuilder(File mavenHome, File classworldsConf) {
            super(mavenHome, classworldsConf);
        }

        ForkedMavenRuntimeBuilder(File mavenHome, File classworldsConf, List<String> extensions, List<String> args) {
            super(mavenHome, classworldsConf, extensions, args);
        }

        public ForkedMavenRuntimeBuilder withEnvironment(Map<String, String> environment) {
            this.environment = new HashMap<>(environment);
            return this;
        }

        @Override
        public ForkedMavenRuntimeBuilder withExtension(File extensionLocation) {
            super.withExtension(extensionLocation);
            return this;
        }

        @Override
        public ForkedMavenRuntimeBuilder withExtensions(Collection<File> extensionLocations) {
            super.withExtensions(extensionLocations);
            return this;
        }

        @Override
        public ForkedMavenRuntimeBuilder withCliOptions(String... options) {
            super.withCliOptions(options);
            return this;
        }

        /**
         * Adds a JVM option, as opposed to application option that can be added via {@link #withCliOptions(String...)}. Use this method to add options you would normally pass to {@code mvn/mvn.bat} via
         * {@code MAVEN_OPTS} environment variable.
         *
         * @param jvmOption the JVM option to add
         * @return this {@link ForkedMavenRuntimeBuilder}
         */
        public ForkedMavenRuntimeBuilder withJvmOption(String jvmOption) {
            this.jvmArgs.add(jvmOption);
            return this;
        }

        /**
         * Add a JVM options, as opposed to application options that can be added via {@link #withCliOptions(String...)}. Use this method to add options you would normally pass to {@code mvn/mvn.bat} via
         * {@code MAVEN_OPTS} environment variable.
         *
         * @param jvmOptions the JVM options to add
         * @return this {@link ForkedMavenRuntimeBuilder}
         */
        public ForkedMavenRuntimeBuilder withJvmOptions(String... jvmOptions) {
            for (String jvmArg : jvmOptions) {
                this.jvmArgs.add(jvmArg);
            }
            return this;
        }

        /**
         * Add a JVM options, as opposed to application options that can be added via {@link #withCliOptions(String...)}. Use this method to add options you would normally pass to {@code mvn/mvn.bat} via
         * {@code MAVEN_OPTS} environment variable.
         *
         * @param jvmOptions the JVM options to add
         * @return this {@link ForkedMavenRuntimeBuilder}
         */
        public ForkedMavenRuntimeBuilder withJvmOptions(Collection<String> jvmOptions) {
            this.jvmArgs.addAll(jvmOptions);
            return this;
        }

        @Override
        public MavenRuntime build() {
            ForkedLauncher launcher =
                    new ForkedLauncher(mavenHome, classworldsConf, extensions, environment, args, jvmArgs);
            return new MavenRuntime(launcher, properties);
        }
    }

    MavenRuntime(MavenLauncher launcher, TestProperties properties) {
        this.launcher = launcher;
        this.properties = properties;
    }

    public static MavenRuntimeBuilder builder(File mavenHome, File classworldsConf) {
        return new MavenRuntimeBuilder(mavenHome, classworldsConf);
    }

    public static ForkedMavenRuntimeBuilder forkedBuilder(File mavenHome) {
        return new ForkedMavenRuntimeBuilder(mavenHome, null);
    }

    public MavenExecution forProject(File multiModuleProjectDirectory) {
        return new MavenExecution(launcher, properties, multiModuleProjectDirectory, null);
    }

    /**
     * @since 2.9
     */
    public MavenExecution forProject(File multiModuleProjectDirectory, String moduleRelpath)
            throws IOException, LauncherException {
        String mavenVersion = launcher.getMavenVersion();
        if (!isVersion330plus(mavenVersion)) {
            throw new UnsupportedOperationException(
                    "Explicit multiModuleProjectDirectory requires Maven 3.3 or newer, current version is "
                            + mavenVersion);
        }
        return new MavenExecution(launcher, properties, multiModuleProjectDirectory, moduleRelpath);
    }

    private boolean isVersion330plus(String version) {
        String[] split = version.split("\\.");
        if (split.length < 2) {
            return false; // can't parse
        }
        if (Integer.parseInt(split[0]) < 3) {
            return false;
        }
        return Integer.parseInt(split[1]) >= 3;
    }

    /**
     * @since 2.0
     */
    public String getMavenVersion() throws IOException, LauncherException {
        return launcher.getMavenVersion();
    }
}
