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
The module is build only when `-DbuildSctp` switch is passed to Maven.
`mvn package -DbuildSctp -f pom.xml -pl org.jitsi:usrsctp` will create a jar that will include the native library and the necessary include headers for current platform.
A resulting `jar` artifact has [maven classifier](https://maven.apache.org/pom.html) specified as a concatenation of `usrsctp` commit and target platform.
For example, an artifact for `Linux` might be named as `usrsctp-1.0-SNAPSHOT-7a8bc9a-linux.jar` with an example content:
```text
$ tree usrsctp-1.0-SNAPSHOT-7a8bc9a-linux --noreport
usrsctp-1.0-SNAPSHOT-7a8bc9a-linux
|-- META-INF
|   |-- MANIFEST.MF
|   `-- maven
|       `-- org.jitsi
|           `-- usrsctp
|               `-- pom.xml
|-- git.properties
|-- include
|   `-- usrsctp.h
`-- lib
    `-- libusrsctp.a
```

* The `jniwrapper` module has 3 nested modules:
  * The `jniwrapper-java` module includes the Java portion of the JNI API and interacts with the native code.
    The artifact produced by this module has Java classes to interact with native `usrsctp` wrapper, it also has
    necessary `JNI` `C` headers, generated from Java classes. An example of artifact content:
    ```text
    $ tree jniwrapper-java-1.0-SNAPSHOT --noreport
    jniwrapper-java-1.0-SNAPSHOT
    |-- META-INF
    |   |-- MANIFEST.MF
    |   `-- maven
    |       `-- org.jitsi
    |           `-- jniwrapper-java
    |               |-- pom.properties
    |               `-- pom.xml
    |-- cz
    |   `-- adamh
    |       `-- utils
    |           `-- NativeUtils.class
    |-- native
    |   `-- headers
    |       `-- org_jitsi_modified_sctp4j_SctpJni.h
    `-- org
        `-- jitsi_modified
            `-- sctp4j
                |-- EightArgumentVoidFunc.class
                |-- FourArgumentIntFunc.class
                |-- IncomingSctpDataHandler.class
                |-- OutgoingSctpDataHandler.class
                `-- SctpJni.class
    ```
  * The `jniwrapper-native` module contains the `C` portion of the JNI API that bridges the Java and the [`usrsctp`](https://github.com/sctplab/usrsctp) native lib.
  The module is build only when `-DbuildJniSctp` switch is passed to Maven.
  It depends on two other modules:
    * `usrsctp` module, because it needs the pre-built `usrsctp` static library and include headers;
    * `jniwrapper-java` module, because it need `java -h` generated `JNI` header file to match the `C` implementation.

    The `compile` build phase of `jniwrapper-native` module will create a new dynamic jni lib in `target/jnisctp/install/lib`. E.g. on `Linux` it produces dynamic library `target/jnisctp/install/lib/linux/libjnisctp.so`.

    The `package` build phase of `jniwrapper-native` module will create a platform specific `jar` that includes the java code and the shared native library for current platform. It is assumed that `usrsctp` artifact for target platform is already available in `Maven`.
    Below is an example of artifact produced by `mvn package -DbuildSctp -DbuildJniSctp -f pom.xml -pl org.jitsi:jniwrapper-native -am` is presented:
      ```text
      $ tree jniwrapper-native-1.0-SNAPSHOT-linux --noreport
      jniwrapper-native-1.0-SNAPSHOT-linux
      |-- META-INF
      |   |-- MANIFEST.MF
      |   `-- maven
      |       `-- org.jitsi
      |           `-- jniwrapper-native
      |               `-- pom.xml
      `-- lib
          `-- linux
              `-- libjnisctp.so

      ```
  **Note:** It is intended that platform specific `Maven` arficats produced by `usrsctp` and `jniwrapper-native` modules are built on each supported platform independently and published ahead of time to [Maven repository](https://github.com/jitsi/jitsi-maven-repository/) before the rest of the artifacts can be built in way which allow them to be used on any of supported platform.

  * The `jnilib` maven module combines the `jniwrapper-java` and `jniwrapper-native` into a single `jar` which includes the Java API and the native JNI library that will be loaded at runtime.
  When built with `mvn package -f pom.xml -DbuildSctp -DbuildJniSctp -pl org.jitsi:jnilib -am` the `jnilib` artifact only include native `jnisctp` library for current platform.
  To have universal (**fat jar**) `jnilib` suitable to run on any supported platform it necessary to build and publish platform-specific `jniwrapper-native` artifacts for all supported platforms in advance and then pass `-DbuildXPlatJar` switch into Maven. For example, fat `jnilib` jar could be built with `mvn package -DbuildXPlatJar -f pom.xml -pl org.jitsi:jnilib`, which will produce fat jar with example content:
    ```
    $ tree jnilib-1.0-SNAPSHOT --noreport
    jnilib-1.0-SNAPSHOT
    |-- META-INF
    |   |-- MANIFEST.MF
    |   `-- maven
    |       `-- org.jitsi
    |           |-- jnilib
    |           |   `-- pom.xml
    |           |-- jniwrapper-java
    |           |   |-- pom.properties
    |           |   `-- pom.xml
    |           `-- jniwrapper-native
    |               `-- pom.xml
    |-- cz
    |   `-- adamh
    |       `-- utils
    |           `-- NativeUtils.class
    |-- lib
    |   |-- darwin
    |   |   `-- libjnisctp.jnilib
    |   |-- linux
    |   |   `-- libjnisctp.so
    |   `-- windows
    |       |-- jnisctp.dll
    |       `-- jnisctp.pdb
    |-- native
    |   `-- headers
    |       `-- org_jitsi_modified_sctp4j_SctpJni.h
    `-- org
        `-- jitsi_modified
            `-- sctp4j
                |-- EightArgumentVoidFunc.class
                |-- FourArgumentIntFunc.class
                |-- IncomingSctpDataHandler.class
                |-- OutgoingSctpDataHandler.class
                `-- SctpJni.class
    ```


* The `sctp` module contains the Java library on top of the JNI code. 
The jar built by this is what is intended to be used by other code.

### Building the jar files
* Clone the project and initialize [`usrsctp`](https://github.com/sctplab/usrsctp) git submodule.
* Run `mvn package -f pom.xml` (and `mvn install -f pom.xml` to install locally). It assumes that platform specific artifacts like `usrsctp` and `jniwrapper-native` are already available in Maven. To compile and install all artifacts locally run `mvn install -DbuildSctp -DbuildJniSctp -f pom.xml`, this will build native and `Java` code for current platform and install resulting artifacts to local repository.
* Depend on the `org.jitsi:sctp` artifact to use `jitsi-sctp` in your project.

### (Re)Building a new JNI lib
The JNI lib will need to be rebuilt if there is a change in the [`usrsctp`](https://github.com/sctplab/usrsctp) version or a change in the JNI wrapper `C` file.
Changes in [`usrsctp`](https://github.com/sctplab/usrsctp) handled by re-compiling `usrsctp` artifact from corresponding Maven module.
Changes in JNI wrapper `C` code are handled by recompiling `jniwrapper-native` artifact from corresponding maven module.
To re-build native libraries cross-platform [`CMake`](https://cmake.org/) build tool, `C` compiler and linker and `JDK` must be installed on system used to build.

The following steps can be done to produce an updated version of `jitsi-sctp` artifact with newver version of `usrsctp` or `jniwrapper-native`:

1. Clone the project with `git clone --recurse-submodules <jitsi-sctp-git-url>`.
    ```
    > git clone --recurse-submodules https://github.com/jitsi/jitsi-sctp.git jitsi-sctp
    ```

2. \[Optional\] initialize the [usrsctp](https://github.com/sctplab/usrsctp) submodule with `git submodule update --init --recursive`:
    ```
    jitsi-sctp/usrsctp/usrsctp>
    (check out whatever hash/version you want in case it distinct from what is defined by git submodule)
    ```

3. Produce an updated platform specific `usrsctp` artifact 
    ```
    jitsi-sctp> mvn clean package install -DbuildSctp -f pom.xml -pl org.jitsi:usrsctp
    ```

4. \[Optional\] Update `<usrsctp_commit_id>` property in `jniwrapper/pom.xml` to specify desired version of `usrsctp` to use.

5. Produce an updated platform specific `jniwrapper-native` artifact and publish it to Maven.
    ```
    jitsi-sctp> mvn clean package install -DbuildJniSctp -f pom.xml -pl org.jitsi:jniwrapper-native 
    ```

6. \[Optional\] Repeat steps `1 - 5` on each of supported platforms: `Linux`, `Mac OSX`, `Windows`.

7. Once `usrsctp` and `jniwrapper-native` artifacts built and published to [Maven repository](https://github.com/jitsi/jitsi-maven-repository/) for each supported platform (`Windows`, `Linux`, `Mac`) with steps `1 - 6`, an updated **fat jar** could be build and installed with following command:
    ```
    jitsi-sctp> mvn clean package install -DbuildXPlatJar -f pom.xml
    ```

8. To produce `jitsi-sctp` artifact usable only on current platform steps `3 - 7` can be skipped and following command could be used instead:
    ```
    jitsi-sctp> mvn clean package install -DbuildSctp -DbuildJniSctp -f pom.xml
    ```
