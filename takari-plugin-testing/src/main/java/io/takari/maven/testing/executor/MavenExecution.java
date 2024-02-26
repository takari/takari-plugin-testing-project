/*
 * Copyright (c) 2014-2024 Takari, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 */
package io.takari.maven.testing.executor;

import io.takari.maven.testing.TestProperties;
import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class MavenExecution {

    private final MavenLauncher launcher;

    private final TestProperties properties;

    private final File multiModuleProjectDirectory;

    private final File basedir;

    private final List<String> cliOptions = new ArrayList<>();

    MavenExecution(
            MavenLauncher launcher, TestProperties properties, File multiModuleProjectDirectory, String moduleRelpath) {
        this.launcher = launcher;
        this.properties = properties;
        this.multiModuleProjectDirectory = multiModuleProjectDirectory;
        this.basedir = moduleRelpath != null
                ? new File(multiModuleProjectDirectory, moduleRelpath)
                : multiModuleProjectDirectory;
    }

    public MavenExecutionResult execute(String... goals) throws Exception {
        File logFile = new File(basedir, "log.txt");

        List<String> args = new ArrayList<>();

        File userSettings = properties.getUserSettings();
        if (userSettings != null && userSettings.isFile() && !isOption(cliOptions, "-s", true)) {
            args.add("-s");
            args.add(userSettings.getAbsolutePath());
        }
        if (!isOption(cliOptions, "-Dmaven.repo.local=", false)) {
            args.add("-Dmaven.repo.local=" + properties.getLocalRepository().getAbsolutePath());
        }
        args.add("-Dit-plugin.version=" + properties.getPluginVersion()); // TODO deprecated and remove
        args.add("-Dit-project.version=" + properties.getPluginVersion());
        args.addAll(cliOptions);

        for (String goal : goals) {
            args.add(goal);
        }

        try {
            launcher.run(args.toArray(new String[args.size()]), multiModuleProjectDirectory, basedir, logFile);
        } catch (Exception e) {
            String ciEnvar = System.getenv("CONTINUOUS_INTEGRATION");
            if (ciEnvar != null && ciEnvar.equalsIgnoreCase("true")) {
                String logFileContent = new String(Files.readAllBytes(logFile.toPath()));
                System.out.println(logFileContent);
            }
            throw e;
        }

        return new MavenExecutionResult(basedir, logFile);
    }

    private boolean isOption(List<String> args, String str, boolean exact) {
        for (String arg : args) {
            if (exact && str.equals(arg)) {
                return true;
            } else if (!exact && arg.startsWith(str)) {
                return true;
            }
        }
        return false;
    }

    public MavenExecution withCliOption(String string) {
        cliOptions.add(string);
        return this;
    }

    public MavenExecution withCliOptions(String... strings) {
        for (String string : strings) {
            cliOptions.add(string);
        }
        return this;
    }
}
