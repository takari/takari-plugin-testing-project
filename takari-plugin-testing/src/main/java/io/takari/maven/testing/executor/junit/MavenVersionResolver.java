/**
 * Copyright (c) 2014 Takari, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package io.takari.maven.testing.executor.junit;

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
import java.lang.reflect.Method;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.junit.runners.model.InitializationError;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import io.takari.maven.testing.TestProperties;

abstract class MavenVersionResolver {
  private static final XPathFactory xpathFactory = XPathFactory.newInstance();
  private static final DocumentBuilderFactory documentBuilderFactory;

  static {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    // settings.xml may have default xmlns, which may confuse xpath if not suppressed
    factory.setNamespaceAware(false);
    documentBuilderFactory = factory;
  }

  private static class Credentials {
    public final String username;
    public final String password;

    public Credentials(String username, String password) {
      this.username = username;
      this.password = password;
    }
  }

  private static class Repository {
    public final URL url;
    public final Credentials credentials;

    public Repository(URL url, Credentials credentials) {
      this.url = url;
      this.credentials = credentials;
    }
  }

  public void resolve(String[] versions) throws Exception {
    List<Repository> repositories = null;
    TestProperties properties = new TestProperties();
    for (String version : versions) {
      // refuse to test with SNAPSHOT maven version when build RELEASE plugins
      if (isSnapshot(version) && !isSnapshot(properties.getPluginVersion())) {
        String msg = String.format("Cannot test %s plugin release with %s maven", properties.getPluginVersion(), version);
        error(version, new IllegalStateException(msg));
      }
      File basdir = new File("target/maven-installation").getCanonicalFile();
      File mavenHome = new File(basdir, "apache-maven-" + version).getCanonicalFile();
      if (!mavenHome.isDirectory()) {
        if (repositories == null) {
          repositories = getRepositories(properties);
        }
        Authenticator defaultAuthenticator = getDefaultAuthenticator();
        try {
          createMavenInstallation(repositories, version, properties.getLocalRepository(), basdir);
        } catch (Exception e) {
          error(version, e);
        } finally {
          Authenticator.setDefault(defaultAuthenticator);
        }
      }
      if (mavenHome.isDirectory()) {
        resolved(mavenHome, version);
      }
    }
  }

  private static Authenticator getDefaultAuthenticator() throws ReflectiveOperationException {
    Method getDefault;
    try {
      getDefault = Authenticator.class.getMethod("getDefault");
    } catch (NoSuchMethodException e) {
      // there is no API to query current default Authenticator before JDK 9
      // assume that integration test jvm does not have any at this point
      return null;
    }
    return (Authenticator) getDefault.invoke(null);
  }

  private boolean isSnapshot(String version) {
    return version != null && version.endsWith("-SNAPSHOT");
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

  private List<Repository> getRepositories(TestProperties properties) throws Exception {
    Map<String, Credentials> credentials = getCredentials(properties);
    List<Repository> repositories = new ArrayList<>();
    for (String property : properties.getRepositories()) {
      InputSource is = new InputSource(new StringReader("<repository>" + property + "</repository>"));
      Element repository = (Element) xpathFactory.newXPath() //
          .compile("/repository") //
          .evaluate(is, XPathConstants.NODE);
      String url = getChildValue(repository, "url");
      if (url == null) {
        continue; // malformed test.properties
      }
      if (!url.endsWith("/")) {
        url = url + "/";
      }
      String id = getChildValue(repository, "id");
      repositories.add(new Repository(new URL(url), credentials.get(id)));
    }
    return repositories;
  }

  private Map<String, Credentials> getCredentials(TestProperties properties) throws IOException {
    File userSettings = properties.getUserSettings();
    if (userSettings == null) {
      return Collections.emptyMap();
    }
    Map<String, Credentials> result = new HashMap<>();
    try {
      Document document = documentBuilderFactory.newDocumentBuilder().parse(userSettings);
      NodeList servers = (NodeList) xpathFactory.newXPath() //
          .compile("//settings/servers/server") //
          .evaluate(document, XPathConstants.NODESET);
      for (int i = 0; i < servers.getLength(); i++) {
        Element server = (Element) servers.item(i);
        String id = getChildValue(server, "id");
        String username = getChildValue(server, "username");
        String password = getChildValue(server, "password");
        if (id != null && username != null) {
          result.put(id, new Credentials(username, password));
        }
      }
    } catch (XPathExpressionException | SAXException | ParserConfigurationException e) {
      // can't happen
    }
    return result;
  }

  private String getChildValue(Element server, String name) {
    NodeList children = server.getElementsByTagName(name);
    if (children.getLength() != 1) {
      return null;
    }
    String value = ((Element) children.item(0)).getTextContent();
    if (value != null) {
      value = value.trim();
    }
    return !value.isEmpty() ? value : null;
  }

  private String getXPathString(InputSource is, String path) throws Exception {
    String value = xpathFactory.newXPath().compile(path).evaluate(is);
    if (value == null) {
      return null;
    }
    value = value.trim();
    return !value.isEmpty() ? value : null;
  }

  private void createMavenInstallation(List<Repository> repositories, String version, File localrepo, File targetdir) throws Exception {
    String versionDir = "org/apache/maven/apache-maven/" + version + "/";
    String filename = "apache-maven-" + version + "-bin.tar.gz";
    File archive = new File(localrepo, versionDir + filename);
    if (archive.canRead()) {
      unarchive(archive, targetdir);
      return;
    }
    Exception cause = null;
    for (Repository repository : repositories) {
      setHttpCredentials(repository.credentials);
      String effectiveVersion;
      if (isSnapshot(version)) {
        try {
          effectiveVersion = getQualifiedVersion(repository.url, versionDir);
        } catch (FileNotFoundException e) {
          continue;
        } catch (IOException e) {
          cause = e;
          continue;
        }
        if (effectiveVersion == null) {
          continue;
        }
      } else {
        effectiveVersion = version;
      }
      filename = "apache-maven-" + effectiveVersion + "-bin.tar.gz";
      archive = new File(localrepo, versionDir + filename);
      if (archive.canRead()) {
        unarchive(archive, targetdir);
        return;
      }
      URL resource = new URL(repository.url, versionDir + filename);
      try (InputStream is = openStream(resource)) {
        archive.getParentFile().mkdirs();
        File tmpfile = File.createTempFile(filename, ".tmp", archive.getParentFile());
        try {
          copy(is, tmpfile);
          unarchive(tmpfile, targetdir);
          Files.move(tmpfile.toPath(), archive.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally {
          tmpfile.delete();
        }
        return;
      } catch (FileNotFoundException e) {
        // ignore the exception. this is expected to happen quite often and not a failure by iteself
      } catch (IOException e) {
        cause = e;
      }
    }
    Exception exception = new FileNotFoundException("Could not download maven version " + version + " from any configured repository");
    exception.initCause(cause);
    throw exception;
  }

  private void setHttpCredentials(final Credentials credentials) {
    Authenticator authenticator = null;
    if (credentials != null) {
      authenticator = new Authenticator() {
        @Override
        protected PasswordAuthentication getPasswordAuthentication() {
          return new PasswordAuthentication(credentials.username, credentials.password.toCharArray());
        }
      };
    }
    Authenticator.setDefault(authenticator);
  }

  private String getQualifiedVersion(URL repository, String versionDir) throws Exception {
    URL resource = new URL(repository, versionDir + "maven-metadata.xml");
    try (InputStream is = openStream(resource)) {
      ByteArrayOutputStream buf = new ByteArrayOutputStream();
      copy(is, buf);
      InputSource xml = new InputSource(new ByteArrayInputStream(buf.toByteArray()));
      String version = getXPathString(xml, "//metadata/versioning/snapshotVersions/snapshotVersion[extension='tar.gz']/value");
      if (version == null) {
        return null;
      }
      version = version.trim();
      return version.isEmpty() ? null : version;
    }
  }

  private InputStream openStream(URL resource) throws IOException {
    URLConnection connection = resource.openConnection();
    if (connection instanceof HttpURLConnection) {
      // for some reason, nexus version 2.11.1-01 returns partial maven-metadata.xml
      // unless request User-Agent header is set to non-default value
      connection.addRequestProperty("User-Agent", "takari-plugin-testing");
      int responseCode = ((HttpURLConnection) connection).getResponseCode();
      if (responseCode == 301 | responseCode == 302 || responseCode == 307) {
        return openStream(new URL(connection.getHeaderField("Location")));
      } else if (responseCode < 200 || responseCode > 299) {
        String message = String.format("HTTP/%d %s", responseCode, ((HttpURLConnection) connection).getResponseMessage());
        throw responseCode == HttpURLConnection.HTTP_NOT_FOUND ? new FileNotFoundException(message) : new IOException(message);
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
