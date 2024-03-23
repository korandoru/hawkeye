use snafu::Snafu;

#[derive(Snafu, Debug)]
#[snafu(visibility(pub))]
pub enum Error {
    #[snafu(display("Selection walker failed. {}", source))]
    SelectionWalker {
        #[snafu(source)]
        source: ignore::Error,
    },

    #[snafu(display("Selection failed. {}", msg))]
    Selection { msg: String },

    #[snafu(display("Header type {} not found", header_type))]
    HeaderDefinitionNotFound { header_type: String },

    #[snafu(display("Failed to create document: {}", source))]
    DocumentCreation {
        #[snafu(source)]
        source: std::io::Error,
    },

    #[snafu(display("Failed to parse config {}: {}", name, source))]
    Deserialize {
        name: String,
        #[snafu(source)]
        source: toml::de::Error,
    },

    #[snafu(display("malformed regex {}: {}", payload, source))]
    MalformedRegex {
        payload: String,
        #[snafu(source)]
        source: regex::Error,
    }
}
