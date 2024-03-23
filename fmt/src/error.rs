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
    Selection {
        msg: String,
    },
}
