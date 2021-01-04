package compressor;

import common.JplRule;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CompressorWithMultisetStaticRuleConstructionTest {
    @Test
    public void testRuleComposer() {
        Map<String, Integer> pred_2_arg_cnt_map = new HashMap<>();
        pred_2_arg_cnt_map.put("p", 2);
        pred_2_arg_cnt_map.put("q", 2);
        pred_2_arg_cnt_map.put("r", 3);
        pred_2_arg_cnt_map.put("w", 2);
        CompressorWithMultisetStaticRuleConstruction.RuleComposer composer = new CompressorWithMultisetStaticRuleConstruction.RuleComposer(
                "p", pred_2_arg_cnt_map
        );
        composer.linkArg(0, "q", 0);
        composer.linkArg(1, "q", 1);
        composer.linkArg(0, "w", 1);
//        composer.linkArg(1, "r", 1);
        List<JplRule> rules = composer.compose();
        assertNotNull(rules);
        for (JplRule rule: rules) {
            System.out.println(rule);
        }
    }
}