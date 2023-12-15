# Contribute to HawkEye

## Debugging Docker image

To print DEBUG level log:

```shell
docker run --rm -v $(pwd):/github/workspace ghcr.io/korandoru/hawkeye-native:v4 -Dorg.slf4j.simpleLogger.defaultLogLevel=debug check
```
