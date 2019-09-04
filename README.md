# jitsi-sctp
The `jitsi-sctp` project creates a JNI wrapper around the [`usrsctp`](https://github.com/sctplab/usrsctp) lib and provides a set of Java classes to further flesh out a convenient Java SCTP API, which can be used on `Linux`, `MacOS X` and `Windows`.

## Project organization
Because JNI has a complex build process multiplied by being platform dependent, this project has multiple Maven modules to try and separate each of the phases necessary from start to finish. The maven modules are laid out as follows:
```
`-- jitsi-sctp
    |-- jniwrapper
    |   |-- java
    |   |-- jnilib
    |   `-- native (produces platform specific artifact)
    |-- sctp
    `-- usrsctp (produces platform specific artifact)
```

* The `usrsctp` module handles the compilation of the [`usrsctp`](https://github.com/sctplab/usrsctp).
This maven module produces platform specific artifact having pre-compiled `usrsctp` static library and corresponding `C` API-header.
`mvn package -DbuildSctp -f usrsctp/pom.xml` will create a jar that will include the native library and the necessary include headers for current platform.
Resulting artifact has target platform set as [maven classifier](https://maven.apache.org/pom.html), e.g. `usrsctp-1.0-SNAPSHOT-windows.jar` or `usrsctp-1.0-SNAPSHOT-linux.jar`

* The `jniwrapper` module has 3 nested modules:
  * The `jniwrapper-java` module includes the Java portion of the JNI API and interacts with the native code.
  * The `jniwrapper-native` module contains the `C` portion of the JNI API that bridges the Java and the [`usrsctp`](https://github.com/sctplab/usrsctp) native lib.
  It depends on both the `usrsctp` module (because it needs the pre-built `usrsctp` static library and include headers) and the `jniwrapper-java` module (to generate the `JNI` header file to match the `C` implementation from the Java file).
  The `compile` build phase will create a new jni lib in `target/jnisctp/install/lib`.
  The `package` build phase will create a platform specific `jar` that includes the java code and the shared native library for current platform.
  It is intended that the JNI libs are built for each platform ahead of time and published ahead of time to `Maven` repository as platform specific artifacts.
  * The `jnilib` maven module combines the `jniwrapper-java` and `jniwrapper-native` into a single `jar` which includes the Java API and the native JNI library that will be loaded at runtime.
  By default `jnilib` only include `jnisctp` native library only for current platform.
  To have universal (**fat jar**) `jnilib` suitable to run on any supported platform it necessary to build and publish platform-specific `jniwrapper-native` artifacts for all supported platforms in advance and then build `jnilib` with `mvn package -DbuildXPlatJar -f jniwrapper/jnilib/pom.xml`.

* The `sctp` module contains the Java library on top of the JNI code. 
The jar built by this is what is intended to be used by other code.

### Building the jar files
* Clone the project and initialize [`usrsctp`](https://github.com/sctplab/usrsctp) git submodule.
* Run `mvn package` (and `mvn install` to install locally)

This will install all the jars built by the project.  Depend on the `org.jitsi:sctp` artifact to use `jitsi-sctp` in your code.

### (Re)Building a new JNI lib
The JNI lib will need to be rebuilt if there is a change in the [`usrsctp`](https://github.com/sctplab/usrsctp) version or a change in the JNI wrapper `C` file.
Chanees in [`usrsctp`](https://github.com/sctplab/usrsctp) handled by re-compiling `usrsctp` artifact from corresponding maven module.
Changes in JNI wrapper `C` code are handled by recompiling `jniwrapper-native` artifact from corresponding maven module.
To re-build native libraries cross-platform [`CMake`](https://cmake.org/) build tool, `C` compiler and linker and `JDK` must be installed on system used to build.

On each supported native platform following commands must be executed to produce platform specific `usrsctp` and `jniwrapper-native` artifacts:

* Clone the project with `git clone --recurse-submodules <jitsi-sctp-git-url>`.
```
> git clone --recurse-submodules https://github.com/jitsi/jitsi-sctp.git jitsi-sctp
```
* [Optional] initialize the [usrsctp](https://github.com/sctplab/usrsctp) submodule with `git submodule update --init --recursive`:
```
jitsi-sctp/usrsctp/usrsctp>
(check out whatever hash/version you want in case it distinct from what is defined by git submodule)
```
* Produce and install new platform specific `usrsctp` and `jniwrapper-native` artifacts 
```
jitsi-sctp> mvn clean package install -DbuildSctp -DbuildJniSctp -f jniwrapper/native/pom.xml
```
* Once `usrsctp` and `jniwrapper-native` artifacts rebuilt and published to [Maven repository](https://github.com/jitsi/jitsi-maven-repository/) for each supported platform (`Windows`, `Linux`, `Mac`) an updated **fat jar** could be build and installed with following command:
```
jitsi-sctp>mvn clean package install -DbuildXPlatJar -f pom.xml
```