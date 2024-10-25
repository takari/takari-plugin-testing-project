/*
 * Copyright (c) 2014-2024 Takari, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 */
package io.takari.maven.testing.executor;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

import static io.takari.maven.testing.executor.MavenInstallationUtils.SYSPROP_MAVEN_MAIN_CLASS;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.exec.ShutdownHookProcessDestroyer;
import org.codehaus.plexus.util.Os;

/**
 * @author Benjamin Bentmann
 */
class ForkedLauncher implements MavenLauncher {

    private final File mavenHome;

    private final File classworldsJar;

    private final Map<String, String> envVars;

    private final List<String> extensions;

    private final List<String> args;

    private final List<String> jvmArgs;

    public ForkedLauncher(
            File mavenHome,
            File classworldsConf,
            List<String> extensions,
            Map<String, String> envVars,
            List<String> args,
            List<String> jvmArgs) {
        this.args = args;
        this.jvmArgs = jvmArgs;
        if (mavenHome == null) {
            throw new NullPointerException();
        }
        if (classworldsConf != null) {
            throw new IllegalArgumentException("Custom classworlds configuration file is not supported");
        }

        this.mavenHome = mavenHome;
        this.envVars = envVars;
        this.extensions = extensions;

        File classworldsJar = null;
        File[] files = new File(mavenHome, "boot").listFiles();
        if (files != null) {
            for (File file : files) {
                String name = file.getName();
                if (name.startsWith("plexus-classworlds-") && name.endsWith(".jar")) {
                    classworldsJar = file;
                    break;
                }
            }
        }
        if (classworldsJar == null) {
            throw new IllegalArgumentException("Invalid maven home " + mavenHome);
        }
        this.classworldsJar = classworldsJar;
    }

    public int run(
            String[] cliArgs,
            Map<String, String> envVars,
            File multiModuleProjectDirectory,
            File workingDirectory,
            File logFile)
            throws IOException, LauncherException {
        String javaHome;
        if (envVars == null || envVars.get("JAVA_HOME") == null) {
            javaHome = System.getProperty("java.home");
        } else {
            javaHome = envVars.get("JAVA_HOME");
        }

        File executable = new File(javaHome, Os.isFamily(Os.FAMILY_WINDOWS) ? "bin/javaw.exe" : "bin/java");

        CommandLine cli = new CommandLine(executable);
        cli.addArgument("-classpath").addArgument(classworldsJar.getAbsolutePath());
        cli.addArgument("-Dclassworlds.conf=" + new File(mavenHome, "bin/m2.conf").getAbsolutePath());
        cli.addArgument("-Dmaven.home=" + mavenHome.getAbsolutePath());
        cli.addArgument("-Dmaven.multiModuleProjectDirectory=" + multiModuleProjectDirectory.getAbsolutePath());
        cli.addArgument("-D" + SYSPROP_MAVEN_MAIN_CLASS + "=org.apache.maven.cling.MavenCling");

        cli.addArguments(jvmArgs.toArray(new String[jvmArgs.size()]));

        cli.addArgument("org.codehaus.plexus.classworlds.launcher.Launcher");

        cli.addArguments(args.toArray(new String[args.size()]));
        if (extensions != null && !extensions.isEmpty()) {
            cli.addArgument("-Dmaven.ext.class.path=" + toPath(extensions));
        }

        cli.addArguments(cliArgs);

        Map<String, String> env = new HashMap<>();
        if (mavenHome != null) {
            env.put("M2_HOME", mavenHome.getAbsolutePath());
        }
        if (envVars != null) {
            env.putAll(envVars);
        }
        if (envVars == null || envVars.get("JAVA_HOME") == null) {
            env.put("JAVA_HOME", System.getProperty("java.home"));
        }

        DefaultExecutor executor = new DefaultExecutor();
        executor.setProcessDestroyer(new ShutdownHookProcessDestroyer());
        executor.setWorkingDirectory(workingDirectory.getAbsoluteFile());

        try (OutputStream log = new FileOutputStream(logFile)) {
            PrintStream out = new PrintStream(log);
            out.format("Maven Executor implementation: %s\n", getClass().getName());
            out.format("Maven home: %s\n", mavenHome);
            out.format("Build work directory: %s\n", workingDirectory);
            out.format("Environment: %s\n", env);
            out.format("Command line: %s\n\n", cli.toString());
            out.flush();

            PumpStreamHandler streamHandler = new PumpStreamHandler(log);
            executor.setStreamHandler(streamHandler);
            return executor.execute(cli, env); // this throws ExecuteException if process return code != 0
        } catch (ExecuteException e) {
            throw new LauncherException("Failed to run Maven: " + e.getMessage() + "\n" + cli, e);
        }
    }

    private static String toPath(List<String> strings) {
        StringBuilder sb = new StringBuilder();
        for (String string : strings) {
            if (sb.length() > 0) {
                sb.append(File.pathSeparatorChar);
            }
            sb.append(string);
        }
        return sb.toString();
    }

    @Override
    public int run(String[] cliArgs, File multiModuleProjectDirectory, File workingDirectory, File logFile)
            throws IOException, LauncherException {
        return run(cliArgs, envVars, multiModuleProjectDirectory, workingDirectory, logFile);
    }

    @Override
    public String getMavenVersion() throws IOException, LauncherException {
        // TODO cleanup, there is no need to write log file, for example

        File logFile;
        try {
            logFile = File.createTempFile("maven", "log");
        } catch (IOException e) {
            throw new LauncherException("Error creating temp file", e);
        }

        // disable EMMA runtime controller port allocation, should be harmless if EMMA is not used
        Map<String, String> envVars = Collections.singletonMap("MAVEN_OPTS", "-Demma.rt.control=false");
        run(new String[] {"--version"}, envVars, new File(""), new File(""), logFile);

        List<String> logLines = Files.readAllLines(logFile.toPath(), Charset.defaultCharset());
        // noinspection ResultOfMethodCallIgnored
        logFile.delete();

        String version = extractMavenVersion(logLines);

        if (version == null) {
            throw new LauncherException(
                    "Illegal Maven output: String 'Maven' not found in the following output:\n" + join(logLines, "\n"));
        } else {
            return version;
        }
    }

    private String join(List<String> lines, String eol) {
        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            sb.append(line).append(eol);
        }
        return sb.toString();
    }

    static String extractMavenVersion(List<String> logLines) {
        String version = null;

        final Pattern mavenVersion = Pattern.compile("(?i).*Maven.*? ([0-9]\\.\\S*).*");

        for (Iterator<String> it = logLines.iterator(); version == null && it.hasNext(); ) {
            String line = it.next();

            Matcher m = mavenVersion.matcher(line);
            if (m.matches()) {
                version = m.group(1);
            }
        }

        return version;
    }
}
