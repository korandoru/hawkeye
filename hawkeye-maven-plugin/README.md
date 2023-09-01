# HawkEye Maven Plugin

## Usage

You can integrate HawkEye's functionality with Maven Plugin:

```xml
<plugin>
    <groupId>io.korandoru.hawkeye</groupId>
    <artifactId>hawkeye-maven-plugin</artifactId>
    <version>${hawkeye.version}</version>
</plugin>
```

The plugin provides three goals:

* `check`: Check license headers.
* `format`: Format license headers (auto-fix all files that failed the check).
* `remove`: Remove license headers.

With the plugin properly configured, you can run a specific goal as (take `check` as an example):

```shell
mvn hawkeye:check
```

You can configure a customized location of the `licenserc.toml` file as:

```xml
<plugin>
    <groupId>io.korandoru.hawkeye</groupId>
    <artifactId>hawkeye-maven-plugin</artifactId>
    <version>${hawkeye.version}</version>
    <configuration>
      <configLocation>${...}</configLocation>
    </configuration>
</plugin>
```

... the default location is `${project.basedir}/licenserc.toml`.

## Verify

The `check` goal is bind to the `verify` phase by default. If you'd like to do all verifications with a single `mvn verify`, you can add the HawkEye checks as:

```xml
<plugin>
    <groupId>io.korandoru.hawkeye</groupId>
    <artifactId>hawkeye-maven-plugin</artifactId>
    <version>${hawkeye.version}</version>
    <executions>
    <execution>
        <goals>
            <goal>check</goal>
        </goals>
    </execution>
    </executions>
</plugin>
```

## Multimodule

HawkEye is designed to run against a whole project but Maven plugin is configured to each module.

That said, the default location of configuration file (`${project.basedir}/licenserc.toml`) will be resolved to different place due to each module has its own `${project.basedir}`. This is the same to the basedir of the execution.

Below are two recommendations for configuring multimodule project.

### Aggregator

The HawkEye plugin provides an option named `aggregate` with which you can check the headers for all modules of your project.

You can configure the plugin with `aggregate` for your parent `pom.xml`:

```xml
<plugin>
    <groupId>io.korandoru.hawkeye</groupId>
    <artifactId>hawkeye-maven-plugin</artifactId>
    <version>${hawkeye.version}</version>
    <configuration>
        <aggregate>true</aggregate>
    </configuration>
</plugin>
```

... and properly skip all the submodules.

You can also run as aggregator from the commandline:

```shell
mvn hawkeye:check -pl . -Daggregate=true
```

### Each module

You can still configure the plugin executions for each module, but pay attention to the resolved value of `configLocation` and `baseDir`.

> This means `aggregate=false` and the plugin will exclude submodules when running against a parent module.

The default `configLocation` is `${project.basedir}/licenserc.toml` which requires a `licenserc.toml` per module. If you use one config file for all modules, you should change the config value to a fixed location. [directory-maven-plugin](https://github.com/jdcasey/directory-maven-plugin), `${maven.multiModuleProjectDirectory}`, or [MNG-7038](https://issues.apache.org/jira/browse/MNG-7038) can help.

The default `basedir` is overwritten by `${project.basedir}`, which means the one configured in `licenserc.toml` is not used. This should often be the value you want, but you can still change the directory for each module.

Be aware that this basedir is also the one for resolving `includes` and `excludes`. If a file is not properly included or excluded, think of the resolved value of `includes` and `excludes` pattens.
