package com.tencent.bk.job.api.exception;

public class TestInitialException extends RuntimeException {
    public TestInitialException() {
    }

    public TestInitialException(String message) {
        super(message);
    }

    public TestInitialException(String message, Throwable cause) {
        super(message, cause);
    }

    public TestInitialException(Throwable cause) {
        super(cause);
    }
}
