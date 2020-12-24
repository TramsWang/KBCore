package common;

public class Fact extends Predicate {

    public Fact(int id, int predicate, int... arguments) throws PredicateCreationException {
        super(id, predicate, arguments, new boolean[arguments.length]);
    }

    public Fact(Fact another) {
        super(another);
    }
}
