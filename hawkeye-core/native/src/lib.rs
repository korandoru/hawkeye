// Copyright 2023 Korandoru Contributors
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

use git2::{Error as GitError, Repository};
use jni::JNIEnv;
use jni::objects::{JClass, JString};
use jni::sys::{jboolean, jlong};
use snafu::ResultExt;

use crate::error::{GitSnafu, JNISnafu};

mod error;

pub type Result<T> = std::result::Result<T, error::Error>;

#[no_mangle]
pub extern "system" fn Java_io_korandoru_hawkeye_core_GitHelper_discoverRepo(
    mut env: JNIEnv,
    _: JClass,
    basedir: JString,
) -> jlong {
    intern_discover_repo(&mut env, basedir).unwrap_or_else(|e| {
        e.throw(&mut env);
        0 as jlong
    })
}

fn intern_discover_repo(env: &mut JNIEnv, basedir: JString,) -> Result<jlong> {
    let basedir = jstring_to_string(env, &basedir)?;
    let repo = Repository::discover(basedir).context(GitSnafu)?;
    if repo.workdir().is_some() {
        Ok(Box::into_raw(Box::new(repo)) as jlong)
    } else {
        Err(GitError::from_str("No workdir")).context(GitSnafu)
    }
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
    let ignored = repo.is_path_ignored(path).context(GitSnafu)?;
    Ok(ignored as jboolean)
}


/// # Safety
///
/// The caller must guarantee that the Object passed in is an instance
/// of `java.lang.String`, passing in anything else will lead to undefined behavior.
pub(crate) fn jstring_to_string(env: &mut JNIEnv, s: &JString) -> Result<String> {
    let res = unsafe { env.get_string_unchecked(s) }.context(JNISnafu)?;
    Ok(res.into())
}
