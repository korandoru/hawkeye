use git2::{Error, Repository};
use jni::JNIEnv;
use jni::objects::JClass;
use jni::sys::jstring;

#[no_mangle]
pub extern "system" fn Java_io_korandoru_hawkeye_core_GitHelper_workdir(
    _: JNIEnv,
    _: JClass,
) -> jstring {
    workdir()
}

fn workdir() -> Result<String, Error> {
    Repository::open_from_env().and_then(|repo| {
        let path = repo.workdir().ok_or_else(|| Error::from_str("No workdir"))?;
        Ok(path.to_string_lossy().to_string())
    })
}
