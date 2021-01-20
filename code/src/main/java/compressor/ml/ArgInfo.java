package compressor.ml;

public class ArgInfo {
    public final int id;
    public final String name;
    public final ArgType type;

    public ArgInfo(int id, ArgType type) {
        this.id = id;
        this.name = String.format("X%d", id);
        this.type = type;
    }

    public ArgInfo(ArgInfo another) {
        this.id = another.id;
        this.name = another.name;
        this.type = another.type;
    }
}
