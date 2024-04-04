# FlowDroid Data Flow Analysis Tool

This repository hosts the FlowDroid data flow analysis tool. FlowDroid statically computes data flows in Android apps and Java programs.
Its goal is to provide researchers and practitioners with a tool and library on which they can base their own research projects and
product implementations. We are happy to see that FlowDroid is now widely used in academia as well as industry.

## Obtaining The Tool

You can either build FlowDroid on your own using Maven, or you can download a release from here on Github.

### Downloading The Release Via Maven

FlowDroid can now be found on <a href="https://mvnrepository.com/artifact/de.fraunhofer.sit.sse.flowdroid">Maven Central</a>.
In order to use FlowDroid in your Maven build, include the following in your ```pom.xml``` file.
We recommend using the latest and greatest version unless you have a specific
issue that prevents you from doing so. In that case, please let us know (see contact below).
```
    <dependencies>
        <dependency>
            <groupId>de.fraunhofer.sit.sse.flowdroid</groupId>
            <artifactId>soot-infoflow</artifactId>
            <version>2.12.0</version>
        </dependency>
        <dependency>
            <groupId>de.fraunhofer.sit.sse.flowdroid</groupId>
            <artifactId>soot-infoflow-summaries</artifactId>
            <version>2.12.0</version>
        </dependency>
        <dependency>
            <groupId>de.fraunhofer.sit.sse.flowdroid</groupId>
            <artifactId>soot-infoflow-android</artifactId>
            <version>2.12.0</version>
        </dependency>
    </dependencies>
```

For a quick start with FlowDroid, look at "Using The Data Flow Tracker" below. If you only want to use the command-line tool,
all you need is the "soot-infoflow-cmd-jar-with-dependencies.jar" file.

### Downloading The Release Via GitHub

The <a href="https://github.com/secure-software-engineering/FlowDroid/releases">Release Page</a> contains all pre-built JAR
files for each release that we officially publish. We recommend using the latest and greatest version unless you have a specific
issue that prevents you from doing so. In that case, please let us know (see contact below).

For a quick start with FlowDroid, look at "Using The Data Flow Tracker" below. If you only want to use the command-line tool,
all you need is the "soot-infoflow-cmd-jar-with-dependencies.jar" file.

### Building The Tool With Maven

Requirements:
* JDK 11 or above
* Maven
* The current snapshot of <a href="https://github.com/soot-oss/soot">Soot</a> installed

At the first time, FlowDroid needs to be built from the parent module, i.e. the project's root folder. The full test
suite takes around 30 minutes, so we recommend to disable the tests when building:
```shell
mvn install -DskipTests
```

To run the build with tests enabled, some additional steps are needed:
* JDK 8 must be installed
* The `rt.jar` must be at the default location (alternatively, place the `rt.jar` inside `$JAVA_HOME/lib/`)
* The DroidBench submodule must be initialized (clone with `--recursive`)
* `ANDROID_JARS` environment variable must be set to the android platforms directory (typically `$HOME/Android/Sdk/platforms/`)

### Building The Tool With Eclipse

We work on FlowDroid using the Eclipse IDE. All modules are Eclipse projects and can be imported into the Eclipse IDE. They will appear as Maven projects there and Eclipse should take care of downloading all required dependencies for you.

## Using The Data Flow Tracker

You can use FlowDroid either through its command-line interface (module soot-infoflow-cmd) or as a library. In general, if you would
like to implement something and need a data flow tracker as a component, you are better off by integrating the FlowDroid modules as
JAR files. If you just need the results quickly, simply run the command-line interface.

FlowDroid is supported on Windows, Mac OS, and Linux.

### Running The Command-Line Tool

If you want to use the command-line tool to run the data flow tracker, you can use the following command:

```
java -jar soot-infoflow-cmd/target/soot-infoflow-cmd-jar-with-dependencies.jar \
    -a <APK File> \
    -p <Android JAR folder> \
    -s <SourcesSinks file>
```

The Android JAR folder is the "platforms" directory inside your Android SDK installation folder. The definition file for sources
and sinks defines what shall be treated as a source of sensitive information and what shall be treated as a sink that can possibly
leak sensitive data to the outside world. These definitions are specific to your use case. However, if you are looking for privacy
issues, you can use our default file "SourcesAndSinks.txt" in the "soot-infoflow-android" folder as a starting point.

For finding out about the other options of the command-line tool, you can run the tool with the "--help" option or have a look at
the MainClass.initializeCommandLineOptions()" method in the source code (module soot-infoflow-cmd).

### Configuring FlowDroid for Performance

For some apps, FlowDroid will take very long for large apps. There are various options with which you can configure a tradeoff between performance, precision and recall.

* ```-ns``` Do not track taints on static fields and disregard static initializers.
* ```-ne``` Do not track exceptional flows.

You can also define timeouts:

* ```-dt N``` Aborts the data flow analysis after N seconds and returns the results obtained so far.
* ```-ct N``` Aborts the callback collection during callgraph construction after N seconds and continues with the (incomplete) callgraph constructed so far.
* ```-rt N``` Aborts the result collection after N seconds and returns the results obtained so far.

Note that timeouts are additive. All three stages must complete or run into a timeout for the tool to return and deliver results.

### Using FlowDroid as a library

If you want to include FlowDroid as a library into your own solution, you can directly reference the respective JAR files. If you
use Maven, you can add FlowDroid as a reference and have Maven resolve all required components. Depending on what you want to analyze
(Android apps or Java programs), your dependencies may vary.

In this section, we will collect code and configuration snippets for common tasks with FlowDroid.

To run a simple data flow analysis, you can use the following code. You need to replace the placeholder ``androidJarFolder`` with the location of the
``platforms`` directory in your Android SDK installation. The placeholder ``apkPath`` refers to the full file path of the APK file. The data flow
results are accessible via the ``InfoflowResults`` class.

```
SetupApplication app = new SetupApplication(androidJarFolder, apkPath);
app.setTaintWrapper(new SummaryTaintWrapper(new LazySummaryProvider("summariesManual")));
InfoflowResults results = app.runInfoflow();
```

The data flow analysis uses the default StubDroid library summaries. In the default configuration, these summaries are stored in
the ``summariesManual`` folder and there is no need to change that.

## Publications

If you want to read the details on how FlowDroid works, <a href="http://tuprints.ulb.tu-darmstadt.de/5937/">the PhD thesis of
Steven Arzt</a> is a good place to start.

## Contributing to FlowDroid

Contributions are always welcome. FlowDroid is an open source project that we published in the hope that it will be useful to
the research community as a whole. If you have a new feature or a bug fix that you would like to see in the official code
repository, please open a merge request here on Github and contact us (see below) with a short description of what you have
done.

## License

FlowDroid is licensed
under the LGPL license, see LICENSE file. This basically means that you are free to use the tool (even in commercial, closed-source
projects). However, if you extend or modify the tool, you must make your changes available under the LGPL as well. This ensures that
we can continue to improve the tool as a community effort.

## Contact

If you experience any issues, you can ask for help on the Soot mailing list. You can also contact us at Steven.Arzt@sit.fraunhofer.de.
