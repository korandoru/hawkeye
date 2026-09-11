// Copyright 2026 FastLabs Developers
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

use super::support::Project;
use super::support::assert_exit;
use super::support::assert_report;

const CONFIG: &str = r#"[header]
text = "Copyright {{ props.year }} Acme"

[props]
year = 2026

[files]
includes = ["*.md", "*.MARKDOWN", "*.mdx", "*.html", "*.yaml"]

[[rules]]
extensions = ["md", "markdown", "mdx", "html", "yaml"]
style_out = "xml"
"#;

#[test]
fn check_recognizes_asf_header_after_markdown_frontmatter() {
    let project = Project::empty();
    project.write(
        "licenserc.toml",
        CONFIG.replace(
            "text = \"Copyright {{ props.year }} Acme\"",
            "builtin = \"Apache-2.0-ASF\"",
        ),
    );
    let header = include_str!("../../src/builtin/headers/Apache-2.0-ASF.txt").trim_end();
    let original = format!(
        "---\nname: license-audit\ndescription: Review licensing and attribution.\n---\n\n<!--\n{header}\n-->\n\n# License audit\n"
    );
    project.write("SKILL.md", &original);

    let checked = project.run(["check", "--output-format=json"]);
    assert_exit(&checked, 0);
    assert_report(&checked, &[("SKILL.md", "clean")]);
    assert_eq!(project.read("SKILL.md"), original);
}

#[test]
fn header_lifecycle_preserves_frontmatter_and_body() {
    for (filename, bom, eol) in [
        ("guide.md", "", "\n"),
        ("guide.MARKDOWN", "\u{feff}", "\r\n"),
    ] {
        let project = Project::empty();
        project.write("licenserc.toml", CONFIG);
        let metadata = r#"---
# Copyright metadata is not a license header.
title: "A guide: examples"
description: >-
  Review licensing
  and attribution.
example: |
  ---
  This delimiter is part of a scalar.
authors: &authors [Ada, 李]
reviewers: *authors
options:
  published: true # Keep this comment.
  labels: [docs, licensing]
---


"#;
        let preamble = format!("{bom}{}", metadata.replace('\n', eol));
        // Mixed line endings and the missing final newline in the body must stay untouched.
        let body = "# Guide\r\n\n---\n正文";
        let original = format!("{preamble}{body}");
        project.write(filename, &original);

        let checked = project.run(["check", "--output-format=json"]);
        assert_exit(&checked, 1);
        assert_report(&checked, &[(filename, "add")]);
        assert_eq!(project.read(filename), original);

        let formatted = project.run(["format", "--output-format=json"]);
        assert_exit(&formatted, 0);
        assert_report(&formatted, &[(filename, "add")]);
        let expected = format!("{preamble}<!--{eol}Copyright 2026 Acme{eol}-->{eol}{eol}{body}");
        assert_eq!(project.read(filename), expected);

        let idempotent = project.run(["format", "--output-format=json"]);
        assert_exit(&idempotent, 0);
        assert_report(&idempotent, &[(filename, "clean")]);
        assert_eq!(project.read(filename), expected);
        assert_exit(&project.run(["check"]), 0);

        project.write(
            "licenserc.toml",
            CONFIG.replace("year = 2026", "year = 2027"),
        );
        let updated = project.run(["format", "--output-format=json"]);
        assert_exit(&updated, 0);
        assert_report(&updated, &[(filename, "replace")]);
        assert_eq!(
            project.read(filename),
            format!("{preamble}<!--{eol}Copyright 2027 Acme{eol}-->{eol}{eol}{body}")
        );

        let removed = project.run(["remove", "--output-format=json"]);
        assert_exit(&removed, 0);
        assert_report(&removed, &[(filename, "remove")]);
        assert_eq!(project.read(filename), original);
    }
}

#[test]
fn frontmatter_recognition_does_not_validate_yaml() {
    let project = Project::empty();
    project.write("licenserc.toml", CONFIG);
    let header = "<!--\nCopyright 2026 Acme\n-->\n\n";
    for metadata in [
        "An ordinary paragraph.\n",
        "- A Markdown list\n- Another item\n",
        "",
        "# A metadata comment\n",
        "title: [an unclosed sequence\n",
    ] {
        let preamble = format!("---\n{metadata}---\n\n");
        let body = "# Guide\n\n---\n\nBody\n";
        let original = format!("{preamble}{header}{body}");
        project.write("guide.md", &original);

        let checked = project.run(["check", "--output-format=json"]);
        assert_exit(&checked, 0);
        assert_report(&checked, &[("guide.md", "clean")]);
        assert_eq!(project.read("guide.md"), original);

        assert_exit(&project.run(["remove"]), 0);
        assert_eq!(project.read("guide.md"), format!("{preamble}{body}"));
        assert_exit(&project.run(["format"]), 0);
        assert_eq!(project.read("guide.md"), original);
    }
}

#[test]
fn non_frontmatter_prefixes_remain_body_text() {
    let project = Project::empty();
    project.write("licenserc.toml", CONFIG);
    let header = "<!--\nCopyright 2026 Acme\n-->\n\n";
    for candidate in [
        "---\ntitle: A guide\n",
        "---\ntitle: A guide\n...\n",
        " ---\ntitle: A guide\n---\n",
        "----\ntitle: A guide\n----\n",
        "\n---\ntitle: A guide\n---\n",
        "# Guide\n\n---\nA paragraph.\n---\n",
    ] {
        // A later license-like comment is part of the body, not a removable leading header.
        let body = format!("{candidate}{header}# Body\n");
        project.write("guide.md", &body);
        let removed = project.run(["remove", "--output-format=json"]);
        assert_exit(&removed, 0);
        assert_report(&removed, &[("guide.md", "clean")]);
        assert_eq!(project.read("guide.md"), body);

        let formatted = project.run(["format", "--output-format=json"]);
        assert_exit(&formatted, 0);
        assert_report(&formatted, &[("guide.md", "add")]);
        // Ordinary leading blank lines follow the existing header whitespace policy.
        let retained_body = body.trim_start_matches('\n');
        assert_eq!(project.read("guide.md"), format!("{header}{retained_body}"));
        assert_exit(&project.run(["remove"]), 0);
        assert_eq!(project.read("guide.md"), retained_body);
    }
}

#[test]
fn frontmatter_is_scoped_to_markdown_paths_even_with_the_same_style() {
    let project = Project::empty();
    project.write("licenserc.toml", CONFIG);
    let content = "---\ntitle: A document\n---\n<!--\nCopyright 2026 Acme\n-->\n\nBody\n";
    project.write("guide.mdx", content);
    project.write("document.html", content);
    project.write("document.yaml", content);
    let checked = project.run(["check", "--output-format=json"]);
    assert_exit(&checked, 1);
    assert_report(
        &checked,
        &[
            ("document.html", "add"),
            ("document.yaml", "add"),
            ("guide.mdx", "clean"),
        ],
    );
    assert_exit(&project.run(["remove"]), 0);
    assert_eq!(
        project.read("guide.mdx"),
        "---\ntitle: A document\n---\nBody\n"
    );
    assert_eq!(project.read("document.html"), content);
    assert_eq!(project.read("document.yaml"), content);
}

#[test]
fn frontmatter_at_eof_gets_a_separate_header_line() {
    let project = Project::empty();
    project.write("licenserc.toml", CONFIG);
    let preamble = "--- \t\n{title: A guide}\n---\t";
    project.write("guide.md", preamble);
    assert_exit(&project.run(["format"]), 0);
    let expected = format!("{preamble}\n<!--\nCopyright 2026 Acme\n-->\n\n");
    assert_eq!(project.read("guide.md"), expected);
    let idempotent = project.run(["format", "--output-format=json"]);
    assert_exit(&idempotent, 0);
    assert_report(&idempotent, &[("guide.md", "clean")]);
    assert_eq!(project.read("guide.md"), expected);
    assert_exit(&project.run(["remove"]), 0);
    assert_eq!(project.read("guide.md"), format!("{preamble}\n"));
}
