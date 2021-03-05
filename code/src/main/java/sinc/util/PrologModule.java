package sinc.util;

public enum PrologModule {
    GLOBAL("global"), CURRENT("current"), VALIDATION("validation");

    private final String sessionName;

    PrologModule(String sessionName) {
        this.sessionName = sessionName;
    }

    public String getSessionName() {
        return sessionName;
    }
}
