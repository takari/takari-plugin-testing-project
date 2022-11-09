/**
 * Copyright (c) 2014 Takari, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package io.takari.maven.testing;


//some of the code was originally copied from org.apache.maven.plugin.testing.AbstractMojoTestCase and org.apache.maven.plugin.testing.MojoRule

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import static org.junit.Assert.assertNotNull;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Constructor;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.maven.DefaultMaven;
import org.apache.maven.Maven;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.DefaultMavenExecutionResult;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequestPopulator;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.internal.MojoDescriptorCreator;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.PluginParameterExpressionEvaluator;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.Parameter;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptorBuilder;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.repository.RepositorySystem;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.building.DefaultSettingsBuildingRequest;
import org.apache.maven.settings.building.SettingsBuilder;
import org.apache.maven.settings.building.SettingsBuildingRequest;
import org.codehaus.plexus.ContainerConfiguration;
import org.codehaus.plexus.DefaultContainerConfiguration;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.classworlds.ClassWorld;
import org.codehaus.plexus.component.configurator.ComponentConfigurator;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluator;
import org.codehaus.plexus.component.repository.ComponentDescriptor;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.codehaus.plexus.configuration.xml.XmlPlexusConfiguration;
import org.codehaus.plexus.util.InterpolationFilterReader;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.XmlStreamReader;
import org.codehaus.plexus.util.xml.Xpp3Dom;

import com.google.inject.Module;

class Maven30xRuntime implements MavenRuntime {
  private static final String PATH_PLUGINXML = "META-INF/maven/plugin.xml";

  protected final TestProperties properties = new TestProperties();

  protected final DefaultPlexusContainer container;
  protected final Map<String, MojoDescriptor> mojoDescriptors;

  public Maven30xRuntime(Module[] modules) throws Exception {
    ClassWorld classWorld = new ClassWorld("plexus.core", Thread.currentThread().getContextClassLoader());
    ContainerConfiguration cc = new DefaultContainerConfiguration() //
        .setClassWorld(classWorld) //
        .setClassPathScanning(PlexusConstants.SCANNING_INDEX) //
        .setAutoWiring(true) //
        .setName("maven");
    this.container = new DefaultPlexusContainer(cc, modules);
    this.mojoDescriptors = Collections.unmodifiableMap(readPluginXml(container));
  }

  private Map<String, MojoDescriptor> readPluginXml(DefaultPlexusContainer container) throws Exception {
    Map<String, MojoDescriptor> mojoDescriptors = new HashMap<String, MojoDescriptor>();
    Enumeration<URL> resources = getClass().getClassLoader().getResources(PATH_PLUGINXML);
    while (resources.hasMoreElements()) {
      InputStream is = resources.nextElement().openStream();

      if (is == null) {
        return Collections.emptyMap();
      }

      XmlStreamReader reader = new XmlStreamReader(is);

      @SuppressWarnings("rawtypes")
      Map contextData = container.getContext().getContextData();
      @SuppressWarnings("unchecked")
      InterpolationFilterReader interpolationFilterReader = new InterpolationFilterReader(new BufferedReader(reader), contextData);

      PluginDescriptor pluginDescriptor = new PluginDescriptorBuilder().build(interpolationFilterReader);

      // Due this commit to prevent NPE https://github.com/apache/maven/commit/05b748ff6aa15aa63a9c9d5f9f5679f47bf9e83d
      // The pluginDescriptor.plugin is set ONLY when it already went through DefaultMavenPluginManager, nothing
      // else sets this.
      Plugin plugin = new Plugin();
      plugin.setGroupId( pluginDescriptor.getGroupId() );
      plugin.setArtifactId( pluginDescriptor.getArtifactId() );
      plugin.setVersion( pluginDescriptor.getVersion() );
      pluginDescriptor.setPlugin( plugin );

      Artifact artifact = container.lookup(RepositorySystem.class) //
          .createArtifact(pluginDescriptor.getGroupId(), pluginDescriptor.getArtifactId(), pluginDescriptor.getVersion(), ".jar");

      artifact.setFile(getPluginArtifactFile());
      pluginDescriptor.setPluginArtifact(artifact);
      pluginDescriptor.setArtifacts(Arrays.asList(artifact));

      for (ComponentDescriptor<?> desc : pluginDescriptor.getComponents()) {
        container.addComponentDescriptor(desc);
      }

      for (MojoDescriptor mojoDescriptor : pluginDescriptor.getMojos()) {
        // TODO decide how to handle duplicate goals
        // this is possible when one plugin extends another
        // for example, Tycho 'compile' goal is an extension of standard maven 'compile'
        mojoDescriptors.put(mojoDescriptor.getGoal(), mojoDescriptor);
      }
    }
    return mojoDescriptors;
  }

  /**
   * Returns best-effort plugin artifact file.
   * <p>
   * First, attempts to determine parent directory of META-INF directory holding the plugin descriptor. If META-INF parent directory cannot be determined, falls back to test basedir.
   */
  private File getPluginArtifactFile() throws IOException {
    final String pluginDescriptorLocation = PATH_PLUGINXML;
    final URL resource = getClass().getResource("/" + pluginDescriptorLocation);

    File file = null;

    // attempt to resolve relative to META-INF/maven/plugin.xml first
    if (resource != null) {
      if ("file".equalsIgnoreCase(resource.getProtocol())) {
        String path = resource.getPath();
        if (path.endsWith(pluginDescriptorLocation)) {
          file = new File(path.substring(0, path.length() - pluginDescriptorLocation.length()));
        }
      } else if ("jar".equalsIgnoreCase(resource.getProtocol())) {
        // TODO is there a helper for this somewhere?
        try {
          URL jarfile = new URL(resource.getPath());
          if ("file".equalsIgnoreCase(jarfile.getProtocol())) {
            String path = jarfile.getPath();
            if (path.endsWith(pluginDescriptorLocation)) {
              file = new File(path.substring(0, path.length() - pluginDescriptorLocation.length() - 2));
            }
          }
        } catch (MalformedURLException e) {
          // not jar:file:/ URL, too bad
        }
      }
    }

    // fallback to test project basedir if couldn't resolve relative to META-INF/maven/plugin.xml
    if (file == null || !file.exists()) {
      file = new File("").getCanonicalFile();
    }

    return file.getCanonicalFile();
  }

  @Override
  public void shutdown() {
    container.dispose();
  }

  @Override
  public MavenProject readMavenProject(File basedir) throws Exception {
    File pom = new File(basedir, "pom.xml");
    MavenExecutionRequest request = newExecutionRequest();
    request.setBaseDirectory(basedir);
    ProjectBuildingRequest configuration = getProjectBuildingRequest(request);
    return container.lookup(ProjectBuilder.class).build(getPomFile(pom), configuration).getProject();
  }

  protected ProjectBuildingRequest getProjectBuildingRequest(MavenExecutionRequest request) throws ComponentLookupException {
    // TODO populate repository system session... if somebody asks for it really nicely
    return request.getProjectBuildingRequest();
  }

  protected File getPomFile(File pom) throws IOException {
    if (!pom.exists()) {
      try (BufferedWriter w = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(pom), "UTF-8"))) {
        w.write("<?xml version='1.0' encoding='UTF-8'?>\n");
        w.write("<project>\n");
        w.write("<modelVersion>4.0.0</modelVersion>\n");
        w.write("<groupId>test</groupId>\n");
        w.write("<artifactId>test</artifactId>\n");
        w.write("<version>1</version>\n");
        w.write("</project>\n");
      }
    }
    return pom;
  }

  @Override
  public MojoExecution newMojoExecution(String goal) {
    MojoDescriptor mojoDescriptor = mojoDescriptors.get(goal);
    assertNotNull(String.format("The MojoDescriptor for the goal %s cannot be null.", goal), mojoDescriptor);
    MojoExecution execution = new MojoExecution(mojoDescriptor);
    execution.setConfiguration(new Xpp3Dom("configuration"));
    return execution;
  }

  // copy&paste from
  // org.apache.maven.lifecycle.internal.DefaultLifecycleExecutionPlanCalculator.finalizeMojoConfiguration(MojoExecution)
  protected void finalizeMojoConfiguration(MojoExecution mojoExecution) {
    MojoDescriptor mojoDescriptor = mojoExecution.getMojoDescriptor();

    Xpp3Dom executionConfiguration = mojoExecution.getConfiguration();
    if (executionConfiguration == null) {
      executionConfiguration = new Xpp3Dom("configuration");
    }

    Xpp3Dom defaultConfiguration = MojoDescriptorCreator.convert(mojoDescriptor);

    Xpp3Dom finalConfiguration = new Xpp3Dom("configuration");

    if (mojoDescriptor.getParameters() != null) {
      for (Parameter parameter : mojoDescriptor.getParameters()) {
        Xpp3Dom parameterConfiguration = executionConfiguration.getChild(parameter.getName());

        if (parameterConfiguration == null) {
          parameterConfiguration = executionConfiguration.getChild(parameter.getAlias());
        }

        Xpp3Dom parameterDefaults = defaultConfiguration.getChild(parameter.getName());

        parameterConfiguration = Xpp3Dom.mergeXpp3Dom(parameterConfiguration, parameterDefaults, Boolean.TRUE);

        if (parameterConfiguration != null) {
          parameterConfiguration = new Xpp3Dom(parameterConfiguration, parameter.getName());

          if (StringUtils.isEmpty(parameterConfiguration.getAttribute("implementation")) && StringUtils.isNotEmpty(parameter.getImplementation())) {
            parameterConfiguration.setAttribute("implementation", parameter.getImplementation());
          }

          finalConfiguration.addChild(parameterConfiguration);
        }
      }
    }

    mojoExecution.setConfiguration(finalConfiguration);
  }

  @Override
  public MavenSession newMavenSession(File basedir) throws Exception {
    MavenExecutionRequest request = newExecutionRequest();
    MavenExecutionResult result = new DefaultMavenExecutionResult();
    DefaultMaven maven = (DefaultMaven) container.lookup(Maven.class);
    try {
      Object repositorySession = maven.getClass().getMethod("newRepositorySession", MavenExecutionRequest.class).invoke(maven, request);
      Class<?> repositorySessionClass = repositorySession.getClass().getClassLoader().loadClass("org.sonatype.aether.RepositorySystemSession");
      Constructor<MavenSession> constructor = MavenSession.class.getConstructor(PlexusContainer.class, repositorySessionClass, MavenExecutionRequest.class, MavenExecutionResult.class);
      return constructor.newInstance(container, repositorySession, request, result);
    } catch (ReflectiveOperationException | SecurityException e) {
      throw new RuntimeException(e);
    }
  }

  @SuppressWarnings("deprecation")
  protected MavenExecutionRequest newExecutionRequest() throws Exception {

    // system properties
    Properties buildProperties = getMavenBuildProperties();
    String mavenVersion = buildProperties.getProperty("version");
    Properties systemProperties = new Properties();
    systemProperties.putAll(System.getProperties()); // TODO not thread safe
    systemProperties.setProperty("maven.version", mavenVersion);
    systemProperties.setProperty("maven.build.version", mavenVersion);

    // request with initial configuration
    MavenExecutionRequest request = new DefaultMavenExecutionRequest();
    request.setLocalRepositoryPath(properties.getLocalRepository());
    request.setUserSettingsFile(properties.getUserSettings());
    request.setGlobalSettingsFile(properties.getGlobalSettings());
    request.setOffline(properties.getOffline());
    request.setUpdateSnapshots(properties.getUpdateSnapshots());
    request.setSystemProperties(systemProperties);

    // read settings
    SettingsBuildingRequest settingsRequest = new DefaultSettingsBuildingRequest();
    settingsRequest.setGlobalSettingsFile(request.getGlobalSettingsFile());
    settingsRequest.setUserSettingsFile(request.getUserSettingsFile());
    settingsRequest.setSystemProperties(request.getSystemProperties());
    settingsRequest.setUserProperties(request.getUserProperties());
    Settings settings = lookup(SettingsBuilder.class).build(settingsRequest).getEffectiveSettings();

    MavenExecutionRequestPopulator populator = container.lookup(MavenExecutionRequestPopulator.class);
    request = populator.populateFromSettings(request, settings);
    return populator.populateDefaults(request);
  }

  private Properties getMavenBuildProperties() {
    Properties properties = new Properties();
    try (InputStream is = getClass().getResourceAsStream("/META-INF/maven/org.apache.maven/maven-core/pom.properties")) {
      properties.load(is);
    } catch (IOException e) {
      // ignore
    }
    return properties;
  }

  @Override
  public Mojo executeMojo(MavenSession session, MavenProject project, MojoExecution execution) throws Exception {
    Mojo mojo = lookupConfiguredMojo(session, execution);
    mojo.execute();
    return mojo;
  }

  @Override
  public Mojo lookupConfiguredMojo(MavenSession session, MojoExecution execution) throws Exception {
    MavenProject project = session.getCurrentProject();
    MojoDescriptor mojoDescriptor = execution.getMojoDescriptor();

    Mojo mojo = container.lookup(Mojo.class, mojoDescriptor.getRoleHint());

    ExpressionEvaluator evaluator = new PluginParameterExpressionEvaluator(session, execution);

    Xpp3Dom configuration = null;
    Plugin plugin = project.getPlugin(mojoDescriptor.getPluginDescriptor().getPluginLookupKey());
    if (plugin != null) {
      configuration = (Xpp3Dom) plugin.getConfiguration();
    }
    if (configuration == null) {
      configuration = new Xpp3Dom("configuration");
    }
    configuration = Xpp3Dom.mergeXpp3Dom(configuration, execution.getConfiguration());
    execution.setConfiguration(configuration);
    finalizeMojoConfiguration(execution);

    PlexusConfiguration pluginConfiguration = new XmlPlexusConfiguration(execution.getConfiguration());

    String configuratorHint = "basic";
    if (mojoDescriptor.getComponentConfigurator() != null) {
      configuratorHint = mojoDescriptor.getComponentConfigurator();
    }

    ComponentConfigurator configurator = container.lookup(ComponentConfigurator.class, configuratorHint);

    configurator.configureComponent(mojo, pluginConfiguration, evaluator, container.getContainerRealm());

    return mojo;
  }

  @Override
  public DefaultPlexusContainer getContainer() {
    return container;
  }

  @Override
  public <T> T lookup(Class<T> role) throws ComponentLookupException {
    return container.lookup(role);
  }

}
