use std::path::PathBuf;

fn main() -> Result<(), Box<dyn std::error::Error>> {
    use std::fs;

    use hawkeye_fmt::config::Config;
    let config = fs::read_to_string("licenserc.toml")?;
    let config = toml::from_str::<Config>(&config)?;
    // println!("config {:#?}", config);

    // let default_headers = hawkeye_fmt::header::model::default_headers();
    // println!("default_headers {:#?}", default_headers);

    let result = hawkeye_fmt::selection::Selection::new(
        config.base_dir,
        config.includes,
        config.excludes,
        config.use_default_excludes,
    )
    .select();

    println!("result {:#?}", result);

    Ok(())
}
