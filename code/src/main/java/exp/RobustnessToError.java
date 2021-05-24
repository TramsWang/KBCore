package exp;

import sinc.SInC;
import sinc.common.EvalMetric;
import sinc.impl.SincBasicWithJPL;
import sinc.util.datagen.FamilyRelationGenerator;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

public class RobustnessToError {
    static final String EXP_DIR = "exp";
    static final String DATA_FILE_TEMPLATE = "%s/%s(%.2f).tsv";
    static final String LOG_FILE_TEMPLATE = "%s/%s(%.2f).log";
    static final double ERROR_RATE_STEP = 0.05;
    static final int FAMILY_CNT = 10;

    static void genData() throws IOException {
        File exp_dir = new File(EXP_DIR);
        if (!exp_dir.exists()) {
            exp_dir.mkdirs();
        }

        for (double error_rate = 0.0; error_rate <= 1.0; error_rate += ERROR_RATE_STEP) {
            FamilyRelationGenerator.generateSimple(
                    String.format(DATA_FILE_TEMPLATE, EXP_DIR, "simple", error_rate),
                    FAMILY_CNT, error_rate
            );
            FamilyRelationGenerator.generateMedium(
                    String.format(DATA_FILE_TEMPLATE, EXP_DIR, "medium", error_rate),
                    FAMILY_CNT, error_rate
            );
        }
    }

    static void runTest() throws IOException {
        for (double error_rate = 0.0; error_rate <= 1.0; error_rate += ERROR_RATE_STEP) {
            System.out.printf("Testing: Error=%.2f\n", error_rate);
            SInC<?> sinc_4_simple = new SincBasicWithJPL(
                    1,
                    3,
                    EvalMetric.CompressionCapacity,
                    String.format(DATA_FILE_TEMPLATE, EXP_DIR, "simple", error_rate),
                    false
            );
            System.setOut(new PrintStream(new FileOutputStream(
                    String.format(LOG_FILE_TEMPLATE, EXP_DIR, "simple", error_rate)
            )));
            sinc_4_simple.run();

//            SInC<?> sinc_4_medium = new SincBasicWithJPL(
//                    1,
//                    3,
//                    EvalMetric.CompressionCapacity,
//                    String.format(DATA_FILE_TEMPLATE, EXP_DIR, "medium", error_rate),
//                    false
//            );
//            System.setOut(new PrintStream(new FileOutputStream(
//                    String.format(LOG_FILE_TEMPLATE, EXP_DIR, "medium", error_rate)
//            )));
//            sinc_4_medium.run();
        }
    }

    public static void main(String[] args) throws IOException {
        genData();
        runTest();
    }
}
