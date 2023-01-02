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
