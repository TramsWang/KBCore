package compressor.multiset.dynamic;

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
}
