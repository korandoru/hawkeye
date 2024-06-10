# Contributing Guide

Thanks for your help in improving the project!

There are two typical contributions to this project:

1. Add support for new languages. You can refer to https://github.com/korandoru/hawkeye/pull/124 as an example.
2. Add support for new license templates. You can refer to https://github.com/korandoru/hawkeye/pull/148 as an example.

You can find the core concepts with names listed below to understand the design better:

* `DocumentType` defines how a file in a specific type should be handled.
* `HeaderDef` defines the format of a specific header; for example, `SCRIPT_STYLE` will construct the header format for scripts like `.sh` or `.py` files.
* `HeaderStyle` is the serde layer for `HeaderDef`.
* `Selection` describes how to find the files to be handled.
* `HeaderParser` extracts the header slice from a source file.
