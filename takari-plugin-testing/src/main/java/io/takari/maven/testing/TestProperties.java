/*
 * Copyright (c) 2014-2024 Takari, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 */
package io.takari.maven.testing;

import io.takari.maven.testing.executor.MavenRuntime.MavenRuntimeBuilder;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.TreeMap;
import org.junit.Assert;

public class TestProperties {

    public static final String PROP_CLASSPATH = "classpath";

    /** @deprecated use {@link #PROP_USER_SETTING_FILE} */
    public static final String PROP_USER_SETTING = "userSettings";

    public static final String PROP_USER_SETTING_FILE = "userSettingsFile";

    public static final String PROP_GLOBAL_SETTING_FILE = "globalSettingsFile";

    public static final String PROP_LOCAL_REPOSITORY = "localRepository";

    public static final String PROP_OFFLINE = "offline";

    public static final String PROP_UPDATESNAPSHOTS = "updateSnapshots";

    public static final String PROP_REPOSITORY = "repository.";

    private final Map<String, String> properties;

    public TestProperties() {
        try {
            this.properties = loadProperties();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected Map<String, String> loadProperties() throws IOException {
        Properties p = new Properties();
        try (InputStream os = getClass().getClassLoader().getResourceAsStream("test.properties")) {
            Assert.assertNotNull(
                    "test.properties must be present on test classpath, see https://github.com/takari/takari-plugin-testing-project/blob/master/testproperties.md for me details",
                    os);
            p.load(os);
        }
        Map<String, String> properties = new HashMap<>();
        for (String key : p.stringPropertyNames()) {
            properties.put(key, p.getProperty(key));
        }
        return Collections.unmodifiableMap(properties);
    }

    public String get(String key) {
        return properties.get(key);
    }

    public File getUserSettings() {
        // can be null
        String path = properties.get(PROP_USER_SETTING_FILE);
        if (path == null) {
            path = properties.get(PROP_USER_SETTING);
        }
        if (path == null) {
            return null;
        }
        File file = new File(path);
        Assert.assertTrue("Can read user settings.xml", file.canRead());
        return file;
    }

    public File getGlobalSettings() {
        // can be null
        String path = properties.get(PROP_GLOBAL_SETTING_FILE);
        if (path == null) {
            return null;
        }
        File file = new File(path);
        Assert.assertTrue("Can read global settings.xml", file.canRead());
        return file;
    }

    public File getLocalRepository() {
        String path = properties.get(PROP_LOCAL_REPOSITORY);
        Assert.assertNotNull("Local repository specified", path);
        return new File(path);
    }

    public boolean getOffline() {
        String value = properties.get(PROP_OFFLINE);
        return value != null ? Boolean.parseBoolean(value) : false;
    }

    public boolean getUpdateSnapshots() {
        String value = properties.get(PROP_UPDATESNAPSHOTS);
        return value != null ? Boolean.parseBoolean(value) : false;
    }

    public String getPluginVersion() {
        return properties.get("project.version");
    }

    /**
     * Returns location of the current project classes, i.e. target/classes directory, and all project dependencies with scope=runtime.
     * <p>
     * Useful for testing maven core extensions, {@link MavenRuntimeBuilder#withExtensions(java.util.Collection)}
     */
    public List<File> getRuntimeClasspath() {
        StringTokenizer st = new StringTokenizer(properties.get(PROP_CLASSPATH), File.pathSeparator);
        List<File> dependencies = new ArrayList<>();
        while (st.hasMoreTokens()) {
            dependencies.add(new File(st.nextToken()));
        }
        return dependencies;
    }

    public List<String> getRepositories() {
        TreeMap<String, String> repositories = new TreeMap<>();
        for (Map.Entry<String, String> property : properties.entrySet()) {
            if (property.getKey().startsWith(PROP_REPOSITORY)) {
                repositories.put(property.getKey(), property.getValue());
            }
        }
        return new ArrayList<>(repositories.values());
    }
}
