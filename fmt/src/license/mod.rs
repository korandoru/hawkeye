use crate::config::Config;

#[derive(Debug, Clone)]
pub struct HeaderSource {
    pub content: String,
}

impl HeaderSource {
    pub fn from_config(config: &Config) -> Option<Self> {
        config
            .inline_header
            .as_ref()
            .map(|content| HeaderSource {
                content: content.clone(),
            })
            .or_else(|| config.header_path.as_ref().and_then(bundled_headers))
    }
}

pub fn bundled_headers(name: &str) -> Option<HeaderSource> {
    match name {
        "Apache-2.0.txt" => Some(HeaderSource {
            content: include_str!("Apache-2.0.txt").to_string(),
        }),
        "Apache-2.0-ASF.txt" => Some(HeaderSource {
            content: include_str!("Apache-2.0-ASF.txt").to_string(),
        }),
        _ => None,
    }
}
