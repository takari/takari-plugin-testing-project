Reactor propagation
===================

Reactor propagation is a concept (and corresponding implementation) that allows
Maven build resolve dependencies directly from "outer" reactor build, without
the need to install or deploy outer build artifacts to a shared artifact
repository.

Reactor propagation allows integration build resolve plugin artifact(s) from 
the "outer" plugin build. It also allows Maven resolve dependencies from m2e 
workspace when run/debug "as maven build"


## Integration test execution during maven plugin build

    +--------------------+
    | outer plugin build |
    +--------------------+
           | (forked)
           |
           | +-------------------------+
           \ |       test jvm          |
             |  +-------------------+  |
             |  | integration test  |  |
             |  | build (embedded)  |  |  
             |  +-------------------+  |
             +-------------------------+

Outer build:

* takari-lifecycle-plugin:testProperties
  * puts location of m2e reactor reader artifact, i.e. 
    org.eclipse.m2e.workspace.cli, to the test properties
  * generates reactor state file and puts its location to the test properties
  * writes the test properties to target/test-classes/test.properties
* Surefire forks the test jvm with target/test-classes on classpath

Test jvm:

* pligin test harness
  * it reads reactor state file location from test.properties and adds it as 
    -D parameter to the maven executor (embedded in the ascii-diagram above)
  * it adds reactor reader artifact, i.e. , to the test maven runtime
    classworlds.conf file
* test Maven reads the -D parameter and puts it into System.properties
* reactor reader uses the System.property to locate outer reactor state

## Forked integration test execution

    +--------------------+
    | outer plugin build |
    +--------------------+
           | (forked)
           |
           | +------------+
           \ |  test jvm  |
             +------------+
                   | (forked)
                   |
                   | +-------------------+
                   \ | integration test  |
                     | build (embedded)  |
                     +-------------------+

Not much different from the embedded integration test execution.

## m2e run/debug as junit and maven junit test

    +---------------+
    | m2e workspace |
    +---------------+
           | (forked)
           |
           | +-------------------------+
           \ |       test jvm          |
             |  +-------------------+  |
             |  | integration test  |  |
             |  | build (embedded)  |  |
             |  +-------------------+  |
             +-------------------------+

Workspace artifacts are written to the reactor state file by m2e;
location of the file is passed as -D system property to the test jvm.

> Note that even though takari-lifecycle-plugin:testProperties runs during 
> Eclipse workspace build and creates target/test-classes/test.properties, 
> **it does not generate reactor state file** because there is no reactor
> build. Test harness must use -D System.property to locate reactor state 
> file create by m2e.

Test harness passes location of the reactor state file as -D parameter to
the test maven build, which converts it to System.property, which is then
used by the reactor reader.

## m2e run/debug as maven build

    +---------------+
    | m2e workspace |
    +---------------+
           | (forked)
           |
           | +---------------+
           \ |  maven build  |
             +---------------+

Workspace artifacts are written to the reactor state file by m2e;
location of the file is passed as -D system property to the test jvm.

## Recursive integration test execution

Reactor propagation can be applied recursively, when the "inner" build executes
inner build of its own

    +--------------------+
    | outer plugin build |
    +--------------------+
           | (forked)
           |
           | +-------------------------+
           \ |       test jvm 1        |
             |  +-------------------+  |
             |  | integration test  |  |
             |  | build (embedded)  |  |
             |  +-------------------+  |
             +-------------------------+
                       | (forked)
                       |
                       | +-------------------------+
                       \ |       test jvm 2        |
                         |  +-------------------+  |
                         |  | integration test  |  |
                         |  | build (embedded)  |  |
                         |  +-------------------+  |
                         +-------------------------+

In this case, takari-lifecycle-plugin:testProperties executed in the first 
"inner" integration test build is expected to merge reactor state provided 
by the "outer" build with the inner build reactor. The merged reactors state
is then passed to the second "inner" test build, which makes both the outer 
and the first inner build artifacts resolvable from the second inner build.

Note that Surefire, somewhat unhelpfully, propagates build jvm's 
System.properties to the forked test jvm. This means the test harness must
for reactor state location in test.properties first, otherwise second inner
build will not be able to resolve artifacts from the first inner build.


