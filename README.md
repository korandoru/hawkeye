# HawkEye

Simple license header checker and formatter, in multiple distribution forms.

## Usage

You can use HawkEye in GitHub Actions or in your local machine.

### GitHub Actions

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
  uses: korandoru/hawkeye/distribution/action@v1
```

### Docker

```shell
docker run -it --rm -v $(pwd):/github/workspace ghcr.io/korandoru/hawkeye-native check --config licenserc.toml
docker run -it --rm -v $(pwd):/github/workspace ghcr.io/korandoru/hawkeye-native check --config licenserc.toml
```

## Build

### Executable JAR

```shell
# Build
./mvnw clean install -DskipTests

# Run
./distribution/commandline/target/hawkeye.jar
```

### Native Image

```shell
# Build with GraalVM
./mvnw clean package -DskipTests -Pnative -DonlyNativeDistro

# Run
./distribution/native/target/hawkeye-native
```

## Configurations

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

# Patterns of path to be excluded on execution.
# default: empty; if useDefaultExcludes is true, append default excludes.
excludes = ["..."]

# Keywords that should occur in the header, case insensitive.
# default: ["copyright"]
keywords = ["copyright", "..."]

# Whether or not use the default mapping. Check DocumentType.defaultMapping() for the completed list.
# default: true
useDefaultMapping = true

# Mapping rules.
#
# The key of a mapping rule is:
# 1. file extension, like 'ts' or 'java';
# 2. filename, like 'Dockerfile.native' or 'my_executable_file'.
#
# Available header type can be found at `HeaderType`, whose name is case insensitive.
# If useDefaultMapping is true, the mapping rules defined here can override the default one.
[mapping]
'extension' = 'HeaderType'
'filename' = 'AnotherHeaderType'

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

## License

[Apache License 2.0](LICENSE)

## Acknowledgment

This software is strongly inspired by [license-maven-plugin](https://github.com/mathieucarbou/license-maven-plugin), with an initial motivation to bring it beyond a Maven plugin.
