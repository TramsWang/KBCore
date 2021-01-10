package compressor.multiset.dynamic;

import java.util.Arrays;

public class PredInfo {
    final String predicate;
    final ArgInfo[] args;

    public PredInfo(String predicate, int arity) {
        this.predicate = predicate;
        args = new ArgInfo[arity];
    }

    public PredInfo(PredInfo another) {
        this.predicate = another.predicate;
        this.args = new ArgInfo[another.args.length];
        for (int i = 0; i < args.length; i++) {
            this.args[i] = (null == another.args[i]) ? null : new ArgInfo(another.args[i]);
        }
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(predicate);
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
