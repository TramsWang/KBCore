package iknows.util.amie;

import iknows.IknowsConfig;
import iknows.common.*;
import iknows.impl.cached.CachedQueryMonitor;
import iknows.impl.cached.recal.IknowsWithRecalculateCache;
import iknows.impl.cached.recal.RecalculateCachedRule;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class SumByAmieRules extends IknowsWithRecalculateCache {

    private static final String SINGLE_TARGET = "FAKE_TARGET";
    private static final int PREDICATE_ARITY = 2;
    private static final String HEAD_INDICATOR = "=>";

    protected final List<String> amieRules = new ArrayList<>();
    private int loadedRules = 0;
    private final AmieRuleMonitor amieMonitor = new AmieRuleMonitor();

    public SumByAmieRules(IknowsConfig config, String kbPath, String dumpPath, String logPath, String amieResultPath) {
        super(config, kbPath, dumpPath, logPath);
        loadAmieRules(amieResultPath);
        amieMonitor.totalRules = amieRules.size();
    }

    protected void loadAmieRules(String amieResultPath) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(amieResultPath));
            String line;
            while (null != (line = reader.readLine())) {
                if (line.startsWith("?")) {
                    amieRules.add(line);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected List<String> getTargetFunctors() {
        return new ArrayList<>(Collections.singletonList(SINGLE_TARGET));
    }

    @Override
    protected Rule findRule(String headFunctor) throws InterruptedSignal {
        while (loadedRules < amieRules.size()) {
            String amie_rule = amieRules.get(loadedRules);
            loadedRules++;

            /* Convert to Rule structure */
            List<Predicate> rule_structure = convertAmieRuleStructure(amie_rule);

            /* Monitor Rule Length */
            int rule_size = 0;
            Set<Integer> var_ids = new HashSet<>();
            for (Predicate p: rule_structure) {
                for (Argument argument: p.args) {
                    if (null != argument) {
                        if (argument.isVar) {
                            var_ids.add(argument.id);
                        }
                        rule_size++;
                    }
                }
            }
            rule_size -= var_ids.size();
            if (amieMonitor.totalRulesLengthDistribution.size() <= rule_size) {
                for (int i = amieMonitor.totalRulesLengthDistribution.size() - 1; i <= rule_size; i++) {
                    amieMonitor.totalRulesLengthDistribution.add(0);
                }
            }
            amieMonitor.totalRulesLengthDistribution.set(rule_size, amieMonitor.totalRulesLengthDistribution.get(rule_size) + 1);

            /* Construct CachedRule accordingly */
            RecalculateCachedRule rule = constructRuleByStructure(rule_structure);
            if (null == rule) {
                logger.println(amie_rule);
                continue;
            }

            /* Check usefulness */
            if (rule.getEval().useful(config.evalMetric)) {
                amieMonitor.usedRules++;

                /* Monitor Length */
                rule_size = rule.size();
                if (amieMonitor.usedRulesLengthDistribution.size() <= rule_size) {
                    for (int i = amieMonitor.usedRulesLengthDistribution.size() - 1; i <= rule_size; i++) {
                        amieMonitor.usedRulesLengthDistribution.add(0);
                    }
                }
                amieMonitor.usedRulesLengthDistribution.set(rule_size, amieMonitor.usedRulesLengthDistribution.get(rule_size) + 1);
                return rule;
            }
        }
        return null;
    }

    protected List<Predicate> convertAmieRuleStructure(String amieRule) {
        String[] components = amieRule.split("\\s+");
        Map<String, Variable> var_map = new HashMap<>();
        List<Predicate> rule_structure = new ArrayList<>(Collections.singleton(null));
        for (int i = 0; null == rule_structure.get(0) && i < components.length; i += 3) {
            if (HEAD_INDICATOR.equals(components[i])) {
                i++;
                Predicate predicate = new Predicate(components[i+1], PREDICATE_ARITY);
                predicate.args[0] = var_map.computeIfAbsent(components[i], k -> new Variable(var_map.size()));
                predicate.args[1] = var_map.computeIfAbsent(components[i+2], k -> new Variable(var_map.size()));
                rule_structure.set(0, predicate);
            } else {
                Predicate predicate = new Predicate(components[i + 1], PREDICATE_ARITY);
                predicate.args[0] = var_map.computeIfAbsent(components[i], k -> new Variable(var_map.size()));
                predicate.args[1] = var_map.computeIfAbsent(components[i + 2], k -> new Variable(var_map.size()));
                rule_structure.add(predicate);
            }
        }

        return rule_structure;
    }

    protected RecalculateCachedRule constructRuleByStructure(List<Predicate> ruleStructure) {
        /* Reorder predicates in rule */
        List<Predicate> new_rule_structure = new ArrayList<>(ruleStructure.size());
        new_rule_structure.add(ruleStructure.get(0));
        ruleStructure.remove(0);
        Set<Integer> visited_var_id = new HashSet<>();
        while (!ruleStructure.isEmpty()) {
            Argument unvisited_var = null;
            for (int pred_idx = 0; null == unvisited_var && pred_idx < new_rule_structure.size(); pred_idx++) {
                Predicate predicate = new_rule_structure.get(pred_idx);
                for (int arg_idx = 0; arg_idx < predicate.arity(); arg_idx++) {
                    Argument argument = predicate.args[arg_idx];
                    if (null != argument && argument.isVar && visited_var_id.add(argument.id)) {
                        /* Found an unvisited variable */
                        unvisited_var = argument;
                        break;
                    }
                }
            }
            if (null == unvisited_var) {
                /* Rule contains independent fragment */
                logger.print("Independent Fragment in: ");
                return null;
            }
            Iterator<Predicate> itr = ruleStructure.listIterator();
            while (itr.hasNext()) {
                Predicate predicate = itr.next();
                for (Argument argument: predicate.args) {
                    if (null != argument && argument.isVar && unvisited_var.id == argument.id) {
                        new_rule_structure.add(predicate);
                        itr.remove();
                    }
                }
            }
        }

        /* Construct rule */
        RecalculateCachedRule rule = new RecalculateCachedRule(new_rule_structure.get(0).functor, new HashSet<>(), kb);
        while (true) {
            Argument unhandled_var = null;
            for (int pred_idx = 0; null == unhandled_var && pred_idx < new_rule_structure.size(); pred_idx++) {
                Predicate predicate = new_rule_structure.get(pred_idx);
                for (int arg_idx = 0; arg_idx < predicate.arity(); arg_idx++) {
                    Argument argument = predicate.args[arg_idx];
                    if (null != argument && argument.isVar) {
                        /* Found a variable */
                        unhandled_var = argument;
                        break;
                    }
                }
            }
            if (null == unhandled_var) {
                /* All vars are constructed, find constants */
                for (int pred_idx = 0; pred_idx < new_rule_structure.size(); pred_idx++) {
                    Predicate predicate = new_rule_structure.get(pred_idx);
                    for (int arg_idx = 0; arg_idx < predicate.arity(); arg_idx++) {
                        Argument argument = predicate.args[arg_idx];
                        if (null != argument && !argument.isVar) {
                            /* Found a constant */
                            if (Rule.UpdateStatus.NORMAL != rule.boundFreeVar2Constant(
                                    pred_idx, arg_idx, argument.name
                            )) {
                                logger.print("Extension Operation (constant) Failed for: ");
                                return null;
                            }
                        }
                    }
                }
                return rule;
            } else {
                /* Find a new var */
                class VarPos {
                    final int predIdx;
                    final int argIdx;

                    public VarPos(int predIdx, int argIdx) {
                        this.predIdx = predIdx;
                        this.argIdx = argIdx;
                    }
                }

                /* Find all positions of the var */
                List<VarPos> positions = new ArrayList<>();
                for (int pred_idx = 0; pred_idx < new_rule_structure.size(); pred_idx++) {
                    Predicate predicate = new_rule_structure.get(pred_idx);
                    for (int arg_idx = 0; arg_idx < predicate.arity(); arg_idx++) {
                        Argument argument = predicate.args[arg_idx];
                        if (null != argument && argument.isVar && unhandled_var.id == argument.id) {
                            predicate.args[arg_idx] = null;
                            positions.add(new VarPos(pred_idx, arg_idx));
                        }
                    }
                }
                if (2 > positions.size()) {
                    /* Unlimited var, ignore */
                    continue;
                }

                /* Extend by new var */
                VarPos pos1 = positions.get(0);
                VarPos pos2 = positions.get(1);
                if (pos2.predIdx >= rule.length()) {
                    if (Rule.UpdateStatus.NORMAL != rule.boundFreeVars2NewVar(
                            new_rule_structure.get(pos2.predIdx).functor, PREDICATE_ARITY, pos2.argIdx,
                            pos1.predIdx, pos1.argIdx
                    )) {
                        logger.print("Extension Operation (new var in new pred) Failed for: ");
                        return null;
                    }
                } else {
                    if (Rule.UpdateStatus.NORMAL != rule.boundFreeVars2NewVar(
                            pos1.predIdx, pos1.argIdx, pos2.predIdx, pos2.argIdx
                    )) {
                        logger.print("Extension Operation (new var in existing pred) Failed for: ");
                        return null;
                    }
                }

                /* Extend by existing var */
                int rule_var_id = rule.getPredicate(pos1.predIdx).args[pos1.argIdx].id;
                for (int i = 2; i < positions.size(); i++) {
                    VarPos position = positions.get(i);
                    if (position.predIdx >= rule.length()) {
                        if (Rule.UpdateStatus.NORMAL != rule.boundFreeVar2ExistingVar(
                                new_rule_structure.get(position.predIdx).functor, PREDICATE_ARITY, position.argIdx,
                                rule_var_id
                        )) {
                            logger.print("Extension Operation (existing var in new pred) Failed for: ");
                            return null;
                        }
                    } else {
                        if (Rule.UpdateStatus.NORMAL != rule.boundFreeVar2ExistingVar(
                                position.predIdx, position.argIdx, rule_var_id
                        )) {
                            logger.print("Extension Operation (existing var in existing pred) Failed for: ");
                            return null;
                        }
                    }
                }
            }
        }
    }

    @Override
    protected Rule getStartRule(String headFunctor, Set<RuleFingerPrint> cache) {
        return super.getStartRule(headFunctor, cache);
    }

    @Override
    protected KbStatistics loadKb() {
        return super.loadKb();
    }

    @Override
    protected void showMonitor() {
        super.cacheMonitor.cacheStats.add(new CachedQueryMonitor.CacheStat(0, 0, 0));
        super.showMonitor();
        amieMonitor.show(logger);
    }
}
