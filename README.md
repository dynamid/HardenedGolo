[![DOI](https://zenodo.org/badge/doi/10.5281/zenodo.16110.svg)](http://dx.doi.org/10.5281/zenodo.16110)
[![Gitter](https://badges.gitter.im/Join Chat.svg)](https://gitter.im/eclipse/golo-lang?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
[![Build Status](https://travis-ci.org/eclipse/golo-lang.svg?branch=master)](https://travis-ci.org/eclipse/golo-lang)
[ ![Download](https://api.bintray.com/packages/golo-lang/downloads/distributions/images/download.svg) ](https://bintray.com/golo-lang/downloads/distributions/_latestVersion)

# Hardened Golo: Have more confidence into your Golo.

> The world didn't need another JVM language.
> So we built yet another one.  A simple one.

Hardened Golo is fork of [Golo](https://github.com/eclipse/golo-lang) adding some static analysis in order to gain confidence into a Golo program.
Since Golo is a dynamic, weakly-typed language for the JVM, Hardened Golo do not support the whole language.

Hardened Golo is developed as part of the research activities of the
[DynaMid](http://dynamid.citi-lab.fr/) group of the
[CITI Laboratory](http://www.citi-lab.fr/) at
[INSA-Lyon](http://www.insa-lyon.fr/).


## Links

* Hardened Golo GitHub: [https://github.com/nstouls/HardenedGolo](https://github.com/nstouls/HardenedGolo)
* Hardened Golo Wiki: [https://github.com/nstouls/HardenedGolo/wiki](https://github.com/nstouls/HardenedGolo/wiki)

* Golo Website: [http://golo-lang.org/](http://golo-lang.org/)
* Golo GitHub: [https://github.com/eclipse/golo-lang](https://github.com/eclipse/golo-lang)

## Building HardenedGolo

### Dependencies

#### Hardened Golo

Hardened Golo is built with [Gradle](https://gradle.org).
Since the source code contains the [Gradle wrapper scripts](https://docs.gradle.org/current/userguide/gradle_wrapper.html),
the build can bootstrap itself by downloading the qualified Gradle version from the Internet.

Hardened Golo needs Java SE 8 or more to build and run.

#### Why and provers

Hardened Golo produces some WhyML files, that have to be verified with [Why3](http://why3.lri.fr/) tool. This tool could be easily installed on Linux and OSX Systems. You will next need to install some external provers.

### Building Golo

Common tasks:

* build: `./gradlew build`
* test: `./gradlew test`
* clean: `./gradlew clean`
* documentation: `./gradlew asciidoctor golodoc javadoc`
* assemble a working distribution in `build/install`: `./gradlew installDist`
* generate a nice JaCoCo tests coverage report: `./gradlew jacocoTestReport`

The complete list of tasks is available by running `./gradlew tasks`.

### IDE support

#### Eclipse

You should use the [buildship plugin](https://projects.eclipse.org/projects/tools.buildship).

Note that you may have to manually adjust the Java source paths to include `build/generated/javacc`
and `build/generated/jjtree`.

#### Netbeans

Netbeans has
[a recommended community-supported Gradle plugin](https://github.com/kelemen/netbeans-gradle-project).

It works with no required manual adjustment on the Golo code base in our tests.

#### IntelliJ IDEA

Gradle support is native in IntelliJ IDEA.

Note that you may have to adjust the module settings to:

1. remove `build` from the excluded folders, and
2. add both `build/generated/javacc` and `build/generated/jjtree` as source folders, and
3. exclude other folders in `build` to reduce completion scopes.

### Special build profiles

#### Bootstrap mode

Working on the compiler may cause your build to fail because proper compilation and bytecode
generation doesn't work. In such cases the `goloc` task is likely to fail, and a wide range of unit tests
will break because some Golo source files won't have been compiled.

You can activate the bootstrap mode for that, and focus solely on the Java parts:

    ./gradlew test -P bootstrap

#### Tests console output

By default Gradle redirects all tests console outputs, and makes them available from the HTML report
found in `build/reports/tests/index.html`.

You can instead opt to have all console outputs:

    ./gradlew test -P consoleTraceTests

#### Verbose tests

It is often desirable to get more outputs from tests, like dumps of intermediate representation
trees or generated JVM bytecode.

Such verbosity can be activated using:

    ./gradlew test -P traceTests

Of course you can combine profiles, like:

    ./gradlew test -P traceTests -P consoleTraceTests -P bootstrap

## License

    Copyright (c) 2012-2016 Institut National des Sciences Appliquées de Lyon (INSA-Lyon) and contributors

    All rights reserved. This program and the accompanying materials
    are made available under the terms of the Eclipse Public License v1.0
    which accompanies this distribution, and is available at
    http://www.eclipse.org/legal/epl-v10.html

## Contributing

We welcome contributions from the community!

Check the `CONTRIBUTING.md` file for instructions.
