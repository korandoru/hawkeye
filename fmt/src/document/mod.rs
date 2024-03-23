use std::{collections::HashMap, fs, fs::File, io::BufRead, path::PathBuf};

use crate::{
    header,
    header::{matcher::HeaderMatcher, model::HeaderDef, parser::HeaderParser},
};

pub mod factory;
pub mod model;

pub struct Document {
    pub filepath: PathBuf,

    header_def: HeaderDef,
    properties: HashMap<String, String>,
    parser: HeaderParser,
}

impl Document {
    pub fn new(
        filepath: PathBuf,
        header_def: HeaderDef,
        keywords: &[String],
        properties: HashMap<String, String>,
    ) -> std::io::Result<Self> {
        let parser = header::parser::parse_header(&filepath, header_def.clone(), keywords)?;
        Ok(Self {
            filepath,
            header_def,
            properties,
            parser,
        })
    }

    pub fn is_unsupported(&self) -> bool {
        self.header_def.name.eq_ignore_ascii_case("unknown")
    }

    /// Detected but not necessarily a valid header
    pub fn header_detected(&self) -> bool {
        self.parser.end_pos.is_some()
    }

    /// Detected and valid header
    pub fn header_matched(&self, header: &HeaderMatcher, strict_check: bool) -> bool {
        if strict_check {
            let file_header = {
                let mut lines = self
                    .read_file_first_lines(header)
                    .expect("read file first lines")
                    .join("\n");
                lines.push_str("\n\n");
                lines.replace(" *\r?\n", "\n")
            };
            let expected_header = {
                let raw_header = header.build_for_definition(&self.header_def);
                let resolved_header = self.merge_properties(&raw_header);
                resolved_header.replace(" *\r?\n", "\n")
            };
            file_header.contains(expected_header.as_str())
        } else {
            let file_header = self
                .read_file_header_on_one_line(header)
                .expect("read file header");
            let expected_header = self.merge_properties(header.header_content_one_line());
            file_header.contains(expected_header.as_str())
        }
    }

    fn read_file_first_lines(&self, header: &HeaderMatcher) -> std::io::Result<Vec<String>> {
        let file = File::open(&self.filepath)?;
        std::io::BufReader::new(file)
            .lines()
            .take(header.header_content_lines_count() + 10)
            .collect::<std::io::Result<Vec<_>>>()
    }

    fn read_file_header_on_one_line(&self, header: &HeaderMatcher) -> std::io::Result<String> {
        let first_lines = self.read_file_first_lines(header)?;
        let file_header = first_lines
            .join("")
            .trim()
            .replace(self.header_def.first_line.trim(), "")
            .replace(self.header_def.end_line.trim(), "")
            .replace(self.header_def.before_each_line.trim(), "")
            .replace(self.header_def.after_each_line.trim(), "")
            .split_whitespace()
            .collect();
        Ok(file_header)
    }

    pub fn update_header(&mut self, header: &HeaderMatcher) {
        let header_str = header.build_for_definition(&self.header_def);
        let begin_pos = self.parser.begin_pos;
        self.parser
            .file_content
            .insert(begin_pos, header_str.as_str());
    }

    pub fn remove_header(&mut self) {
        if let Some(end_pos) = self.parser.end_pos {
            self.parser
                .file_content
                .delete(self.parser.begin_pos, end_pos);
        }
    }

    pub fn save(&mut self, filepath: Option<&PathBuf>) -> std::io::Result<()> {
        if let Some(filepath) = filepath {
            fs::write(filepath, self.parser.file_content.content())
        } else {
            fs::write(&self.filepath, self.parser.file_content.content())
        }
    }

    pub(crate) fn merge_properties(&self, s: &str) -> String {
        let mut properties = self.properties.clone();
        properties.insert(
            "hawkeye.core.filename".to_string(),
            self.filepath
                .file_name()
                .map(|s| s.to_string_lossy().to_string())
                .expect("malformed filename"),
        );
        merge_properties(&properties, s)
    }
}

pub fn merge_properties(properties: &HashMap<String, String>, s: &str) -> String {
    let mut result = s.to_string();
    for (key, value) in properties {
        result = result.replace(&format!("${{{key}}}"), value);
    }
    result
}
