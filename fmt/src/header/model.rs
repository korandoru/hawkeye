// Copyright 2024 tison <wander4096@gmail.com>
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

use std::collections::HashMap;

use regex::Regex;
use serde::{Deserialize, Serialize};
use snafu::ResultExt;

use crate::{
    default_true,
    error::{DeserializeSnafu, MalformedRegexSnafu},
    Result,
};

#[derive(Debug, Clone, Default)]
pub struct HeaderDef {
    pub name: String,
    pub first_line: String,
    pub end_line: String,
    pub before_each_line: String,
    pub after_each_line: String,

    pub allow_blank_lines: bool,
    pub multiple_lines: bool,
    pub pad_lines: bool,

    pub skip_line_pattern: Option<Regex>,
    pub first_line_detection_pattern: Option<Regex>,
    pub last_line_detection_pattern: Option<Regex>,
}

impl HeaderDef {
    /// Tells if the given content line must be skipped according to this header definition. The
    /// header is outputted after any skipped line if any pattern defined on this point or on the
    /// first line if not pattern defined.
    pub fn is_skip_line(&self, line: &str) -> bool {
        self.skip_line_pattern
            .as_ref()
            .map_or(false, |pattern| pattern.is_match(line))
    }

    /// Tells if the given content line is the first line of a possible header of this definition
    /// kind.
    pub fn is_first_header_line(&self, line: &str) -> bool {
        self.first_line_detection_pattern
            .as_ref()
            .map_or(false, |pattern| pattern.is_match(line))
    }

    /// Tells if the given content line is the last line of a possible header of this definition
    /// kind.
    pub fn is_last_header_line(&self, line: &str) -> bool {
        self.last_line_detection_pattern
            .as_ref()
            .map_or(false, |pattern| pattern.is_match(line))
    }
}

pub fn default_headers() -> Result<HashMap<String, HeaderDef>> {
    let defaults = include_str!("defaults.toml");
    deserialize_header_definitions(defaults.to_string())
}

pub fn deserialize_header_definitions(value: String) -> Result<HashMap<String, HeaderDef>> {
    let header_styles: HashMap<String, HeaderStyle> =
        toml::from_str(&value).context(DeserializeSnafu {
            name: "default headers",
        })?;

    let headers = header_styles
        .into_iter()
        .map(|(name, style)| {
            let name = name.to_lowercase();

            assert!(
                !(style.allow_blank_lines && !style.multiple_lines),
                "Header style {name} is configured to allow blank lines, so it should be set as a multi-line header style"
            );

            let def = HeaderDef {
                name: name.clone(),
                first_line: style.first_line,
                end_line: style.end_line,
                before_each_line: style.before_each_line,
                after_each_line: style.after_each_line,
                allow_blank_lines: style.allow_blank_lines,
                multiple_lines: style.multiple_lines,
                pad_lines: style.pad_lines,
                skip_line_pattern: style
                    .skip_line_pattern
                    .map(|pattern| Regex::new(&pattern).context(MalformedRegexSnafu {
                        payload: pattern,
                    })).transpose()?,
                first_line_detection_pattern: style
                    .first_line_detection_pattern
                    .map(|pattern| Regex::new(&pattern).expect("malformed regex")),
                last_line_detection_pattern: style
                    .last_line_detection_pattern
                    .map(|pattern| Regex::new(&pattern).expect("malformed regex")),
            };

            Ok((name, def))
        })
        .collect::<Result<HashMap<String, HeaderDef>>>()?;
    Ok(headers)
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(default, rename_all = "camelCase")]
pub struct HeaderStyle {
    /// The first fixed line of this header.
    pub first_line: String,
    /// The last fixed line of this header.
    pub end_line: String,
    /// The characters to prepend before each license header lines. Default to empty.
    pub before_each_line: String,
    /// The characters to append after each license header lines. Default to empty.
    pub after_each_line: String,
    /// Only for multi-line comments: specify if blank lines are allowed.
    /// Default to false because most of the time, a header has some characters on each line.
    pub allow_blank_lines: bool,
    /// Specify whether this is a multi-line comment style or not.
    ///
    /// A multi-line comment style is equivalent to what we have in Java, where a first line and
    /// line will delimit a whole multi-line comment section.
    ///
    /// A style that is not multi-line is usually repeating in each line the characters before and
    /// after each line to delimit a one-line comment.
    #[serde(default = "default_true")]
    pub multiple_lines: bool,
    /// Only for non multi-line comments: specify if some spaces should be added after the header
    /// line and before the {@link #afterEachLine} characters so that all the lines are aligned.
    /// Default to false.
    pub pad_lines: bool,
    /// A regex to define a first line in a file that should be skipped and kept untouched, like
    /// the XML declaration at the top of XML documents. Default to none.
    pub skip_line_pattern: Option<String>,
    /// The regex used to detect the start of a header section or line.
    pub first_line_detection_pattern: Option<String>,
    /// The regex used to detect the end of a header section or line.
    pub last_line_detection_pattern: Option<String>,
}