package exp;

import compressor.NaiveCompressor;
import utils.datagen.FamilyRelationGenerator;

import java.io.File;
import java.io.IOException;

public class NaiveWithFamilyRelation {
    private static void genData(double errorRate, int familyCnt, String dataPath) throws IOException  {
        System.out.printf("\n>>> Generating Data(Error: %f; Families: %d; Path)...\n", errorRate, familyCnt);
        FamilyRelationGenerator.ERROR_PROB = errorRate;
        FamilyRelationGenerator.FAMILY_CNT = familyCnt;
        File data_dir = new File(dataPath);
        if (!data_dir.exists()) {
            data_dir.mkdirs();
        }
        FamilyRelationGenerator.generateSimple(dataPath);
        FamilyRelationGenerator.generateMedium(dataPath);
    }

    private static void runAlg(String bkPath, String hypothesisPath) {
        System.out.println("\n>>> Running Algorithm...");
        NaiveCompressor compressor = new NaiveCompressor(bkPath, hypothesisPath);
        compressor.run();
    }

    public static void main(String[] args) throws IOException {
        System.out.println("==================================================");
        double error_rate = Double.parseDouble(args[0]);
        int family_cnt = Integer.parseInt(args[1]);
        String data_path = args[2];
        genData(error_rate, family_cnt, data_path);
        runAlg(
                String.format("%s/FamilyRelationSimple(%f)(%dx).tsv", data_path, error_rate, family_cnt),
                "HypothesisHumanSimple.amie"
        );
        System.out.println("-----");
        runAlg(
                String.format("%s/FamilyRelationMedium(%f)(%dx).tsv", data_path, error_rate, family_cnt),
                "HypothesisHumanMedium.amie"
        );
    }
}
