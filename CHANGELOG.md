# CHANGELOG

All notable changes to this project will be documented in this file.

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
