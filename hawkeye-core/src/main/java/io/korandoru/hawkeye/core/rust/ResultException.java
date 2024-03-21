/*
 * Copyright 2023 Korandoru Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.korandoru.hawkeye.core.rust;

import lombok.Getter;

/**
 * An exception encapsulates the error of a native operation. This exception
 * type is used to describe an error from the native opendal library.
 */
@Getter
public class ResultException extends RuntimeException {
    /**
     * Error code returned from the native library.
     */
    private final Code code;

    /**
     * Construct an ResultException. This constructor is called from native code.
     *
     * @param code    string representation of the error code
     * @param message error message
     */
    @SuppressWarnings("unused")
    public ResultException(String code, String message) {
        this(Code.valueOf(code), message);
    }

    public ResultException(Code code, String message) {
        super(message);
        this.code = code;
    }

    /**
     * Enumerate all kinds of Error that the native library may return.
     */
    public enum Code {
        GitError,
        JNIError,
    }
}
