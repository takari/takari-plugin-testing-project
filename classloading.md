Integration test classloading
============

## Background

Unit tests expect project main and test classpath dependencies. This is how 
surefire configures test jvm system classpath. This includes maven core
and their transitive dependencies, notable Aether.

Integration tests expect to run with different versions of Maven. Default 
integration test harness loads each test maven version in a separate 
classloader in the same test jvm. These separate classloaders use
jvm extensions classloader (more precisely, parent of the system classloader)
as their parent and do not have access to maven core classes used by unit
tests.

## Test dependencies "leak" into integration test runtime

**Update**: as of 2015-01-08, Maven 3.2.6-SNAPSHOT does not use hardcoded system
classloader as the parent of plugin/extensions realms and is not affected by
the problem described below.

As of version 3.2.5, maven uses jvm system classloader is the parent of both 
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

The reason this works in most cases, is because ClassRealm implementaion uses
imported classes before it delegates to parent classloader, so expected
integration test maven core classes are used.

The duplicate maven-core classes cause problems for maven plugins that use
ClassLoader#loadClass and access unexpected maven core classses from system
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

    @RunWith(MavenJUnitTestRunner.class)
    @MavenVersions({"3.0.5", "3.2.5"})
    public class PluginIntegrationTest {
    
      public final MavenRuntime maven;
    
      public PluginIntegrationTest(MavenRuntimeBuilder mavenBuilder) {
        this.maven = mavenBuilder.forkedBuilder().build();
      }
    
      ...    
    }

