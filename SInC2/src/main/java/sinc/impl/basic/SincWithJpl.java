package sinc.impl.basic;

import sinc.SInC;
import sinc.SincConfig;
import sinc.common.*;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SincWithJpl extends SInC {

    private final PrologKb kb = new PrologKb();

    public SincWithJpl(SincConfig config, String bkPath, String dumpPath) {
        super(
                new SincConfig(
                        config.threads,
                        config.validation,
                        config.debug,
                        config.beamWidth,
                        config.searchOrigins,
                        config.evalMetric,
                        config.minHeadCoverage,
                        config.minConstantProportion,
                        false,
                        -1.0,
                        false,
                        false
                ),
                bkPath,
                dumpPath
        );
    }

    @Override
    protected int loadKb() {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(kbPath));
            String line;
            while (null != (line = reader.readLine())) {
                final String[] components = line.split("\t");
                final Predicate predicate = new Predicate(components[0], components.length - 1);
                for (int i = 1; i < components.length; i++) {
                    predicate.args[i - 1] = new Constant(CONST_ID, components[i]);
                }
                kb.addFact(predicate);
            }
            kb.calculatePromisingConstants(config.minConstantProportion);

            return kb.totalFacts();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return -1;
    }

    @Override
    protected List<String> getTargetFunctors() {
        return kb.getAllFunctors();
    }

    @Override
    protected Rule getStartRule(String headFunctor, Set<RuleFingerPrint> cache) {
        return new JplRule(headFunctor, kb.getArity(headFunctor), cache, kb);
    }

    @Override
    protected Map<String, Integer> getFunctor2ArityMap() {
        return kb.getFunctor2ArityMap();
    }

    @Override
    protected Map<String, List<String>[]> getFunctor2PromisingConstantMap() {
        return kb.getFunctor2PromisingConstantMap();
    }

    @Override
    protected UpdateResult updateKb(Rule rule) {
        JplRule jpl_rule = (JplRule) rule;
        return jpl_rule.updateInKb();
    }

    @Override
    protected Set<Predicate> getOriginalKb() {
        return kb.originalKb;
    }
}
