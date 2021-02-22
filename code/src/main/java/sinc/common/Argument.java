package sinc.common;

public abstract class Argument {
    public final int id;
    public final String name;
    public final boolean isVar;

    public Argument(int id, String name, boolean isVar) {
        this.id = id;
        this.name = name;
        this.isVar = isVar;
    }

    public Argument(Argument another) {
        this.id = another.id;
        this.name = another.name;
        this.isVar = another.isVar;
    }
}
