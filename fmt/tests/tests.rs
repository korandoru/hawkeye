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

use std::path::Path;

use hawkeye_fmt::header::{model::default_headers, parser::parse_header};

#[test]
fn test_remove_file_only_header() {
    let file = Path::new("tests/content/empty.py");
    let defs = default_headers().unwrap();
    let def = defs.get("script_style").unwrap().clone();
    let keywords = vec!["copyright".to_string()];

    let document = parse_header(file, &def, &keywords).unwrap();
    let end_pos = document.end_pos.unwrap();
    let content = document.file_content.content();
    assert!(content[end_pos..].trim().is_empty());
}

#[test]
fn test_two_headers_should_only_remove_the_first() {
    let file = Path::new("tests/content/two_headers.rs");
    let defs = default_headers().unwrap();
    let def = defs.get("doubleslash_style").unwrap().clone();
    let keywords = vec!["copyright".to_string()];

    let document = parse_header(file, &def, &keywords).unwrap();
    let end_pos = document.end_pos.unwrap();
    let content = document.file_content.content();
    assert!(content[end_pos..].contains("Copyright 2015 The Prometheus Authors"));
}
