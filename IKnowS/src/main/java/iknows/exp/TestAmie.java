package iknows.exp;

import iknows.IknowsConfig;
import iknows.common.Dataset;
import iknows.common.Eval;
import iknows.impl.cached.recal.IknowsWithRecalculateCache;
import iknows.util.amie.SumByAmieRules;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;

public class TestAmie {
    public static void main(String[] args) {
        final Dataset dataset = Dataset.getByShortName(args[0]);
        final String MODEL = "Cr";
        final String PURPOSE = "AMIE";
        final int beam_width = 0;
        final Eval.EvalMetric eval_metric = Eval.EvalMetric.CompressionCapacity;

        File dir = new File(String.format("%s/%s", PURPOSE, dataset.getName()));
        if (!dir.exists() && !dir.mkdirs()) {
            System.err.printf("Dir Make Failed: %s/%s\n", PURPOSE, dataset.getName());
            return;
        }

        System.out.printf(
                "%5s %3s\n",
                PURPOSE,
                dataset.getShortName()
        );

        final IknowsConfig config = new IknowsConfig(
                1,
                false,
                false,
                beam_width,
                false,
                eval_metric,
                -1,
                1,
                false,
                -1.0,
                false,
                false
        );
        final String stdout_path = String.format(
                "%s/%s/%s_%s_%d.stdout",
                PURPOSE, dataset.getName(), MODEL, eval_metric.getName(), beam_width
        );
        final String stderr_path = String.format(
                "%s/%s/%s_%s_%d.stderr",
                PURPOSE, dataset.getName(), MODEL, eval_metric.getName(), beam_width
        );
        final String dump_path = String.format(
                "%s/%s/%s_%s_%d.result",
                PURPOSE, dataset.getName(), MODEL, eval_metric.getName(), beam_width
        );
        final String log_path = String.format(
                "%s/%s/%s_%s_%d.log",
                PURPOSE, dataset.getName(), MODEL, eval_metric.getName(), beam_width
        );
        final String amie_result_path = String.format(
                "../datasets/amie/%s.amie", dataset.getName()
        );

        PrintStream original_out = System.out;
        PrintStream original_err = System.err;
        try {
            PrintStream ps_out = new PrintStream(new FileOutputStream(stdout_path));
            PrintStream ps_err = new PrintStream(new FileOutputStream(stderr_path));
            System.setOut(ps_out);
            System.setErr(ps_err);
            IknowsWithRecalculateCache iknows = new SumByAmieRules(
                    config, dataset.getPath(), dump_path, log_path, amie_result_path
            );
            iknows.run();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            System.setOut(original_out);
            System.setErr(original_err);
        }
    }
}
