use snafu::Snafu;

#[derive(Snafu, Debug)]
#[snafu(visibility(pub))]
pub enum Error {
    #[snafu(display("selection walker failed: {}", source))]
    SelectionWalker {
        #[snafu(source)]
        source: ignore::Error,
    },

    #[snafu(display("select files failed: {}", message))]
    SelectFiles { message: String },

    #[snafu(display("header type {} not found", header_type))]
    HeaderDefinitionNotFound { header_type: String },

    #[snafu(display("cannot to create document {}: {}", path, source))]
    CreateDocument {
        path: String,
        #[snafu(source)]
        source: std::io::Error,
    },

    #[snafu(display("cannot try to matching header: {}", source))]
    TryMatchHeader {
        #[snafu(source)]
        source: std::io::Error,
    },

    #[snafu(display("cannot load config {}: {}", name, source))]
    LoadConfig {
        name: String,
        #[snafu(source)]
        source: std::io::Error,
    },

    #[snafu(display("cannot parse {}: {}", name, source))]
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
    },

    #[snafu(display("invalid config: {}", message))]
    InvalidConfig { message: String },
}
