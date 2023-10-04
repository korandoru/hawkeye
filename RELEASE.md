# Release HawkEye

## Git, GitHub, and Maven Central

This part is driven by Maven Release Plugin.

Prerequisite - set up [your OSSRH account](https://central.sonatype.org/publish/publish-guide/) and add to this project.

First, prepare the release:

```shell
./mvnw release:prepare
```

Second, perform the release:

```shell
./mvnw release:perform
```

This step will publish the artifacts to Maven Central, and create Git tag `vX.Y.Z` locally.

Third, publish the Git tag that triggers Docker image releases:

```shell
git push --tags
```

Finally, check out the `vX.Y.Z` tag and overwrite the `vX` tag:

```shell
# take v3.2.0 and v3 as an example
git checkout v3.2.0
git tag -d v3
git tag -s -m "alias v3.2.0" v3
git push v3 -f
```

