package sinc.exp;

import sinc.SincConfig;
import sinc.common.Eval;
import sinc.impl.basic.SincWithJpl;
import sinc.impl.cached.recal.SincWithRecalculateCache;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;

public class TestFactOverage {
    public static void main(String[] args) {
        final String MODEL = "Cr";

        final String purpose = "Test Fact Coverage";
        final int beam_width = 2;
        final Dataset dataset = Dataset.FAMILY_MEDIUM;
        final Eval.EvalMetric eval_metric = Eval.EvalMetric.CompressionCapacity;

        for (double fc = 0; fc <= 0.05; fc += 0.01) {
            File dir = new File(String.format("%s/%s", purpose, dataset.getName()));
            if (!dir.exists() && !dir.mkdirs()) {
                System.err.printf("Dir Make Failed: %s/%s\n", purpose, dataset.getName());
                return;
            }

            System.out.printf(
                    "%5s %3d %3s\n",
                    dataset.getShortName(),
                    beam_width,
                    eval_metric.getName()
            );

            final SincConfig config = new SincConfig(
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
                    "%s/%s/%s_%s_%.2f.stdout",
                    purpose, dataset.getName(), MODEL, eval_metric.getName(), fc
            );
            final String stderr_path = String.format(
                    "%s/%s/%s_%s_%.2f.stderr",
                    purpose, dataset.getName(), MODEL, eval_metric.getName(), fc
            );
            final String dump_path = String.format(
                    "%s/%s/%s_%s_%.2f.result",
                    purpose, dataset.getName(), MODEL, eval_metric.getName(), fc
            );
            final String log_path = String.format(
                    "%s/%s/%s_%s_%.2f.log",
                    purpose, dataset.getName(), MODEL, eval_metric.getName(), fc
            );

            PrintStream original_out = System.out;
            PrintStream original_err = System.err;
            try {
                PrintStream ps_out = new PrintStream(new FileOutputStream(stdout_path));
                PrintStream ps_err = new PrintStream(new FileOutputStream(stderr_path));
                System.setOut(ps_out);
                System.setErr(ps_err);
                SincWithRecalculateCache sinc = new SincWithRecalculateCache(
                        config, dataset.getPath(), dump_path, log_path
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
