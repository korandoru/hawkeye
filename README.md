# HawkEye

Simple license header checker and formatter, in multiple distribution forms.

## Usage

You can use HawkEye in GitHub Actions or in your local machine.

### GitHub Actions

The HawkEye GitHub Action enables users to run license header check by HawkEye with a config file.

First of all, add a `licenserc.toml` file in the root of your project. The simplest config for projects licensed under Apache License 2.0 is as below:

> **Note** The full configurations can be found in [the configuration section](#configurations).

```toml
inlineHeader = """
Copyright 2023 tison <wander4096@gmail.com>

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
"""
```

You should change the copyright line according to your information.

To check license headers in GitHub Actions, add a step in your GitHub workflow:

```yaml
- name: Check License Header
  uses: korandoru/hawkeye@v1
```

### Docker

[Native Image](https://www.graalvm.org/22.3/reference-manual/native-image/) powered image (28MB):

```shell
docker run -it --rm -v $(pwd):/github/workspace ghcr.io/korandoru/hawkeye-native check --config licenserc.toml
```

[Eclipse Temurin](https://projects.eclipse.org/projects/adoptium.temurin) JRE based image (266MB):

```shell
docker run -it --rm -v $(pwd):/github/workspace ghcr.io/korandoru/hawkeye check --config licenserc.toml
```

### Executable JAR

The executable JAR requires a preinstalled JRE environment (version >= 17).

```shell
export HAWKEYE_VERSION=1.0.0 # replace with expected version
wget https://repo1.maven.org/maven2/io/korandoru/hawkeye/commandline/$HAWKEYE_VERSION/commandline-$HAWKEYE_VERSION-bin.tar.gz
tar -xvzf commandline-$HAWKEYE_VERSION-bin.tar.gz
hawkeye-$HAWKEYE_VERSION/hawkeye check -h
```

## Build

### Executable JAR

This steps requires JDK 17. Higher versions *may* work.

```shell
# Build
./mvnw clean install -DskipTests

# Run
./distribution/commandline/target/hawkeye.jar
```

Build Docker image:

```shell
docker build . -t hawkeye
```

### Native Image

This steps requires GraalVM 22.3.0. Higher versions *may* work.

```shell
# Build with GraalVM
./mvnw clean package -DskipTests -Pnative

# Run
./distribution/native/target/hawkeye-native
```

Build Docker image:

```shell
docker build . -t hawkeye -f Dockerfile.native
```

## Configurations

### Config file

```toml
# Base directory for the whole execution.
# All relative paths is based on this path.
# default: current working directory
baseDir = "."

# Inline header template.
# Either inlineHeader or headerPath should be configured, and inlineHeader is prioritized.
inlineHeader = "..."

# Path to the header template file.
# Either inlineHeader or headerPath should be configured, and inlineHeader is prioritized.
# This path is resolved by the ResourceFinder. Check ResourceFinder for the concrete strategy.
# The header template file is skipped on any execution.
headerPath = "path/to/header.txt"

# On enabled, check the license header matches exactly with whitespace.
# Otherwise, strip the header in one line and check.
# default: true
strictCheck = true

# Whether or not use the default excludes. Check Default.EXCLUDES for the completed list.
# To suppress part of excludes in the list, declare exact the same pattern in `includes` list.
# default: true
useDefaultExcludes = true

# The supported patterns of includes and excludes follow java.nio.file.PathMatcher,
# plus that `[^/]**/` can match zero folder.
# See also https://docs.oracle.com/javase/8/docs/api/java/nio/file/FileSystem.html#getPathMatcher-java.lang.String-

# Patterns of path to be included on execution.
# default: all the files under `baseDir`.
includes = ["..."]

# Patterns of path to be excluded on execution. A leading bang (!) indicates a invert exclude rule.
# default: empty; if useDefaultExcludes is true, append default excludes.
excludes = ["..."]

# Keywords that should occur in the header, case insensitive.
# default: ["copyright"]
keywords = ["copyright", "..."]

# Whether or not use the default mapping. Check DocumentType.defaultMapping() for the completed list.
# default: true
useDefaultMapping = true

# Paths to additional header style files. The model of user-defined header style can be found below.
# default: empty
additionalHeaders = ["..."]

# Mapping rules.
#
# The key of a mapping rule is:
# 1. file extension, like 'ts' or 'java';
# 2. filename, like 'Dockerfile.native' or 'my_executable_file'.
#
# Available header style types consist of those defined at `HeaderType` and user-defined ones in `additionalHeaders`.
# The name of header style type is case insensitive.
#
# If useDefaultMapping is true, the mapping rules defined here can override the default one.
[mapping]
'extension' = 'header_style_type'
'filename' = 'another_header_style_type'

# Properties to fulfill the template.
# For a defined key-value pair, you can use ${key} in the header template, which will be substituted
# with the corresponding value. Builtin properties have a 'builtin.' prefix.
#
# Builtin properties:
# 1. builtin.filename is the current file name, like: pom.xml.
# 2. builtin.thisYear is the current year, like: 2023.
[properties]
inceptionYear = 2023
```

### Header style file

```toml
# [REQUIRED] The name of this header.
[my_header_style]

# The first fixed line of this header. Default to none.
firstLine = "..."

# The last fixed line of this header. Default to none.
endLine = "..."

# The characters to prepend before each license header lines. Default to empty.
beforeEachLine = "..."

# The characters to append after each license header lines. Default to empty.
afterEachLine = "..."

# Only for multi-line comments: specify if blank lines are allowed.
# Default to false because most of the time, a header has some characters on each line.
allowBlankLines = false

# Specify whether this is a multi-line comment style or not.
#
# A multi-line comment style is equivalent to what we have in Java, where a first line and line will delimit
# a whole multi-line comment section.
#
# A style that is not multi-line is usually repeating in each line the characters before and after each line
# to delimit a one-line comment.
#
# Defaulut to true.
multipleLines = true

# Only for non multi-line comments: specify if some spaces should be added after the header line and before
# the `afterEachLine` characters so that all the lines are aligned.
#
# Default to false.
padLines = false

# A regex to define a first line in a file that should be skipped and kept untouched, like the XML declaration
# at the top of XML documents.
#
# Default to none.
skipLinePattern = "..."

# [REQUIRED] The regex used to detect the start of a header section or line.
firstLineDetectionPattern = "..."

# [REQUIRED] The regex used to detect the end of a header section or line.
lastLineDetectionPattern = "..."
```

## License

[Apache License 2.0](LICENSE)

## Acknowledgment

This software is strongly inspired by [license-maven-plugin](https://github.com/mathieucarbou/license-maven-plugin), with an initial motivation to bring it beyond a Maven plugin.
