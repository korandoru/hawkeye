# HawkEye

Simple license header checker and formatter, in multiple distribution forms.

## Usage

You can use HawkEye in GitHub Actions or in your local machine. HawkEye provides three basic commands:

```bash
# check license headers
hawkeye check

# format license headers (auto-fix all files that failed the check)
hawkeye format

# remove license headers
hawkeye remove
```

You can use `-h` or `--help` to list out all config options.

### GitHub Actions

The HawkEye GitHub Action enables users to run license header check by HawkEye with a config file.

First of all, add a `licenserc.toml` file in the root of your project. The simplest config for projects licensed under Apache License 2.0 is as below:

> **Note** The full configurations can be found in [the configuration section](#configurations).

```toml
headerPath = "Apache-2.0.txt"

[properties]
inceptionYear = 2023
copyrightOwner = "tison <wander4096@gmail.com>"
```

You should change the copyright line according to your information.

To check license headers in GitHub Actions, add a step in your GitHub workflow:

```yaml
- name: Check License Header
  uses: korandoru/hawkeye@v4
```

### Docker

Alpine image (~27MB):

```shell
docker run -it --rm -v $(pwd):/github/workspace ghcr.io/korandoru/hawkeye check
# or (for historical reason, duplicate a "hawkeye-native" image)
docker run -it --rm -v $(pwd):/github/workspace ghcr.io/korandoru/hawkeye-native check
```

## Build

This steps requires Rust toolchain.

```shell
# Build
cargo build --workspace --all-features --tests --examples --benches
# Run
cargo run
```

Build Docker image:

```shell
docker build . -t hawkeye
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

# Whether you use the default excludes. Check Default.EXCLUDES for the completed list.
# To suppress part of excludes in the list, declare exact the same pattern in `includes` list.
# default: true
useDefaultExcludes = true

# The supported patterns of includes and excludes follow gitignore pattern format, plus that:
# 1. `includes` does not support `!`
# 2. backslash does not escape letter
# 3. whitespaces and `#` are normal since we configure line by line
# See also https://git-scm.com/docs/gitignore#_pattern_format

# Patterns of path to be included on execution.
# default: all the files under `baseDir`.
includes = ["..."]

# Patterns of path to be excluded on execution. A leading bang (!) indicates an invert exclude rule.
# default: empty; if useDefaultExcludes is true, append default excludes.
excludes = ["..."]

# Keywords that should occur in the header, case-insensitive.
# default: ["copyright"]
keywords = ["copyright", "..."]

# Whether you use the default mapping. Check DocumentType.defaultMapping() for the completed list.
# default: true
useDefaultMapping = true

# Paths to additional header style files. The model of user-defined header style can be found below.
# default: empty
# WARNING: not yet re-supported.
additionalHeaders = ["..."]

# Mapping rules (repeated).
#
# The key of a mapping rule is a header style type (case-insensitive).
#
# Available header style types consist of those defined at `HeaderType` and user-defined ones in `additionalHeaders`.
# The name of header style type is case-insensitive.
#
# If useDefaultMapping is true, the mapping rules defined here can override the default one.
[mapping.STYLE_NAME]
filenames = ["..."]  # e.g. "Dockerfile.native"
extensions = ["..."] # e.g. "cc"

# Properties to fulfill the template.
# For a defined key-value pair, you can use ${key} in the header template, which will be substituted
# with the corresponding value.
#
# Preset properties:
# * 'hawkeye.core.filename' is the current file name, like: pom.xml.
[properties]
inceptionYear = 2023

# Options to configure Git features.
[git]
# If enabled, do not process files that are ignored by Git; possible value: ['auto', 'enable', 'disable']
# 'auto' means this feature tries to be enabled with:
#   * libgit2 - if `basedir` is in a Git repository.
#   * ignore crate's gitignore rules - if `basedir` is not in a Git repository.
# 'enabled' means always enabled with libgit2; failed if it is impossible.
# default: 'auto'
ignore = 'auto'
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

## History

This software is originally from [license-maven-plugin](https://github.com/mathieucarbou/license-maven-plugin),with an initial motivation to bring it beyond a Maven plugin. The core abstractions like `Document`, `Header`, and `HeaderParser` are originally copied from the license-maven-plugin sources under the terms of Apache License 2.0.

Later, when I started focusing on the Docker image's size and integrate with Git, I found that Rust is better than Java (GraalVM Native Image) for this purpose. So I rewrote the core logic in Rust and keep ship a slim image.
