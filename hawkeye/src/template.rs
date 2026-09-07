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

use std::borrow::Cow;
use std::collections::BTreeMap;
use std::env;
use std::ffi::OsStr;
use std::path::Path;
use std::path::PathBuf;

use minijinja::AutoEscape;
use minijinja::Environment;
use minijinja::ErrorKind as TemplateErrorKind;
use minijinja::UndefinedBehavior;
use minijinja::Value;

use crate::Error;
use crate::ErrorKind;
use crate::engine::FileAttrs;

pub struct PathTemplates<'a> {
    environment: Environment<'static>,
    directory: &'a Path,
}

impl<'a> PathTemplates<'a> {
    pub fn new(config_path: &'a Path) -> Result<Self, Error> {
        let directory = config_path.parent().ok_or_else(|| {
            Error::new(
                ErrorKind::ConfigInvalid,
                "config file has no parent directory",
            )
        })?;
        let mut environment = template_environment();
        environment.set_keep_trailing_newline(true);
        environment.add_global(
            "config_dir",
            native_string("config_dir", directory.as_os_str()),
        );
        environment.add_global(
            "cwd",
            match env::current_dir() {
                Ok(path) => native_string("cwd", path.as_os_str()),
                Err(err) => Value::from(
                    minijinja::Error::new(
                        TemplateErrorKind::InvalidOperation,
                        "cannot determine cwd",
                    )
                    .with_source(err),
                ),
            },
        );
        Ok(Self {
            environment,
            directory,
        })
    }

    pub fn resolve(&self, field: &str, path: &Path) -> Result<PathBuf, Error> {
        let source = path.to_str().ok_or_else(|| {
            Error::new(
                ErrorKind::ConfigInvalid,
                format!("{field} template is not valid UTF-8"),
            )
        })?;
        let rendered = self
            .environment
            .render_named_str(field, source, ())
            .map_err(|err| {
                let detail = render_error_message(&err, source);
                Error::new(
                    ErrorKind::ConfigInvalid,
                    format!("cannot render {field} template: {detail}"),
                )
            })?;
        if rendered.is_empty() {
            return Err(Error::new(
                ErrorKind::ConfigInvalid,
                format!("{field} template rendered an empty path"),
            ));
        }
        if rendered.contains('\0') {
            return Err(Error::new(
                ErrorKind::ConfigInvalid,
                format!("{field} template rendered a NUL byte"),
            ));
        }
        // Canonical Windows paths use verbatim prefixes. Normalize template separators,
        // then rebuild components so `.` and `..` follow native path-joining rules.
        #[cfg(windows)]
        let rendered: PathBuf = Path::new(&rendered.replace('/', "\\"))
            .components()
            .collect();
        Ok(self.directory.join(rendered))
    }
}

fn native_string(name: &str, value: &OsStr) -> Value {
    match value.to_str() {
        Some(value) => Value::from(value),
        // Defer the error until lookup so unused non-UTF-8 values do not prevent loading a config.
        None => Value::from(minijinja::Error::new(
            TemplateErrorKind::InvalidOperation,
            format!("path template variable {name:?} is not valid UTF-8"),
        )),
    }
}

pub struct HeaderTemplate {
    environment: Environment<'static>,
}

impl HeaderTemplate {
    pub fn new<S: Into<Cow<'static, str>>>(source: S) -> Result<Self, Error> {
        let mut environment = template_environment();
        environment
            .add_template_owned("header", source)
            .map_err(|err| {
                Error::new(ErrorKind::ConfigInvalid, "cannot compile header template")
                    .with_source(err)
            })?;
        Ok(Self { environment })
    }

    pub fn render(
        &self,
        props: &BTreeMap<String, toml::Value>,
        attrs: &FileAttrs,
    ) -> Result<String, Error> {
        let template = self.environment.get_template("header").unwrap();
        let rendered = template
            .render(minijinja::context! { props, attrs })
            .map_err(|err| {
                let detail = render_error_message(&err, template.source());
                Error::new(
                    ErrorKind::ConfigInvalid,
                    format!("cannot render header template: {detail}"),
                )
            })?;
        let normalized = rendered.replace("\r\n", "\n").replace('\r', "\n");
        let normalized = normalized.trim_matches('\n').to_owned();
        if normalized.trim().is_empty() {
            return Err(Error::new(
                ErrorKind::ConfigInvalid,
                "header template rendered an empty value",
            ));
        }
        if normalized.contains('\0') {
            return Err(Error::new(
                ErrorKind::ConfigInvalid,
                "header template rendered a NUL byte",
            ));
        }
        Ok(normalized)
    }
}

fn template_environment() -> Environment<'static> {
    let mut environment = Environment::new();
    environment.set_undefined_behavior(UndefinedBehavior::Strict);
    environment.set_auto_escape_callback(|_| AutoEscape::None);
    environment
}

fn render_error_message(error: &minijinja::Error, template_source: &str) -> String {
    if error.kind() != TemplateErrorKind::UndefinedError || error.detail().is_some() {
        return error.to_string();
    }

    let Some(expression) = error
        .range()
        .and_then(|range| template_source.get(range))
        .map(str::trim)
        .filter(|expression| !expression.is_empty())
    else {
        return error.to_string();
    };

    match error.name() {
        Some(name) => format!(
            "undefined value in template expression {expression:?} (in {name}:{})",
            error.line().unwrap_or(0)
        ),
        None => format!("undefined value in template expression {expression:?}"),
    }
}
