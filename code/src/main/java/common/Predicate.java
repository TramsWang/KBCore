package common;

import java.util.Arrays;
import java.util.Objects;

public class Predicate {
    public final int id;
    public final int predicate;
    public final int[] arguments;
    public final boolean[] isVariable;

    public Predicate(int id, int predicate, int[] arguments, boolean[] isVariable) throws PredicateCreationException {
        if (arguments.length != isVariable.length) {
            throw new PredicateCreationException(
                    String.format(
                            "Argument list length(%d) does not match variable tag list length(%d)",
                            arguments.length, isVariable.length
                    )
            );
        }
        this.id = id;
        this.predicate = predicate;
        this.arguments = arguments;
        this.isVariable = isVariable;
    }

    public Predicate(Predicate another) {
        this.id = another.id;
        this.predicate = another.predicate;
        this.arguments = new int[another.arguments.length];
        this.isVariable = new boolean[another.isVariable.length];
        System.arraycopy(another.arguments, 0, this.arguments, 0, another.arguments.length);
        System.arraycopy(another.isVariable, 0, this.isVariable, 0, another.isVariable.length);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Predicate predicate1 = (Predicate) o;
        return predicate == predicate1.predicate &&
                Arrays.equals(arguments, predicate1.arguments) &&
                Arrays.equals(isVariable, predicate1.isVariable);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(predicate);
        result = 31 * result + Arrays.hashCode(arguments);
        result = 31 * result + Arrays.hashCode(isVariable);
        return result;
    }
}
