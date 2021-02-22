package sinc.common;

public abstract class ArgIndicator {
    public final String functor;
    public final int idx;

    public ArgIndicator(String functor, int idx) {
        this.functor = functor;
        this.idx = idx;
    }

    public ArgIndicator(ArgIndicator another) {
        this.functor = another.functor;
        this.idx = another.idx;
    }
}
