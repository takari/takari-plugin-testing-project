/**
 * Copyright (c) 2014 Takari, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package io.takari.maven.testing.executor.junit;

import io.takari.maven.testing.TestProperties;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.xml.xpath.XPathFactory;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.junit.runners.model.InitializationError;
import org.xml.sax.InputSource;

abstract class MavenVersionResolver {
  private static final XPathFactory xpathFactory = XPathFactory.newInstance();

  public void resolve(String[] versions) throws Exception {
    Collection<URL> repositories = null;
    TestProperties properties = new TestProperties();
    for (String version : versions) {
      File basdir = new File("target/maven-installation").getCanonicalFile();
      File mavenHome = new File(basdir, "apache-maven-" + version).getCanonicalFile();
      if (!mavenHome.isDirectory()) {
        if (repositories == null) {
          repositories = getRepositories(properties);
        }
        File archive = null;
        try {
          archive = downloadMaven(properties.getLocalRepository(), repositories, version);
        } catch (Exception e) {
          error(version, e);
        }
        if (archive != null) {
          unarchive(archive, basdir);
        }
      }
      if (mavenHome.isDirectory()) {
        resolved(mavenHome, version);
      }
    }
  }

  private void unarchive(File archive, File directory) throws IOException {
    try (TarArchiveInputStream ais = new TarArchiveInputStream(new GzipCompressorInputStream(new FileInputStream(archive)))) {
      TarArchiveEntry entry;
      while ((entry = ais.getNextTarEntry()) != null) {
        if (entry.isFile()) {
          String name = entry.getName();
          File file = new File(directory, name);
          file.getParentFile().mkdirs();
          try (OutputStream os = new BufferedOutputStream(new FileOutputStream(file))) {
            copy(ais, os);
          }
          int mode = entry.getMode();
          if (mode != -1 && (mode & 0100) != 0) {
            try {
              Path path = file.toPath();
              Set<PosixFilePermission> permissions = Files.getPosixFilePermissions(path);
              permissions.add(PosixFilePermission.OWNER_EXECUTE);
              Files.setPosixFilePermissions(path, permissions);
            } catch (UnsupportedOperationException e) {
              // must be windows, ignore
            }
          }
        }
      }
    }
  }

  private Collection<URL> getRepositories(TestProperties properties) throws Exception {
    LinkedHashSet<URL> repositories = new LinkedHashSet<>();
    for (String property : properties.getRepositories()) {
      InputSource is = new InputSource(new StringReader("<repository>" + property + "</repository>"));
      String url = getXPath(is, "/repository/url");
      if (url == null) {
        continue; // malformed test.properties
      }
      if (!url.endsWith("/")) {
        url = url + "/";
      }
      repositories.add(new URL(url));
    }
    return repositories;
  }

  private String getXPath(InputSource is, String path) throws Exception {
    String value = xpathFactory.newXPath().compile(path).evaluate(is);
    if (value == null) {
      return null;
    }
    value = value.trim();
    return !value.isEmpty() ? value : null;
  }

  private File downloadMaven(File targetdir, Collection<URL> repositories, String version) throws Exception {
    String versionDir = "org/apache/maven/apache-maven/" + version + "/";
    String filename = "apache-maven-" + version + "-bin.tar.gz";
    File file = new File(targetdir, versionDir + filename);
    if (file.canRead()) {
      return file;
    }
    for (URL repository : repositories) {
      String effectiveVersion = version;
      if (version.endsWith("-SNAPSHOT")) {
        effectiveVersion = getQualifiedVersion(repository, versionDir);
      }
      if (effectiveVersion == null) {
        continue;
      }
      filename = "apache-maven-" + effectiveVersion + "-bin.tar.gz";
      file = new File(targetdir, versionDir + filename);
      if (file.canRead()) {
        return file;
      }
      URL resource = new URL(repository, versionDir + filename);
      try (InputStream is = openStream(resource)) {
        copy(is, file);
        return file;
      } catch (FileNotFoundException e) {
        // try next repository, if any
      }
    }
    throw new FileNotFoundException("Could not download maven version " + version + " from any configured repository");
  }

  private String getQualifiedVersion(URL repository, String versionDir) throws Exception {
    URL resource = new URL(repository, versionDir + "maven-metadata.xml");
    try (InputStream is = openStream(resource)) {
      ByteArrayOutputStream buf = new ByteArrayOutputStream();
      copy(is, buf);
      InputSource xml = new InputSource(new ByteArrayInputStream(buf.toByteArray()));
      String version = getXPath(xml, "//metadata/versioning/snapshotVersions/snapshotVersion[extension='tar.gz']/value");
      if (version == null) {
        return null;
      }
      version = version.trim();
      return version.isEmpty() ? null : version;
    } catch (FileNotFoundException e) {
      return null;
    }
  }

  private InputStream openStream(URL resource) throws IOException {
    URLConnection connection = resource.openConnection();
    if (connection instanceof HttpURLConnection) {
      // for some reason, nexus version 2.11.1-01 returns partial maven-metadata.xml
      // unless request User-Agent header is set to non-default value
      connection.addRequestProperty("User-Agent", "takari-plugin-testing");
      int responseCode = ((HttpURLConnection) connection).getResponseCode();
      if (responseCode < 200 || responseCode > 299) {
        String responseMessage = ((HttpURLConnection) connection).getResponseMessage();
        throw new IOException(String.format("HTTP/%d %s", responseCode, responseMessage));
      }
    }
    return connection.getInputStream();
  }

  private void copy(InputStream from, File to) throws IOException {
    to.getParentFile().mkdirs();
    try (OutputStream out = new FileOutputStream(to)) {
      copy(from, out);
    }
  }

  private void copy(InputStream from, OutputStream to) throws IOException {
    byte[] buf = new byte[4096];
    int len;
    while ((len = from.read(buf)) > 0) {
      to.write(buf, 0, len);
    }
  }

  protected abstract void error(String version, Exception e);

  protected abstract void resolved(File mavenHome, String version) throws InitializationError;
}
