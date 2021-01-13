package compressor.estimation.condprob;

import java.util.Objects;

public class ArgInfo {
    final String name;
    final ArgType type;

    public ArgInfo(String name, ArgType type) {
        this.name = name;
        this.type = type;
    }

    public ArgInfo(ArgInfo another) {
        this.name = another.name;
        this.type = another.type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ArgInfo argInfo = (ArgInfo) o;
        return Objects.equals(name, argInfo.name) && type == argInfo.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, type);
    }
}
