/*
 * Copyright (c) 2014-2024 Takari, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 */
package io.takari.maven.testing;

import com.google.inject.Module;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.util.xml.Xpp3Dom;

public abstract class AbstractTestMavenRuntime {

    private static final DefaultArtifactVersion MAVEN_VERSION;

    static {
        DefaultArtifactVersion version = null;
        String path = "/META-INF/maven/org.apache.maven/maven-core/pom.properties";
        try (InputStream is = TestMavenRuntime.class.getResourceAsStream(path)) {
            Properties properties = new Properties();
            if (is != null) {
                properties.load(is);
            }
            String property = properties.getProperty("version");
            if (property != null) {
                version = new DefaultArtifactVersion(property);
            }
        } catch (IOException e) {
            // odd, where did this come from
        }
        MAVEN_VERSION = version;
    }

    @FunctionalInterface
    private static interface RuntimeFactory {
        MavenRuntime newInstance(Module[] modules) throws Exception;
    }

    // ordered map of supported maven runtime factories
    private static final Map<VersionRange, RuntimeFactory> FACTORIES;

    static {
        Map<VersionRange, RuntimeFactory> factories = new LinkedHashMap<>();
        try {
            factories.put(VersionRange.createFromVersionSpec("[3.0,3.1.1)"), Maven30xRuntime::new);
            factories.put(VersionRange.createFromVersionSpec("[3.1.1,3.2.1)"), Maven311Runtime::new);
            factories.put(VersionRange.createFromVersionSpec("[3.2.1,3.2.5)"), Maven321Runtime::create);
            factories.put(VersionRange.createFromVersionSpec("[3.2.5]"), Maven325Runtime::new);
            // the last entry is expected to handle everything else
            factories.put(VersionRange.createFromVersionSpec("(3.2.5,]"), Maven331Runtime::new);
        } catch (InvalidVersionSpecificationException e) {
            throw new RuntimeException(e);
        }
        FACTORIES = Collections.unmodifiableMap(factories);
    }

    private final Module[] modules;
    private MavenRuntime runtime;

    public class TestDependency {

        private final File file;
        private String groupId = "test";
        private String artifactId;
        private String classifier;
        private String version = "1.0";
        private String type = "jar";
        private String scope = Artifact.SCOPE_COMPILE;
        private boolean optional;

        private TestDependency(File artifact) {
            this.file = artifact;
            this.artifactId = artifact.getName();
        }

        public TestDependency setArtifactId(String artifactId) {
            this.artifactId = artifactId;

            return this;
        }

        public TestDependency setGroupId(String groupId) {
            this.groupId = groupId;

            return this;
        }

        public TestDependency setVersion(String version) {
            this.version = version;

            return this;
        }

        public TestDependency setType(String type) {
            this.type = type;

            return this;
        }

        public TestDependency setOptional(boolean optional) {
            this.optional = optional;

            return this;
        }

        public TestDependency setClassifier(String classifier) {
            this.classifier = classifier;

            return this;
        }

        public TestDependency setScope(String scope) {
            this.scope = scope;

            return this;
        }

        public TestDependency addTo(MavenProject project) throws Exception {
            return addTo(project, true);
        }

        public TestDependency addTo(MavenProject project, boolean direct) throws Exception {
            ArtifactHandler handler = getContainer().lookup(ArtifactHandler.class, type);
            DefaultArtifact artifact =
                    new DefaultArtifact(groupId, artifactId, version, scope, type, classifier, handler);
            artifact.setFile(file);
            artifact.setOptional(optional);
            Set<Artifact> artifacts = project.getArtifacts();
            artifacts.add(artifact);
            project.setArtifacts(artifacts);
            if (direct) {
                Set<Artifact> directDependencies = project.getDependencyArtifacts();
                directDependencies = directDependencies == null
                        ? new LinkedHashSet<Artifact>()
                        : new LinkedHashSet<>(directDependencies);
                directDependencies.add(artifact);
                project.setDependencyArtifacts(directDependencies);
            }

            return this;
        }
    }

    AbstractTestMavenRuntime() {
        this(new Module[0]);
    }

    AbstractTestMavenRuntime(Module... modules) {
        this.modules = modules;
    }

    void createMavenRuntime() throws Exception {
        runtime = newMavenRuntime(modules);
    }

    void shutDownMavenRuntime() {
        runtime.shutdown();
        runtime = null;
    }

    private MavenRuntime newMavenRuntime(Module[] modules) throws Exception {
        for (Map.Entry<VersionRange, RuntimeFactory> entry : FACTORIES.entrySet()) {
            if (entry.getKey().containsVersion(MAVEN_VERSION)) {
                return entry.getValue().newInstance(modules);
            }
        }
        throw new AssertionError(String.format(
                "Maven version %s is not supported, supprted versions: %s", MAVEN_VERSION, FACTORIES.entrySet()));
    }

    public MavenProject readMavenProject(File basedir) throws Exception {
        MavenProject project = runtime.readMavenProject(basedir);
        Objects.requireNonNull(project);
        return project;
    }

    public MavenSession newMavenSession(MavenProject project) throws Exception {
        MavenSession session = runtime.newMavenSession(project.getBasedir());
        session.setCurrentProject(project);
        session.setProjects(Arrays.asList(project));
        return session;
    }

    /** @since 2.9 */
    public MojoExecution newMojoExecution(String goal, Xpp3Dom... parameters) {
        MojoExecution execution = runtime.newMojoExecution(goal);
        if (parameters != null) {
            // TODO decide if this should go to runtime.newMojoExecution
            Xpp3Dom configuration = execution.getConfiguration();
            for (Xpp3Dom parameter : parameters) {
                configuration.addChild(parameter);
            }
        }
        return execution;
    }

    public void executeMojo(File basedir, String goal, Xpp3Dom... parameters) throws Exception {
        MavenProject project = readMavenProject(basedir);
        MavenSession session = newMavenSession(project);
        executeMojo(session, project, goal, parameters);
    }

    public void executeMojo(MavenSession session, MavenProject project, String goal, Xpp3Dom... parameters)
            throws Exception {
        MojoExecution execution = newMojoExecution(goal, parameters);
        executeMojo(session, project, execution);
    }

    public void executeMojo(MavenProject project, String goal, Xpp3Dom... parameters) throws Exception {
        MavenSession session = newMavenSession(project);
        executeMojo(session, project, goal, parameters);
    }

    public void executeMojo(MavenSession session, MavenProject project, MojoExecution execution) throws Exception {
        runtime.executeMojo(session, project, execution);
    }

    public Mojo lookupConfiguredMojo(MavenSession session, MojoExecution execution) throws Exception {
        return runtime.lookupConfiguredMojo(session, execution);
    }

    public DefaultPlexusContainer getContainer() {
        return runtime.getContainer();
    }

    public <T> T lookup(Class<T> role) throws Exception {
        return runtime.lookup(role);
    }

    public static Xpp3Dom newParameter(String name, String value) {
        Xpp3Dom child = new Xpp3Dom(name);
        child.setValue(value);
        return child;
    }

    public TestDependency newDependency(File artifact) {
        return new TestDependency(artifact);
    }
}
