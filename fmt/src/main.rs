use std::fs;
use hawkeye_fmt::config::Config;

fn main() -> Result<(), Box<dyn std::error::Error>> {
    let config = fs::read_to_string("licenserc.toml")?;
    let config = toml::from_str::<Config>(&config)?;
    println!("{:#?}", config);
    Ok(())
}
