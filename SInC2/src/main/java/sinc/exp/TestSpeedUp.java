package sinc.exp;

import sinc.SInC;
import sinc.SincConfig;
import sinc.common.Eval;
import sinc.impl.basic.SincWithJpl;
import sinc.impl.cached.recal.SincWithRecalculateCache;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;

public class TestSpeedUp {
    public static void main(String[] args) {
        final String PURPOSE = "Test Speed Up";
        final int BEAM_WIDTH = 5;
        final Eval.EvalMetric eval_metric = Eval.EvalMetric.CompressionCapacity;

        File dir = new File(PURPOSE);
        if (!dir.exists() && !dir.mkdirs()) {
            System.err.printf("Dir Make Failed: %s/\n", PURPOSE);
            return;
        }

        final String MODEL = args[0];
        final int FAMILIES = Integer.parseInt(args[1]);
        final String DATASET_PATH = String.format("%s/Fm_%d.tsv", PURPOSE, FAMILIES);

        System.out.printf(
                "%s %d\n",
                MODEL,
                FAMILIES
        );

        final SincConfig config = new SincConfig(
                1,
                false,
                false,
                BEAM_WIDTH,
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
                "%s/%s_%s_%d.stdout",
                PURPOSE, MODEL, eval_metric.getName(), FAMILIES
        );
        final String stderr_path = String.format(
                "%s/%s_%s_%d.stderr",
                PURPOSE, MODEL, eval_metric.getName(), FAMILIES
        );
        final String dump_path = String.format(
                "%s/%s_%s_%d.result",
                PURPOSE, MODEL, eval_metric.getName(), FAMILIES
        );

        PrintStream original_out = System.out;
        PrintStream original_err = System.err;
        try {
            PrintStream ps_out = new PrintStream(new FileOutputStream(stdout_path));
            PrintStream ps_err = new PrintStream(new FileOutputStream(stderr_path));
            System.setOut(ps_out);
            System.setErr(ps_err);
            switch (MODEL) {
                case "basic": {
                    SInC sinc = new SincWithJpl(
                            config, DATASET_PATH, dump_path
                    );
                    sinc.run();
                    break;
                }
                case "Cr": {
                    SInC sinc = new SincWithRecalculateCache(
                            config, DATASET_PATH, dump_path
                    );
                    sinc.run();
                    break;
                }
                default:
                    throw new Exception("Unknown Model: " + MODEL);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            System.setOut(original_out);
            System.setErr(original_err);
        }
    }
}
