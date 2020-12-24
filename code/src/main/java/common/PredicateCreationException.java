package common;

public class PredicateCreationException extends Exception {
    public PredicateCreationException() {
    }

    public PredicateCreationException(String s) {
        super(s);
    }

    public PredicateCreationException(String s, Throwable throwable) {
        super(s, throwable);
    }

    public PredicateCreationException(Throwable throwable) {
        super(throwable);
    }
}
