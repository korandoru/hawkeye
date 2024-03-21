use jni::JNIEnv;
use jni::objects::{JThrowable, JValue};
use snafu::Snafu;

#[derive(Snafu)]
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
