fn main() {
    let repo = git2::Repository::open_from_env().unwrap();
    println!("ignored: {}", repo.is_path_ignored("hawkeye-core/native/target").unwrap());
    println!("Repo: {:?}", repo.workdir());
}
