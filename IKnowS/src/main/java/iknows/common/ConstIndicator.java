package iknows.common;

public class ConstIndicator extends ArgIndicator{
    private static final int CONST_IDX = -1;

    public ConstIndicator(String name) {
        super(name, CONST_IDX);
    }
}
