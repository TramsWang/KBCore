package iknows.common;

public class IknowsException extends Exception {
    public IknowsException() {
    }

    public IknowsException(String message) {
        super(message);
    }

    public IknowsException(String message, Throwable cause) {
        super(message, cause);
    }

    public IknowsException(Throwable cause) {
        super(cause);
    }

    public IknowsException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
