package sinc.exp;

import sinc.SincConfig;
import sinc.common.Eval;
import sinc.impl.cached.recal.SincWithRecalculateCache;
import sinc.util.datagen.FamilyRelationGenerator;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;

public class TestScalability {
    public static void main(String[] args) {
        final double ERROR = 0.0;
        final int[] FAMILIES_LIST = new int[]{
                1, 10, 50, 100, 150, 200
        };

        final String PURPOSE = "Test Scalability";
        final String MODEL = "Cr";
        final int BEAM_WIDTH = 3;
        final Eval.EvalMetric eval_metric = Eval.EvalMetric.CompressionCapacity;

        File dir = new File(PURPOSE);
        if (!dir.exists() && !dir.mkdirs()) {
            System.err.printf("Dir Make Failed: %s/\n", PURPOSE);
            return;
        }

        for (int families: FAMILIES_LIST) {
            final String dataset_path = String.format("%s/Fm_%d.tsv", PURPOSE, families);

            try {
                FamilyRelationGenerator.generateMedium(dataset_path, families, ERROR);
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }

            System.out.printf(
                    "%d %s\n",
                    families,
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
                    "%s/%s_%s_%d.stdout",
                    PURPOSE, MODEL, eval_metric.getName(), families
            );
            final String stderr_path = String.format(
                    "%s/%s_%s_%d.stderr",
                    PURPOSE, MODEL, eval_metric.getName(), families
            );
            final String dump_path = String.format(
                    "%s/%s_%s_%d.result",
                    PURPOSE, MODEL, eval_metric.getName(), families
            );
            final String log_path = String.format(
                    "%s/%s_%s_%d.log",
                    PURPOSE, MODEL, eval_metric.getName(), families
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
