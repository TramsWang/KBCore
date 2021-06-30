package sinc.exp;

import sinc.SincConfig;
import sinc.common.Eval;
import sinc.impl.cached.recal.SincWithRecalculateCache;
import sinc.util.datagen.FamilyRelationGenerator;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;

public class TestDataQuality {
    public static void main(String[] args) {
        final double MIN_ERROR = 0.0;
        final double MAX_ERROR = 0.3;
        final double ERROR_STEP = 0.05;
        final int FAMILIES = 10;

        final String PURPOSE = "Test Quality";
        final String MODEL = "Cr";
        final int BEAM_WIDTH = 5;
        final Eval.EvalMetric eval_metric = Eval.EvalMetric.CompressionCapacity;

        File dir = new File(PURPOSE);
        if (!dir.exists() && !dir.mkdirs()) {
            System.err.printf("Dir Make Failed: %s/\n", PURPOSE);
            return;
        }

        for (double error = MIN_ERROR; error <= MAX_ERROR; error += ERROR_STEP) {
            final String dataset_path = String.format("%s/Fm_%d_%.2f.tsv", PURPOSE, FAMILIES, error);
            try {
                FamilyRelationGenerator.generateMedium(dataset_path, FAMILIES, error);
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }

            System.out.printf(
                    "%.2f %s\n",
                    error,
                    eval_metric.getName()
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
                    "%s/%s_%s_%.2f.stdout",
                    PURPOSE, MODEL, eval_metric.getName(), error
            );
            final String stderr_path = String.format(
                    "%s/%s_%s_%.2f.stderr",
                    PURPOSE, MODEL, eval_metric.getName(), error
            );
            final String dump_path = String.format(
                    "%s/%s_%s_%.2f.result",
                    PURPOSE, MODEL, eval_metric.getName(), error
            );
            final String log_path = String.format(
                    "%s/%s_%s_%.2f.log",
                    PURPOSE, MODEL, eval_metric.getName(), error
            );

            PrintStream original_out = System.out;
            PrintStream original_err = System.err;
            try {
                PrintStream ps_out = new PrintStream(new FileOutputStream(stdout_path));
                PrintStream ps_err = new PrintStream(new FileOutputStream(stderr_path));
                System.setOut(ps_out);
                System.setErr(ps_err);
                SincWithRecalculateCache sinc = new SincWithRecalculateCache(
                        config, dataset_path, dump_path, log_path
                );
                sinc.run();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                System.setOut(original_out);
                System.setErr(original_err);
            }
        }
    }
}
