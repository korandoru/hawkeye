# HawkEye GitHub Action

This action provides the following functionality for GitHub Actions users:

* Run license header check, format or remove action provided by HawkEye with a config file.

## Usage

See [action.yml](action.yml).

**Basic:**

```yaml
steps:
  - uses: actions/checkout@v3
  - uses: korandoru/hawkeye/distribution/action@v1
```

Read more at [HawkEye's README](../../README.md).

## License

The code and documentation in this project are released under the [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0).
