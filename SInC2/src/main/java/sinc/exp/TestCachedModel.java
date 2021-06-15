package sinc.exp;

import sinc.SincConfig;
import sinc.common.Eval;
import sinc.impl.cached.SincWithCachedRule;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;

public class TestCachedModel {
    public static void main(String[] args) {
        final String MODEL = "cached";

        final String purpose = args[0];
        final int beam_width = Integer.parseInt(args[1]);
        final Eval.EvalMetric eval_metric = Eval.EvalMetric.getByName(args[2]);
        final Dataset dataset = Dataset.getByShortName(args[3]);

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
                "%s/%s/%s_%s_%d.stdout",
                purpose, dataset.getName(), MODEL, eval_metric.getName(), beam_width
        );
        final String stderr_path = String.format(
                "%s/%s/%s_%s_%d.stderr",
                purpose, dataset.getName(), MODEL, eval_metric.getName(), beam_width
        );
        final String dump_path = String.format(
                "%s/%s/%s_%s_%d.result",
                purpose, dataset.getName(), MODEL, eval_metric.getName(), beam_width
        );

        PrintStream original_out = System.out;
        PrintStream original_err = System.err;
        try {
            PrintStream ps_out = new PrintStream(new FileOutputStream(stdout_path));
            PrintStream ps_err = new PrintStream(new FileOutputStream(stderr_path));
            System.setOut(ps_out);
            System.setErr(ps_err);
            SincWithCachedRule sinc = new SincWithCachedRule(
                    config, dataset.getPath(), dump_path
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
