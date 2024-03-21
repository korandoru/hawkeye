use git2::{Error as GitError, Repository};
use jni::JNIEnv;
use jni::objects::{JClass, JString};
use jni::sys::{jboolean, jlong};

mod error;

pub type Result<T> = std::result::Result<T, error::Error>;

#[no_mangle]
pub extern "system" fn Java_io_korandoru_hawkeye_core_GitHelper_openRepository(
    mut env: JNIEnv,
    _: JClass,
) -> jlong {
    intern_open_repository().unwrap_or_else(|e| {
        e.throw(&mut env);
        0 as jlong
    })
}

fn intern_open_repository() -> Result<jlong> {
    let repo = Repository::open_from_env()?;
    if repo.workdir().is_none() {
        GitError::from_str("No workdir")?;
    }
    Ok(Box::into_raw(Box::new(repo)) as jlong)
}

#[no_mangle]
pub extern "system" fn Java_io_korandoru_hawkeye_core_GitHelper_isPathIgnored(
    mut env: JNIEnv,
    _: JClass,
    repo: *mut Repository,
    path: JString,
) -> jboolean {
    intern_is_path_ignored(&mut env, repo, path).unwrap_or_else(|e| {
        e.throw(&mut env);
        0 as jboolean
    })
}

fn intern_is_path_ignored(env: &mut JNIEnv, repo: *mut Repository, path: JString) -> Result<jboolean> {
    let repo = unsafe { &*repo };
    let path = jstring_to_string(env, &path)?;
    let ignored = repo.is_path_ignored(path)?;
    Ok(ignored as jboolean)
}


/// # Safety
///
/// The caller must guarantee that the Object passed in is an instance
/// of `java.lang.String`, passing in anything else will lead to undefined behavior.
pub(crate) fn jstring_to_string(env: &mut JNIEnv, s: &JString) -> Result<String> {
    let res = unsafe { env.get_string_unchecked(s)? };
    Ok(res.into())
}
