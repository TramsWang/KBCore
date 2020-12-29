package exp;

import common.JplRule;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jpl7.Compound;
import org.jpl7.Term;
import utils.AmieRuleLoader;
import utils.MultiSet;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class ObserveArgumentCorrelation {

    static class PredAndArgIdx {
        final String pred;
        final int argIdx;

        public PredAndArgIdx(String pred, int argIdx) {
            this.pred = pred;
            this.argIdx = argIdx;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PredAndArgIdx that = (PredAndArgIdx) o;
            return argIdx == that.argIdx && Objects.equals(pred, that.pred);
        }

        @Override
        public int hashCode() {
            return Objects.hash(pred, argIdx);
        }
    }

    static class FormulaIdxAndVariable {
        int formulaIdx;
        String varName;

        public FormulaIdxAndVariable(int formulaIdx, String varName) {
            this.formulaIdx = formulaIdx;
            this.varName = varName;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            FormulaIdxAndVariable that = (FormulaIdxAndVariable) o;
            return formulaIdx == that.formulaIdx && Objects.equals(varName, that.varName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(formulaIdx, varName);
        }
    }

    private final String bkPath;
    private final String hypothesisPath;
    private final Map<String, MultiSet<String>[]> predicate2ArgumentSetMap = new HashMap<>();
    private final List<JplRule> rules = new ArrayList<>();
    private final Map<PredAndArgIdx, Set<FormulaIdxAndVariable>> predArg2FormulaVarMap = new HashMap<>();
    private final XSSFWorkbook workbook = new XSSFWorkbook();
    private final XSSFSheet sheetMatrix = workbook.createSheet("matrix");
    private final XSSFSheet sheetSortedList = workbook.createSheet("sortedList");

    public ObserveArgumentCorrelation(String bkPath, String hypothesisPath) {
        this.bkPath = bkPath;
        this.hypothesisPath = hypothesisPath;
    }

    /**
     * BK format(each line):
     * [pred]\t[arg1]\t[arg2]\t...\t[argn]
     */
    private void loadBk() {
        try {
            System.out.println("\n>>> Loading BK...");
            BufferedReader reader = new BufferedReader(new FileReader(bkPath));
            String line;
            int line_cnt = 0;
            while (null != (line = reader.readLine())) {
                String[] components = line.split("\t");
                MultiSet<String>[] arg_set_list =  predicate2ArgumentSetMap.computeIfAbsent(components[0], k -> {
                    MultiSet<String>[] _arg_set_list = new MultiSet[components.length - 1];
                    for (int i = 0; i < _arg_set_list.length; i++) {
                        _arg_set_list[i] = new MultiSet<>();
                    }
                    return _arg_set_list;
                });
                for (int i = 1; i < components.length; i++) {
                    arg_set_list[i-1].add(components[i]);
                }
                line_cnt++;
            }
            System.out.printf("BK loaded: %d predicates; %d facts\n", predicate2ArgumentSetMap.size(), line_cnt);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadHypothesis() {
        try {
            System.out.println("\n>>> Loading Hypothesis...");
            BufferedReader reader = new BufferedReader(new FileReader(hypothesisPath));
            String line;
            while (null != (line = reader.readLine())) {
                JplRule rule = AmieRuleLoader.toPrologSyntaxObject(line);
                int rule_idx = rules.size();
                recordCompoundVariableInfo(rule.head, rule_idx);
                for (Compound body: rule.body) {
                    recordCompoundVariableInfo(body, rule_idx);
                }
                rules.add(rule);
            }
            System.out.printf("Hypothesis loaded: %d rules\n", rules.size());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void recordCompoundVariableInfo(Compound compound, int formulaIdx) {
        String pred = compound.name();
        Term[] args = compound.args();
        for (int i = 0; i < args.length; i++) {
            if (args[i].isVariable()) {
                PredAndArgIdx pred_arg_idx = new PredAndArgIdx(pred, i);
                FormulaIdxAndVariable formula_idx_variable = new FormulaIdxAndVariable(formulaIdx, args[i].name());
                predArg2FormulaVarMap.computeIfAbsent(pred_arg_idx, k -> new HashSet<>()).add(formula_idx_variable);
            }
        }
    }

    private boolean predArgsShareSameVarInSameRule(String pred1, int argIdx1, String pred2, int argIdx2) {
        PredAndArgIdx pred_arg_idx1 = new PredAndArgIdx(pred1, argIdx1);
        PredAndArgIdx pred_arg_idx2 = new PredAndArgIdx(pred2, argIdx2);
        Set<FormulaIdxAndVariable> set1 = predArg2FormulaVarMap.get(pred_arg_idx1);
        Set<FormulaIdxAndVariable> set2 = predArg2FormulaVarMap.get(pred_arg_idx2);

        if (null == set1 || null == set2) {
            return false;
        }

        Set<FormulaIdxAndVariable> src, dst;
        if (set1.size() < set2.size()) {
            src = set1;
            dst = set2;
        } else {
            src = set2;
            dst = set1;
        }

        for (FormulaIdxAndVariable fv: src) {
            if (dst.contains(fv)) {
                return true;
            }
        }
        return false;
    }

    private void computeJaccardSimilarities() {
        class SimilarityInfo {
            double similarity;
            String pred1;
            int pred1ArgIdx;
            String pred2;
            int pred2ArgIdx;

            public SimilarityInfo(double similarity, String pred1, int pred1ArgIdx, String pred2, int pred2ArgIdx) {
                this.similarity = similarity;
                this.pred1 = pred1;
                this.pred1ArgIdx = pred1ArgIdx;
                this.pred2 = pred2;
                this.pred2ArgIdx = pred2ArgIdx;
            }
        }

        /* Calculate Chart Size */
        int rows_cnt = 2;
        for (Map.Entry<String, MultiSet<String>[]> entry: predicate2ArgumentSetMap.entrySet()) {
            rows_cnt += entry.getValue().length;
        }
        Row[] rows = new Row[rows_cnt];
        String[] predicates = predicate2ArgumentSetMap.keySet().toArray(new String[0]);
        List<SimilarityInfo> similarity_info_list = new ArrayList<>();

        /* Create titles */
        for (int i = 0; i < rows.length; i++) {
            rows[i] = sheetMatrix.createRow(i);
        }
        for (int col_offset = 2, i = 0; i < predicates.length; i++) {
            String predicate = predicates[i];
            rows[0].createCell(col_offset).setCellValue(predicate);
            rows[col_offset].createCell(0).setCellValue(predicate);
            int arity = predicate2ArgumentSetMap.get(predicate).length;
            for (int j = 0; j < arity; j++) {
                rows[1].createCell(col_offset + j).setCellValue(j + 1);
                rows[col_offset + j].createCell(1).setCellValue(j + 1);
            }
            col_offset += arity;
        }
        Row title_row = sheetSortedList.createRow(0);
        title_row.createCell(0).setCellValue("Similarity");
        title_row.createCell(1).setCellValue("P 1");
        title_row.createCell(2).setCellValue("Arg 1");
        title_row.createCell(3).setCellValue("P 2");
        title_row.createCell(4).setCellValue("Arg 2");

        /* Fill in the chart */
        for (int i = 0, row_offset = 2; i < predicates.length; i++) {
            MultiSet<String>[] args_i = predicate2ArgumentSetMap.get(predicates[i]);
            int arity_i = args_i.length;
            for (int j = i, col_offset = row_offset; j < predicates.length; j++) {
                MultiSet<String>[] args_j = predicate2ArgumentSetMap.get(predicates[j]);
                int arity_j = args_j.length;

                for (int ii = 0; ii < arity_i; ii++) {
                    for (int jj = 0; jj < arity_j; jj++) {
                        double similarity = args_i[ii].jaccardSimilarity(args_j[jj]);
                        int row_idx = row_offset + ii;
                        int col_idx = col_offset + jj;
                        rows[row_idx].createCell(col_idx).setCellValue(similarity);
                        rows[col_idx].createCell(row_idx).setCellValue(similarity);
                        if (row_idx != col_idx) {
                            similarity_info_list.add(
                                    new SimilarityInfo(similarity, predicates[i], ii, predicates[j], jj)
                            );
                        }
                    }
                }
                col_offset += arity_j;
            }
            row_offset += arity_i;
        }

        /* Sort According to Jaccard Similarities */
        similarity_info_list.sort(Comparator.comparingDouble((SimilarityInfo e) -> e.similarity).reversed());
        XSSFCellStyle cell_style = workbook.createCellStyle();
        cell_style.setFillForegroundColor(IndexedColors.RED.getIndex());
        cell_style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        for (int i = 0; i < similarity_info_list.size(); i++) {
            SimilarityInfo info = similarity_info_list.get(i);
            Row row = sheetSortedList.createRow(i + 1);
            row.createCell(0).setCellValue(info.similarity);
            row.createCell(1).setCellValue(info.pred1);
            row.createCell(2).setCellValue(info.pred1ArgIdx + 1);
            row.createCell(3).setCellValue(info.pred2);
            row.createCell(4).setCellValue(info.pred2ArgIdx + 1);

            if (predArgsShareSameVarInSameRule(info.pred1, info.pred1ArgIdx, info.pred2, info.pred2ArgIdx)) {
                /* Emphasize the row */
                for (int ii = 0; ii <= 4; ii++) {
                    row.getCell(ii).setCellStyle(cell_style);
                }
            }
        }
    }

    private void writeMatrix() {
        try (FileOutputStream fout = new FileOutputStream(
                "testData/familyRelation/MultisetCorrelationMatrix.xlsx"
        )) {
            workbook.write(fout);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void run() {
        loadBk();
        loadHypothesis();
        computeJaccardSimilarities();
        writeMatrix();
    }

    public static void main(String[] args) {
        ObserveArgumentCorrelation exp = new ObserveArgumentCorrelation(
                "testData/familyRelation/FamilyRelationMedium(0.05)(100x).tsv",
                "testData/familyRelation/HypothesisHumanMedium.amie"
        );
        exp.run();
    }
}
