# computationPrototype

## Overview
This project implements an Assembly that wraps the APS Algorithm Library using
TMT Common Software ([CSW](https://github.com/tmtsoftware/csw)) APIs.

Commands are handled by worker actors that implement the WorkerCommand interface.  
This prototype implements three commands: ExecuteColorStep, ExecuteTtOffsetsToActs and ExecuteDecomposeActs.
Each is implemented as a worker that handles the named command.

Fortran computations are called from the AlgorithmLibrary class, referenced within each WorkerCommand implementation class.

A deeper discussion of the prototyping effort, including integration with the prototype procedure data service, is described
in [APS-PEAS PROTOTYPING AND RISK REDUCTION REPORT, section 4](https://docushare.tmt.org/docushare/dsweb/ServicesLib/Document-95089/History)



### WorkerCommands
#### Command Metadata
Each WorkerCommand implementation contains metadata about the parameters passed to the Fortran function:
* name - used to load and store values from Configuration or Result singletons
* class
* array shape or scalar indicator
* source - configuration, setup, constant, previous computation result (from service) or passed in command
* direction - input or output parameter

The WorkerCommand implementation classes each prepare the arguments to the AlgorithmLibrary class function call that worker is responsible for, calls the function and stores all output variable to the Result singleton.

Constants are loaded during initialization.  Setup and configuration parameters are loaded using commands: loadSetup, loadConfig.

#### ExecuteColorStep
A colorstep function command that gets inputs from configuration and outputs to procedure data service.
#### ExecuteTtOffsetsToActs
Executes ttOffsetsToActs, using non-command parameter inputs and outputs
#### ExecuteDecomposeActs
Executes decomposeActs using non-command parameter inputs including procedure data service to get inputs that were outputs of ttOffsetsToActs.
####

## Subprojects

* aps-computationprototypeassembly - the computation assembly that wraps the Algorithm Library
* aps-computationprototypehcd - not used
* aps-computationprototypedeploy - for starting assembly
* aps-computationclient - used to send commands to the assembly for testing purposes

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
## Running the Test Client

Run the app using sbt:

```
sbt "aps-computationclient/run"
```

