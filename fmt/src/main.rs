use hawkeye_fmt::processor::check_license_header;

fn main() -> Result<(), Box<dyn std::error::Error>> {
    use std::fs;

    use hawkeye_fmt::config::Config;
    let config = fs::read_to_string("licenserc.toml")?;
    let config = toml::from_str::<Config>(&config)?;

    check_license_header(config).unwrap();

    //
    // // let before = "before";
    // // let after = "after";
    // // let line = "line";
    // // let max_length = 10;
    // // println!("{}", format!("{before}{line: <max_length$}{after}"));
    //
    // // let default_mapping = hawkeye_fmt::document::model::default_mapping();
    // // println!("default_mapping {:#?}", default_mapping);
    //
    // // let default_headers = hawkeye_fmt::header::model::default_headers();
    // // println!("default_headers {:#?}", default_headers);
    // let header_source = hawkeye_fmt::license::HeaderSource::from_config(&config).unwrap();
    // // let header_matcher =
    // //     hawkeye_fmt::header::matcher::HeaderMatcher::new(header_source.content);
    // // println!(
    // //     "header_matcher:\n{}",
    // //     header_matcher.build_for_definition(default_headers.get("script_style").unwrap())
    // // );
    // // println!("[delimiter]");
    //
    // let merged_header = hawkeye_fmt::document::merge_properties(
    //     &config.properties,
    //     &header_source.content,
    // );
    // println!("merged_header:\n{}", merged_header);
    //
    // // let result = hawkeye_fmt::selection::Selection::new(
    // //     config.base_dir,
    // //     config.includes,
    // //     config.excludes,
    // //     config.use_default_excludes,
    // // )
    // // .select();
    // //
    // // println!("result {:#?}", result);

    Ok(())
}
