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
