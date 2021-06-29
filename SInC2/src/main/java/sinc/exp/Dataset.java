package sinc.exp;

import sinc.common.Eval;

import java.util.HashMap;
import java.util.Map;

public enum Dataset {
    ELTI("../datasets/elti.tsv", "Elti", "E"),
    DUNUR("../datasets/dunur.tsv", "Dunur", "D"),
    STUDENT_LOAN("../datasets/student_loan.tsv", "Student Loan", "S"),
    DBPEDIA_FACTBOOK("../datasets/dbpedia_factbook.tsv", "DBpedia.factbook", "DBf"),
    DBPEDIA_LOBIDORG("../datasets/dbpedia_lobidorg.tsv", "DBpedia.lobidorg", "DBl"),
    WEBKB_CORNELL("../datasets/webkb.cornell.tsv", "WebKB.Cornell", "WKc"),
    WEBKB_TEXAS("../datasets/webkb.texas.tsv", "WebKB.Texas", "WKt"),
    WEBKB_WASHINGTON("../datasets/webkb.washington.tsv", "WebKB.Washington", "WKw"),
    WEBKB_WISCONSIN("../datasets/webkb.wisconsin.tsv", "WebKB.Wisconsin", "WKi"),
    NELL("../datasets/nell.tsv", "NELL", "N"),
    FAMILY_SIMPLE("../datasets/family_simple.tsv", "Family.simple", "Fs"),
    FAMILY_MEDIUM("../datasets/family_medium.tsv", "Family.medium", "Fm")
    ;
    private final String path;
    private final String name;
    private final String shortName;

    private static final Map<String, Dataset> shortName2ValMap = new HashMap<>();
    static {
        for (Dataset dataset: Dataset.values()) {
            shortName2ValMap.put(dataset.shortName, dataset);
        }
    }

    Dataset(String path, String name, String shortName) {
        this.path = path;
        this.name = name;
        this.shortName = shortName;
    }

    public String getPath() {
        return path;
    }

    public String getName() {
        return name;
    }

    public String getShortName() {
        return shortName;
    }

    static public Dataset getByShortName(String shortName) {
        return shortName2ValMap.get(shortName);
    }
}
