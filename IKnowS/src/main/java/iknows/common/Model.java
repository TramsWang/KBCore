package iknows.common;

import iknows.IKnowS;
import iknows.IknowsConfig;
import iknows.impl.basic.IknowsWithJpl;
import iknows.impl.cached.recal.IknowsWithRecalculateCache;
import iknows.impl.cached.spec.IknowsWithSpecificCache;

import java.util.HashMap;
import java.util.Map;

public enum Model {
    JPL("J", "IKnowS with JPL"),
    CACHE_COMPACT("C", "IKnowS with compact cache"),
    CACHE_MATERIALIZED("M", "IKnowS with materialized cache");

    static final Map<String, Model> nameMap = new HashMap<>();
    static {
        for (Model model: Model.values()) {
            nameMap.put(model.name, model);
        }
    }

    private final String name;
    private final String description;

    Model(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public static Model getModelByName(String name) {
        return nameMap.get(name);
    }

    public static IKnowS getModel(String name, IknowsConfig config, String dataPath, String dumpPath, String logPath) throws IknowsException {
        switch (getModelByName(name)) {
            case CACHE_COMPACT:
                return new IknowsWithRecalculateCache(config, dataPath, dumpPath, logPath);
            case CACHE_MATERIALIZED:
                return new IknowsWithSpecificCache(config, dataPath, dumpPath, logPath);
            case JPL:
                return new IknowsWithJpl(config, dataPath, dumpPath, logPath);
            default:
                throw new IknowsException("Unknown Model: " + name);
        }
    }
}
