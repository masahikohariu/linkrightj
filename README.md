[![Build Status](https://travis-ci.org/bitcoinj/bitcoinj.png?branch=master)](https://travis-ci.org/bitcoinj/bitcoinj)   [![Coverage Status](https://coveralls.io/repos/bitcoinj/bitcoinj/badge.png?branch=master)](https://coveralls.io/r/bitcoinj/bitcoinj?branch=master) 

[![Visit our IRC channel](https://kiwiirc.com/buttons/irc.freenode.net/bitcoinj.png)](https://kiwiirc.com/client/irc.freenode.net/bitcoinj)

### Welcome to bitcoinj

The bitcoinj library is a Java implementation of the Bitcoin protocol, which allows it to maintain a wallet and send/receive transactions without needing a local copy of Bitcoin Core. It comes with full documentation and some example apps showing how to use it.

### Technologies

* Java 7 for the core modules, Java 8 for everything else
* [Maven 3+](http://maven.apache.org) - for building the project
* [Google Protocol Buffers](https://github.com/google/protobuf) - for use with serialization and hardware communications

### Getting started

To get started, it is best to have the latest JDK and Maven installed. The HEAD of the `master` branch contains the latest development code and various production releases are provided on feature branches.

#### Building from the command line

To perform a full build use
```
mvn clean package
```
You can also run
```
mvn site:site
```
to generate a website with useful information like JavaDocs.

The outputs are under the `target` directory.

#### Building from an IDE

Alternatively, just import the project using your IDE. [IntelliJ](http://www.jetbrains.com/idea/download/) has Maven integration built-in and has a free Community Edition. Simply use `File | Import Project` and locate the `pom.xml` in the root of the cloned project source tree.

### Example applications

These are found in the `examples` module.

### Where next?

Now you are ready to [follow the tutorial](https://bitcoinj.github.io/getting-started).

