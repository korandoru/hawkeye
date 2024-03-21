use git2::{Repository, Error as GitError};
use jni::JNIEnv;
use jni::objects::{JClass, JString};
use jni::sys::jstring;

mod error;

pub type Result<T> = std::result::Result<T, error::Error>;

#[no_mangle]
pub extern "system" fn Java_io_korandoru_hawkeye_core_GitHelper_workdir(
    mut env: JNIEnv,
    _: JClass,
) -> jstring {
    workdir().unwrap_or_else(|e| {
        e.throw(&mut env);
        JString::default().into_raw()
    })
}

fn workdir() -> Result<jstring> {
    let repo = Repository::open_from_env()?;
    let path = repo.workdir().ok_or_else(|| GitError::from_str("No workdir"))?;
    let path = path.to_string_lossy().to_string();
    Ok(JString::from(path).into_inner())
}
