package exp;

import compressor.NaiveCompressor;
import utils.datagen.FamilyRelationGenerator;

import java.io.File;
import java.io.IOException;

public class NaiveWithFamilyRelation {
    private static void genData(double errorRate, int familyCnt, String dataPath, String suffix) throws IOException  {
        System.out.printf(
                "\n>>> Generating Data[%s](Error: %f; Families: %d; Path: %s)...\n",
                suffix, errorRate, familyCnt, dataPath
        );
        FamilyRelationGenerator.ERROR_PROB = errorRate;
        FamilyRelationGenerator.FAMILY_CNT = familyCnt;
        File data_dir = new File(dataPath);
        if (!data_dir.exists()) {
            data_dir.mkdirs();
        }
        switch (suffix) {
            case "Simple":
                FamilyRelationGenerator.generateSimple(dataPath);
                break;
            case "Medium":
                FamilyRelationGenerator.generateMedium(dataPath);
                break;
            default:
                throw new IOException("Wrong suffix: " + suffix);
        }
    }

    private static void runAlg(String bkPath, String hypothesisPath) {
        System.out.println("\n>>> Running Algorithm...");
        NaiveCompressor compressor = new NaiveCompressor(bkPath, hypothesisPath, false);
        compressor.run();
    }

    public static void main(String[] args) throws IOException {
        System.out.println("==================================================");
        double error_rate = Double.parseDouble(args[0]);
        int family_cnt = Integer.parseInt(args[1]);
        String data_path = args[2];
        String suffix = args[3];
        genData(error_rate, family_cnt, data_path, suffix);
        runAlg(
                String.format("%s/FamilyRelation%s(%f)(%dx).tsv", data_path, suffix, error_rate, family_cnt),
                String.format("HypothesisHuman%s.amie", suffix)
        );
    }
}
