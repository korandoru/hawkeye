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

use hawkeye::Config;
use hawkeye::ErrorKind;

use super::support::Project;
use super::support::assert_exit;
use super::support::assert_report;
use super::support::stderr;

#[test]
fn shared_configs_use_cwd_independently_of_their_location() {
    for directory in [
        ".hawkeye",
        "template/.hawkeye",
        "template/template/.hawkeye",
    ] {
        let project = Project::empty();
        let config_path = format!("{directory}/licenserc.toml");
        project.write(
            &config_path,
            r#"[header]
path = "HEADER.txt"

[files]
root = "{{ cwd }}"
includes = ["**/*.rs"]
excludes = ["generated/**"]

[props]
owner = "Acme"

[git]
ignore = "disable"
"#,
        );
        project.write(
            format!("{directory}/HEADER.txt"),
            "Copyright {{ props.owner }} in {{ attrs.filename }}",
        );
        project.write("main.rs", "fn main() {}\n");
        project.write("generated/ignored.rs", "fn ignored() {}\n");
        project.write("untouched.txt", "untouched\n");

        let formatted = project
            .command(["--config", &config_path, "format", "--output-format=json"])
            .env("PWD", project.path().join(directory))
            .output()
            .expect("run with a misleading PWD environment variable");
        assert_exit(&formatted, 0);
        assert_report(&formatted, &[("main.rs", "add")]);
        assert_eq!(
            project.read("main.rs"),
            "// Copyright Acme in main.rs\n\nfn main() {}\n"
        );
        assert_eq!(project.read("generated/ignored.rs"), "fn ignored() {}\n");
        assert_eq!(project.read("untouched.txt"), "untouched\n");
    }
}

#[test]
fn defaults_and_relative_results_use_the_config_directory() {
    let project = Project::empty();
    for root in ["", "root = '.'", "root = \"{{ '.' }}\""] {
        project.write(
            "config/licenserc.toml",
            format!("[header]\npath = 'HEADER.txt'\n[files]\n{root}\n"),
        );
        let path = project.path().join("config/licenserc.toml");
        let directory = path
            .canonicalize()
            .expect("resolve config")
            .parent()
            .unwrap()
            .to_owned();
        let config = Config::load(path).expect("resolve default or relative root");
        assert_eq!(config.files.root, directory.join("."));
        assert_eq!(config.header.path, Some(directory.join("HEADER.txt")));
    }
}

#[test]
fn config_path_and_config_directory_are_absolute_template_values() {
    let project = Project::empty();
    project.write(
        "config/licenserc.toml",
        r#"[header]
path = "{{ config_path }}.header"

[files]
root = "{{ [config_dir, 'source'] | join_path }}"
includes = ["**/*.rs"]

[git]
ignore = "disable"
"#,
    );
    project.write("config/licenserc.toml.header", "Copyright Acme");
    project.write("config/source/main.rs", "fn main() {}\n");
    project.write("source/outside.rs", "fn outside() {}\n");

    let checked = project.run([
        "--config",
        "config/licenserc.toml",
        "check",
        "--output-format=json",
    ]);
    assert_exit(&checked, 1);
    assert_report(&checked, &[("main.rs", "add")]);
}

#[test]
fn environment_paths_are_rendered_once_before_relative_resolution() {
    let project = Project::empty();
    let headers = Project::empty();
    project.write(
        "config/licenserc.toml",
        r#"[header]
path = "{{ env.HAWKEYE_TEST_HEADER }}"

[files]
root = "{{ env.HAWKEYE_TEST_ROOT | default(cwd) }}"
includes = ["**/*.rs"]

[git]
ignore = "disable"
"#,
    );
    headers.write("HEADER.txt", "Copyright {{ attrs.filename }}");
    let directory = "source & 'quoted' {{ literal }}";
    project.write(format!("config/{directory}/main.rs"), "fn main() {}\n");

    for root in [
        directory.into(),
        project
            .path()
            .join("config")
            .join(directory)
            .into_os_string(),
    ] {
        let formatted = project
            .command([
                "--config",
                "config/licenserc.toml",
                "format",
                "--dry-run",
                "--output-format=json",
            ])
            .env("HAWKEYE_TEST_ROOT", root)
            .env("HAWKEYE_TEST_HEADER", headers.path().join("HEADER.txt"))
            .output()
            .expect("render relative or absolute environment paths");
        assert_exit(&formatted, 0);
        assert_report(&formatted, &[("main.rs", "add")]);
    }

    let fallback = project
        .command([
            "--config",
            "config/licenserc.toml",
            "check",
            "--output-format=json",
        ])
        .env_remove("HAWKEYE_TEST_ROOT")
        .env("HAWKEYE_TEST_HEADER", headers.path().join("HEADER.txt"))
        .output()
        .expect("use cwd when an optional environment variable is absent");
    assert_exit(&fallback, 1);
    assert_report(
        &fallback,
        &[(&format!("config/{directory}/main.rs"), "add")],
    );
}

#[test]
fn templated_roots_control_git_discovery_and_requested_paths() {
    let project = Project::empty();
    let policy = Project::empty();
    policy.write(
        "licenserc.toml",
        r#"[header]
path = "{{ [config_dir, 'HEADER.txt'] | join_path }}"

[files]
root = "{{ [cwd, 'source'] | join_path }}"
includes = ["**/*.rs"]
excludes = ["generated/**"]

[git]
ignore = "enable"
file_attrs = "enable"
"#,
    );
    policy.write(
        "HEADER.txt",
        "Copyright {{ attrs.git_file_created_year }} Acme",
    );
    project.write("source/main.rs", "fn main() {}\n");
    project.write("source/other.rs", "fn other() {}\n");
    project.write("source/ignored.rs", "fn ignored() {}\n");
    project.write("source/generated/ignored.rs", "fn generated() {}\n");
    project.write("outside.rs", "fn outside() {}\n");
    project.write(".gitignore", "source/ignored.rs\n");
    project.git(["init", "--initial-branch=main"]);
    project.git(["add", "source/main.rs"]);
    project.commit(
        "add source",
        "Acme",
        "acme@example.com",
        "2020-01-01T00:00:00Z",
    );

    let checked = project
        .command(["check", "--output-format=json"])
        .arg("--config")
        .arg(policy.path().join("licenserc.toml"))
        .output()
        .expect("discover Git from the rendered root");
    assert_exit(&checked, 1);
    assert_report(&checked, &[("main.rs", "add"), ("other.rs", "add")]);

    let formatted = project
        .command([
            "format",
            "source/main.rs",
            "source/generated/ignored.rs",
            "outside.rs",
            "--output-format=json",
        ])
        .arg("--config")
        .arg(policy.path().join("licenserc.toml"))
        .output()
        .expect("scope requested paths under the rendered root");
    assert_exit(&formatted, 0);
    assert_report(&formatted, &[("main.rs", "add")]);
    assert_eq!(
        project.read("source/main.rs"),
        "// Copyright 2020 Acme\n\nfn main() {}\n"
    );
    assert_eq!(project.read("source/other.rs"), "fn other() {}\n");
    assert_eq!(
        project.read("source/generated/ignored.rs"),
        "fn generated() {}\n"
    );
    assert_eq!(project.read("outside.rs"), "fn outside() {}\n");
}

#[test]
fn path_templates_preserve_whitespace_and_allow_literal_delimiters() {
    let project = Project::empty();
    let root = " leading & trailing \r\n";
    project.write(
        "licenserc.toml",
        format!(
            "[header]\npath = '{{% raw %}}{{{{ literal }}}}{{% endraw %}}'\n[files]\nroot = {}\n",
            toml::Value::String(root.to_owned()),
        ),
    );
    let path = project
        .path()
        .join("licenserc.toml")
        .canonicalize()
        .expect("resolve config");
    let config = Config::load(&path).expect("preserve literal path text");
    let directory = path.parent().unwrap();
    assert_eq!(config.files.root, directory.join(root));
    assert_eq!(config.header.path, Some(directory.join("{{ literal }}")));
}

#[test]
fn path_template_errors_identify_the_field_and_failing_expression() {
    let project = Project::empty();
    for (expression, diagnostic) in [
        (
            "{{ missing }}",
            "undefined value in template expression \"missing\"",
        ),
        ("{{", "syntax error"),
        ("", "empty path"),
        ("{{ '' }}", "empty path"),
        ("{{ '\0' }}", "NUL byte"),
        (
            "{{ ['source', 42] | join_path }}",
            "join_path expects a list of strings",
        ),
    ] {
        for field in ["files.root", "header.path"] {
            let quoted = toml::Value::String(expression.to_owned());
            let source = if field == "files.root" {
                format!("[header]\ntext = 'Copyright'\n[files]\nroot = {quoted}\n")
            } else {
                format!("[header]\npath = {quoted}\n")
            };
            project.write("licenserc.toml", source);
            let err = Config::load(project.path().join("licenserc.toml"))
                .expect_err("reject an invalid path template at load time");
            assert_eq!(err.kind(), ErrorKind::ConfigInvalid);
            assert!(err.to_string().contains(field), "{err}");
            assert!(err.to_string().contains(diagnostic), "{err}");
        }
    }

    project.write(
        "licenserc.toml",
        "[header]\npath = '{{ env.HAWKEYE_TEST_HEADER }}'\n",
    );
    let checked = project
        .command(["check"])
        .env_remove("HAWKEYE_TEST_HEADER")
        .output()
        .expect("report a missing environment variable");
    assert_exit(&checked, 2);
    let diagnostic = stderr(&checked);
    assert!(diagnostic.contains("header.path"), "{diagnostic}");
    assert!(
        diagnostic.contains("env.HAWKEYE_TEST_HEADER"),
        "{diagnostic}"
    );
}

#[cfg(target_os = "linux")]
#[test]
fn non_utf8_context_values_fail_only_when_referenced() {
    use std::ffi::OsString;
    use std::os::unix::ffi::OsStringExt;

    let project = Project::named(OsString::from_vec(b"project-\xff".to_vec()));
    project.write("source/main.rs", "fn main() {}\n");
    let invalid = OsString::from_vec(b"source-\xff".to_vec());
    for root in [
        "source",
        "{{ 'source' }}",
        "{{ cwd }}",
        "{{ config_dir }}",
        "{{ config_path }}",
        "{{ env.HAWKEYE_TEST_ROOT }}",
    ] {
        project.write(
            "licenserc.toml",
            format!("[header]\ntext = 'Copyright'\n[files]\nroot = {}\nincludes = ['**/*.rs']\n[git]\nignore = 'disable'\n", toml::Value::String(root.to_owned())),
        );
        let checked = project
            .command(["check", "--output-format=json"])
            .env("HAWKEYE_TEST_ROOT", &invalid)
            .output()
            .expect("load config with non-UTF-8 native paths and environment values");
        if root == "source" || root == "{{ 'source' }}" {
            assert_exit(&checked, 1);
            assert_report(&checked, &[("main.rs", "add")]);
        } else {
            assert_exit(&checked, 2);
            let diagnostic = stderr(&checked);
            assert!(diagnostic.contains("files.root"), "{diagnostic}");
            assert!(diagnostic.contains("not valid UTF-8"), "{diagnostic}");
        }
    }
}
