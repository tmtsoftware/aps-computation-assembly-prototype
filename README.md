# computationPrototype

This project implements an HCD (Hardware Control Daemon) and an Assembly using
TMT Common Software ([CSW](https://github.com/tmtsoftware/csw)) APIs.

## Subprojects

* aps-computationprototypeassembly - an assembly that talks to the computationPrototype HCD
* aps-computationprototypehcd - an HCD that talks to the computationPrototype hardware
* aps-computationprototypedeploy - for starting/deploying HCDs and assemblies

## Upgrading CSW Version

`project/build.properties` file contains `csw.version` property which indicates CSW version number.
Updating `csw.version` property will make sure that CSW services as well as library dependency for HCD and Assembly modules are using same CSW version.

## Build Instructions

The build is based on sbt and depends on libraries generated from the
[csw](https://github.com/tmtsoftware/csw) project.

See [here](https://www.scala-sbt.org/1.0/docs/Setup.html) for instructions on installing sbt.

## CSW Prerequisites for running Components

The CSW services need to be running before starting the components.
   This is done by starting the `csw-services`.
   If you are not building csw from the sources, you can run `csw-services` as follows:

- Install `coursier` using steps described [here](https://tmtsoftware.github.io/csw/apps/csinstallation.html) and add TMT channel.
- Run `cs install csw-services`. This will create an executable file named `csw-services` in the default installation directory.
- Run `csw-services start` command to start all the CSW services i.e. Location, Config, Event, Alarm and Database Service
- Run `csw-services --help` to get more information.

Note: while running the csw-services use the csw version from `project/build.properties`

## Changes to add a the Algorithms Library Jar to build
Changes to Dependencies.scala, use the following block:
```
val Computationprototypeassembly = Seq(
CSW.`csw-framework`,
CSW.`csw-testkit` % Test,
Libs.`scalatest` % Test,
Libs.`junit4-interface` % Test,
Libs.`testng-6-7` % Test,
"org.tmt.aps.peas" % "algorithm-lib" % "1.0.0"
)
```
Changes in Common.scala, update the propertiesSettings by adding
mavelLocal resolver and adding /opt/apps/lib to the java.library.path

```
    resolvers += "jitpack" at "https://jitpack.io",
    resolvers += Resolver.mavenLocal,
    version := "0.1.0",
    fork := true,
    javaOptions += "-Djava.library.path=/opt/apps/lib",

```
Setting the javaOptions allows the JVM to find the shared library
assuming library is in /opt/apps/lib

## Running the Assembly

Run the container cmd script with arguments. For example:

* Run the Assembly in a standalone mode with a local config file (The standalone config format is different than the container format):

```
sbt "aps-computationprototypedeploy/runMain aps.computationprototypedeploy.ComputationprototypeContainerCmdApp --local ./src/main/resources/JComputationprototypeassemblyStandalone.conf"
```

