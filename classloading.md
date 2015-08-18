Unit test classloading
============
Unit test harness (maven surefire, to be more precise) uses the same 
`scope=test` classpath to load Maven core, plugin and plugin test classes. 
This is different from normal Maven invocation, where each plugin is loaded
in a separate classloader with access to only a subset of Maven core classes
and dependencies.

This implies that version of common dependencies, Guava in particular, used 
during unit tests must be compatible with both Maven core and the plugin being
tested. In practical terms, this means that version of Guava used by a plugin
must match version of Maven the plugin depends on during the build. 
For example, if a plugin depends on Maven `3.3.3`, the plugin must use 
Guava `18.0` to be able to use unit test harness. Consider writing integration
tests if the plugin requires incompatible versions of Guava and Maven core.

Integration test classloading
============

As during normal Maven invocation, Maven core classes and each plugin are 
loaded into separate classloaders during integration tests. Only limited number
of Mavencore classes are visible to plugin classloaders, see 
[Maven classloading](http://takari.io/book/91-maven-classloading.html) for more 
details.

There are two ways to run integration tests. By default, integration tests are 
executed in separate classloader(s) within the same test jvm, a.k.a. "embedded"
integration test execution. This allows debugger breakpoints anywhere in Maven,
plugin or test code. All embedded integration test executions share jvm system
and extension classloaders, which may cause problem when testing with 
Maven `3.2.5` and earlier (see below). There are no known problems with 
embedded integration test execution when using Maven `3.3.1` and newer.

Integration tests can also be executed in a forked jvm, i.e. exactly the same 
way Maven is executed during normal command-line invocation. Forked execution 
provides "perfect" classloading environment, but it does not support end-to-end
debugging and is noticeably slower compared to embedded execution.

    @RunWith(MavenJUnitTestRunner.class)
    @MavenVersions({"3.0.5", "3.2.5"})
    public class PluginIntegrationTest {
    
      public final MavenRuntime maven;
    
      public PluginIntegrationTest(MavenRuntimeBuilder mavenBuilder) {
        this.maven = mavenBuilder.forkedBuilder().build();
      }
    
      ...    
    }

-----

## Test dependencies "leak" into integration test runtime (Maven 3.2.5 and older)

This problem affects integration tests under the following circumstances

* The integration test uses Maven `3.2.5` or older
* The integration test is collocated with the plugin and/or unit tests in the 
  Maven project. Or, more precisely, Maven core artifacts are present on 
  integration test classpath
* The plugin uses `ClassLoader#loadClass` to provide different behaviour 
  depending on version of Maven used. Only `maven-dependency-tree` is known to
  use this technique.

Maven `3.2.5` and older uses jvm system classloader is the parent of both 
plugin and project extensions class realms. This results in duplicate maven 
core classes visible to the plugins during integration tests. Here is little
ascii-art diagram to illustrate the problem

                       +------------------------+
                       | extensions classloader |
                       +------------------------+
                            /              \
                           /                \
       +-------------------------+    +-----------------------------+
       |    system classloader   |    | integration test maven core |
       |   unit test maven core  |    |                             |
       +-------------------------+    +-----------------------------+
                             \            /
                     (parent) \          / (import)
                            +--------------+
                            | plugin realm |
                            +--------------+

The reason this works for most integration tests, is because ClassRealm 
implementaion uses imported classes before it delegates to parent classloader,
so expected integration test maven core classes are used.

The duplicate maven-core classes cause problems for maven plugins that use
`ClassLoader#loadClass` and access unexpected maven core classses from system
classloader. For example, `maven-dependency-tree` uses presence of 
`org.eclipse.aether.*` classes as indication of maven 3.1+ and misbehaves
for projects that are compiled with maven 3.1+ but integration tested with
maven 3.0.x.

## Workarounds

### Separate integration test project module (recommended)

It should be possible to setup integration test project module such that it
does not have maven core dependencies. Although test dependencies will still
be visible to plugin and project extension realms, these dependencies most
likely won't cause problems.

### Forked integration test maven runtime

Forked maven runtime provides "perfect" classpath but it is significantly 
slower and does not support end-to-end debugging from m2e workspace.
