package sinc.common;

public class Predicate {
    final String functor;
    final Argument[] args;

    public Predicate(String functor, int arity) {
        this.functor = functor;
        args = new Argument[arity];
    }

    public Predicate(Predicate another) {
        this.functor = another.functor;
        this.args = another.args.clone();
    }

    public int arity() {
        return args.length;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(functor);
        builder.append('(');
        if (0 < args.length) {
            builder.append((null == args[0]) ? "?" : args[0].name);
            for (int i = 1; i < args.length; i++) {
                builder.append(',').append((null == args[i]) ? "?" : args[i].name);
            }
        }
        builder.append(')');
        return builder.toString();
    }
}
