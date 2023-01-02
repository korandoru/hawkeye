# HawkEye

Simple license header checker and formatter, in multiple distribution forms.

## Build

### Executable JAR

```shell
# Build
mvn clean install -DskipTests

# Run
./distribution/commandline/target/hawkeye-<version>-bin/hawkeye-<version>/hawkeye
```

### Native Image

```shell
# Build with GraalVM
mvn clean insatll -DskipTests

# Run
./distribution/native/target/hawkeye-native
```

## Acknowledgment

This software is strongly inspired by [license-maven-plugin](https://github.com/mathieucarbou/license-maven-plugin), with an initial motivation to bring it beyond a Maven plugin.