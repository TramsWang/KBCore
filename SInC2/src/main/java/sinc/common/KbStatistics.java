package sinc.common;

public class KbStatistics {
    public final int facts;
    public final int functors;
    public final int actualConstantSubstitutions;
    public final int totalConstantSubstitutions;

    public KbStatistics(int facts, int functors, int actualConstantSubstitutions, int totalConstantSubstitutions) {
        this.facts = facts;
        this.functors = functors;
        this.actualConstantSubstitutions = actualConstantSubstitutions;
        this.totalConstantSubstitutions = totalConstantSubstitutions;
    }
}
