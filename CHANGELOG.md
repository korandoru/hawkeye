# CHANGELOG

All notable changes to this project will be documented in this file.

## [6.3.0] 2025-10-09

### New features

* Add distribution against musl libc ([#196](https://github.com/korandoru/hawkeye/pull/196)).

## [6.2.0] 2025-08-25

### New features

* Supports format Vue files: pattern = "vue" and headerType = "XML_STYLE".
* Supports format Containerfile files: pattern = "Containerfile" and headerType = "SCRIPT_STYLE".
* Add a shared flag to store lists of files to change ([#194](https://github.com/korandoru/hawkeye/pull/194)).

## [6.1.1] 2025-06-11

### New features

* Supports format CommonJS files: pattern = "cjs" and headerType = "SLASHSTAR_STYLE".
* Supports format Verilog files: pattern = "v" and headerType = "SLASHSTAR_STYLE".
* Supports format SystemVerilog files: pattern = "sv" and headerType = "SLASHSTAR_STYLE".

## [6.1.0] 2025-06-06

### New features

* `attrs.disk_file_created_year` can be used to replace nonexisting Git attrs like `{{attrs.git_file_created_year if attrs.git_file_created_year else attrs.disk_file_created_year }}`

## [6.0.0] 2025-01-28

### Breaking changes

Now, HawkEye uses MiniJinja as the template engine.

All the `properties` configured will be passed to the template engine as the `props` value, and thus:

* Previous `${property}` should be replaced with `{{ props["property"] }}`.
* Previous built-in variables `hawkeye.core.filename` is now `attrs.filename`.
* Previous built-in variables `hawkeye.git.fileCreatedYear` is now `attrs.git_file_created_year`.
* Previous built-in variables `hawkeye.git.fileModifiedYear` is now `attrs.git_file_modified_year`.

New properties:

* `attrs.git_authors` is a collection of authors of the file. You can join them with `, ` to get a string by `{{ attrs.git_authors | join(", ") }}`.

### Notable changes

Now, HawkEye would detect a leading BOM (Byte Order Mark) and remove it if it exists (#166). I tend to treat this as a bug fix, but it may affect the output of the header.
