# social-wallet

A generic social wallet UI which uses the social-wallet-api for a beckend

<a href="https://www.dyne.org"><img
src="https://zenroom.dyne.org/img/software_by_dyne.png"
alt="software by Dyne.org"
title="software by Dyne.org" class="pull-right"></a>

[Getting started](#Getting-Started) | [Prerequisites](#Prerequisites) | [Running](#Running) | [Running the tests](#Running-the-tests) | [Deployment](#Deployment) | [Todos](#Todos) | [Acknowledgements](#Acknowledgements) | [License](#License) | [change log](https://github.com/dyne/social-wallet/blob/master/CHANGELOG.markdown) 

[![Build Status](https://travis-ci.org/Commonfare-net/social-wallet.svg?branch=master)](https://travis-ci.org/Commonfare-net/social-wallet)
[![Clojars Project](https://img.shields.io/clojars/v/social-wallet.svg)](https://clojars.org/social-wallet)
[![License: AGPL v3](https://img.shields.io/badge/License-AGPL%20v3-blue.svg)](https://www.gnu.org/licenses/agpl-3.0)

## Getting Started

FIXME

### Prerequisites

<img class="pull-left" src="https://secrets.dyne.org/static/img/leiningen.jpg"
style="padding-right: 1.5em">

Please install
1. A JDK. The software is tested on [openJDK](http://openjdk.java.net/) versions 7 and 8 as well as with [oracleJDK 8](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html) and 10. Make sure that the env var JAVA_HOME is set to the JDK install dir like [mentioned here](https://docs.oracle.com/cd/E19182-01/820-7851/inst_cli_jdk_javahome_t/index.html).
2. [MongoDB community edition](https://docs.mongodb.com/manual/administration/install-community/). The software has been tested on Mongo v3.6.4. Earlier versions might not work due to loss of precision (Decimal128 was not introduced).
3. [leiningen](https://leiningen.org/) which is used for dependency management like:
```
mkdir ~/bin
wget https://raw.githubusercontent.com/technomancy/leiningen/stable/bin/lein -O ~/bin/lein
chmod +x ~/bin/lein
```

### Running 

First of all check the configuration in
`resources/social-wallet.yaml` and adjust its contents to your
setup. Here a sample configuration:

FIXME

Once correctly configured, from inside the social-wallet-api source
directory one can use various commands to run it live (refreshing live
changes to the code) using:

- `lein ring server` (which will start and spawn a browser on it)
- `lein ring server-headless` (will start without browser)

One can also use `lein uberjar` to build a standalone jar application,
or `lein uberwar` to build a standalone war application ready to be
served from enterprise infrastructure using JBoss or Tomcat.

## Running the tests

To run all tests one need to run
` lein midje`
on the project dir


## Deployment

FIXME

## Todos

FIXME

## Acknowledgements

FIXME

## License

This project is licensed under the AGPL 3 License - see the [LICENSE](LICENSE) file for details

#### Additional permission under GNU AGPL version 3 section 7.

If you modify social-wallet, or any covered work, by linking or combining it with any library (or a modified version of that library), containing parts covered by the terms of EPL v 1.0, the licensors of this Program grant you additional permission to convey the resulting work. Your modified version must prominently offer all users interacting with it remotely through a computer network (if your version supports such interaction) an opportunity to receive the Corresponding Source of your version by providing access to the Corresponding Source from a network server at no charge, through some standard or customary means of facilitating copying of software. Corresponding Source for a non-source form of such a combination shall include the source code for the parts of the libraries (dependencies) covered by the terms of EPL v 1.0 used as well as that of the covered work.


