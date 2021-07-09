package iknows.exp;

import iknows.IknowsConfig;
import iknows.common.Eval;
import iknows.impl.basic.IknowsWithJpl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;

public class TestBasicModel {
    static void testBeam(int beam_width, Eval.EvalMetric eval_metric, Dataset dataset) {
        final String PURPOSE = "ORIGIN";
        final String MODEL = "cached";
        
        File dir = new File(String.format("%s/%s", PURPOSE, dataset.getName()));
        if (!dir.exists() && !dir.mkdirs()) {
            System.err.printf("Dir Make Failed: %s/%s\n", PURPOSE, dataset.getName());
            return;
        }
        final IknowsConfig config = new IknowsConfig(
                1,
                false,
                false,
                beam_width,
                true,
                eval_metric,
                0.05,
                0.25,
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

        try {
            PrintStream ps_out = new PrintStream(new FileOutputStream(stdout_path));
            PrintStream ps_err = new PrintStream(new FileOutputStream(stderr_path));
            System.setOut(ps_out);
            System.setErr(ps_err);
            IknowsWithJpl sinc = new IknowsWithJpl(
                    config, dataset.getPath(), dump_path, log_path
            );
            sinc.run();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        final int beam_width = Integer.parseInt(args[0]);
        final Eval.EvalMetric metric = Eval.EvalMetric.getByName(args[1]);
        final Dataset dataset = Dataset.getByShortName(args[2]);
        testBeam(beam_width, metric, dataset);
    }
}
