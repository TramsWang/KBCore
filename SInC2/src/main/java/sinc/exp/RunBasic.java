package sinc.exp;

import sinc.SincConfig;
import sinc.common.Eval;
import sinc.impl.basic.SincWithJpl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;

public class RunBasic {
    static void testBeam(int beam_width, Eval.EvalMetric eval_metric, Dataset dataset) {
        File dir = new File(String.format("%s/%s", "Beam", dataset.getName()));
        if (!dir.exists() && !dir.mkdirs()) {
            System.err.printf("Dir Make Failed: %s/%s\n", "Beam", dataset.getName());
            return;
        }
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
                "Beam", dataset.getName(), "basic", eval_metric.getName(), beam_width
        );
        final String stderr_path = String.format(
                "%s/%s/%s_%s_%d.stderr",
                "Beam", dataset.getName(), "basic", eval_metric.getName(), beam_width
        );
        final String dump_path = String.format(
                "%s/%s/%s_%s_%d.result",
                "Beam", dataset.getName(), "basic", eval_metric.getName(), beam_width
        );

        try {
            PrintStream ps_out = new PrintStream(new FileOutputStream(stdout_path));
            PrintStream ps_err = new PrintStream(new FileOutputStream(stderr_path));
            System.setOut(ps_out);
            System.setErr(ps_err);
            SincWithJpl sinc = new SincWithJpl(
                    config, dataset.getPath(), dump_path
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
