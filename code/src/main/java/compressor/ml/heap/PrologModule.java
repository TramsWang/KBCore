package compressor.ml.heap;

public enum PrologModule {
    GLOBAL("global"), CURRENT("current"), START_SET("start_set");

    private final String sessionName;

    PrologModule(String sessionName) {
        this.sessionName = sessionName;
    }

    public String getSessionName() {
        return sessionName;
    }
}
