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

use jni::JNIEnv;
use jni::objects::{JThrowable, JValue};
use snafu::Snafu;

#[derive(Debug, Snafu)]
#[snafu(visibility(pub))]
pub enum Error {
    #[snafu(display("Git operation failed."))]
    GitError {
        #[snafu(source)]
        source: git2::Error,
    },
    #[snafu(display("Git operation failed."))]
    JNIError {
        #[snafu(source)]
        source: jni::errors::Error,
    },
}

impl Error {
    pub(crate) fn throw(&self, env: &mut JNIEnv) {
        if let Err(err) = self.do_throw(env) {
            match err {
                jni::errors::Error::JavaException => {
                    // other calls throws exception; safely ignored
                }
                _ => env.fatal_error(err.to_string()),
            }
        }
    }

    pub(crate) fn to_exception<'local>(
        &self,
        env: &mut JNIEnv<'local>,
    ) -> jni::errors::Result<JThrowable<'local>> {
        let class = env.find_class("io/korandoru/hawkeye/core/rust/ResultException")?;
        let code = env.new_string(match self {
            Error::GitError { .. } => "GitError",
            Error::JNIError { .. } => "JNIError",
        })?;
        let message = env.new_string(format!("{}", self.to_string()))?;
        let exception = env.new_object(
            class,
            "(Ljava/lang/String;Ljava/lang/String;)V",
            &[JValue::Object(&code), JValue::Object(&message)],
        )?;
        Ok(JThrowable::from(exception))
    }

    fn do_throw(&self, env: &mut JNIEnv) -> jni::errors::Result<()> {
        let exception = self.to_exception(env)?;
        env.throw(exception)
    }
}
