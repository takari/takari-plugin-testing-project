/**
 * Copyright (c) 2021 Takari, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package io.takari.maven.testing;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.FileUtils;
import org.junit.Assert;

/**
 * Abstract base class to extract and assert test resources.
 */
public abstract class AbstractTestResources {

  private final String projectsDir;

  private final String workDir;

  private String name;

  AbstractTestResources() {
    this("src/test/projects", "target/test-projects");
  }

  AbstractTestResources(String projectsDir, String workDir) {
    this.projectsDir = projectsDir;
    this.workDir = workDir;
  }

  void starting(Class<?> testClass, String methodName) {
    if (methodName != null) {
      methodName = methodName.replace('/', '_').replace('\\', '_');
    }
    name = testClass.getSimpleName() + "_" + methodName;
  }
  
  abstract String getRequiredAnnotationClassName();

  /**
   * Creates new clean copy of test project directory structure. The copy is named after both the test being executed and test project name, which allows the same test project can be used by multiple
   * tests and by different instances of the same parametrized tests.<br/>
   * TODO Provide alternative working directory naming for Windows, which still limits path names to ~250 charecters
   */
  public File getBasedir(String project) throws IOException {
    if (name == null) {
      throw new IllegalStateException(getClass().getSimpleName() + " must be a test class field annotated with " + getRequiredAnnotationClassName());
    }
    File basedir = new File(workDir, name + "_" + project).getCanonicalFile();
    FileUtils.deleteDirectory(basedir);
    Assert.assertTrue("Test project working directory created", basedir.mkdirs());
    File src = new File(projectsDir, project).getCanonicalFile();
    Assert.assertTrue("Test project directory does not exist: " + src.getPath(), src.isDirectory());
    FileUtils.copyDirectoryStructure(src, basedir);
    return basedir;
  }

  /**
   * Creates new clean test work directory. The directory is named after test being executed.
   * 
   * @since 2.2
   */
  public File getBasedir() throws IOException {
    if (name == null) {
      throw new IllegalStateException(getClass().getSimpleName() + " must be a test class field annotated with " + getRequiredAnnotationClassName());
    }
    File basedir = new File(workDir, name).getCanonicalFile();
    FileUtils.deleteDirectory(basedir);
    Assert.assertTrue("Test project working directory created", basedir.mkdirs());
    return basedir;
  }

  // static helpers

  public static void cp(File basedir, String from, String to) throws IOException {
    // TODO ensure destination lastModified timestamp changes
    FileUtils.copyFile(new File(basedir, from), new File(basedir, to));
  }

  public static void assertFileContents(File basedir, String expectedPath, String actualPath) throws IOException {
    String expected = fileRead(new File(basedir, expectedPath), true);
    String actual = fileRead(new File(basedir, actualPath), true);
    Assert.assertEquals(expected, actual);
  }

  private static String fileRead(File file, boolean normalizeEOL) throws IOException {
    StringBuilder sb = new StringBuilder();
    try (BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(file)))) {
      if (normalizeEOL) {
        String str;
        while ((str = r.readLine()) != null) {
          sb.append(str).append('\n');
        }
      } else {
        int ch;
        while ((ch = r.read()) != -1) {
          sb.append((char) ch);
        }
      }
    }
    return sb.toString();
  }

  public static void assertFileContents(String expectedContents, File basedir, String path) throws IOException {
    String actualContents = fileRead(new File(basedir, path), true);
    Assert.assertEquals(expectedContents, actualContents);
  }

  public static void assertDirectoryContents(File dir, String... expectedPaths) {
    DirectoryScanner scanner = new DirectoryScanner();
    scanner.setBasedir(dir);
    scanner.addDefaultExcludes();
    scanner.scan();

    Set<String> actual = new TreeSet<String>();
    for (String path : scanner.getIncludedFiles()) {
      actual.add(path.replace(File.separatorChar, '/'));
    }
    for (String path : scanner.getIncludedDirectories()) {
      if (path.length() > 0) {
        actual.add(path.replace(File.separatorChar, '/') + "/");
      }
    }

    Set<String> expected = new TreeSet<String>();
    if (expectedPaths != null) {
      for (String path : expectedPaths) {
        expected.add(path.replace(File.separatorChar, '/'));
      }
    }

    // compare textual representation to make diff easier to understand
    Assert.assertEquals(toString(expected), toString(actual));
  }

  private static String toString(Collection<String> strings) {
    StringBuilder sb = new StringBuilder();
    for (String string : strings) {
      sb.append(string).append('\n');
    }
    return sb.toString();
  }

  public static void touch(File basedir, String path) throws InterruptedException {
    touch(new File(basedir, path));
  }

  public static void touch(File file) throws InterruptedException {
    if (!file.isFile()) {
      throw new IllegalArgumentException("Not a file " + file);
    }
    long lastModified = file.lastModified();
    file.setLastModified(System.currentTimeMillis());

    // TODO do modern filesystems still have this silly lastModified resolution?
    if (lastModified == file.lastModified()) {
      Thread.sleep(1000L);
      file.setLastModified(System.currentTimeMillis());
    }
  }

  public static void rm(File basedir, String path) {
    Assert.assertTrue("delete " + path, new File(basedir, path).delete());
  }

  public static void create(File basedir, String... paths) throws IOException {
    if (paths == null || paths.length == 0) {
      throw new IllegalArgumentException();
    }
    for (String path : paths) {
      File file = new File(basedir, path);
      file.getParentFile().mkdirs();
      Assert.assertTrue(file.getParentFile().isDirectory());
      file.createNewFile();
      Assert.assertTrue(file.isFile() && file.canRead());
    }
  }

  public static void assertFilesPresent(File basedir, String... paths) {
    if (basedir == null || paths == null || paths.length <= 0) {
      throw new IllegalArgumentException();
    }
    if (paths.length == 1) {
      Assert.assertTrue(paths[0] + " PRESENT", new File(basedir, paths[0]).isFile());
    } else {
      StringBuilder expected = new StringBuilder();
      StringBuilder actual = new StringBuilder();
      for (String path : paths) {
        expected.append(path).append("\n");
        if (!new File(basedir, path).isFile()) {
          actual.append("NOT PRESENT ");
        }
        actual.append(path).append("\n");
      }
      Assert.assertEquals(expected.toString(), actual.toString());
    }
  }

  public static void assertFilesNotPresent(File basedir, String... paths) {
    if (basedir == null || paths == null || paths.length <= 0) {
      throw new IllegalArgumentException();
    }
    if (paths.length == 1) {
      Assert.assertFalse(paths[0] + " NOT PRESENT", new File(basedir, paths[0]).isFile());
    } else {
      StringBuilder expected = new StringBuilder();
      StringBuilder actual = new StringBuilder();
      for (String path : paths) {
        expected.append(path).append("\n");
        if (new File(basedir, path).isFile()) {
          actual.append("PRESENT ");
        }
        actual.append(path).append("\n");
      }
      Assert.assertEquals(expected.toString(), actual.toString());
    }
  }

  /**
   * @since 2.2
   */
  public static Map<String, String> readProperties(File basedir, String path) throws IOException {
    Properties properties = new Properties();
    try (InputStream is = new FileInputStream(new File(basedir, path))) {
      properties.load(is);
    }
    Map<String, String> result = new HashMap<>();
    for (String key : properties.stringPropertyNames()) {
      result.put(key, properties.getProperty(key));
    }
    return Collections.unmodifiableMap(result);
  }

}
