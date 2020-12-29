package exp;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import utils.MultiSet;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ObserveArgumentCorrelation {

    private final String bkPath;
    private final Map<String, MultiSet<String>[]> predicate2ArgumentSetMap;
    private final XSSFWorkbook workbook;
    private final XSSFSheet sheet;

    public ObserveArgumentCorrelation(String bkPath) {
        this.bkPath = bkPath;
        this.predicate2ArgumentSetMap = new HashMap<>();
        this.workbook = new XSSFWorkbook();
        this.sheet = workbook.createSheet("observation");
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

    private void computeJaccardMatrix() {
        /* Calculate Chart Size */
        int rows_cnt = 2;
        for (Map.Entry<String, MultiSet<String>[]> entry: predicate2ArgumentSetMap.entrySet()) {
            rows_cnt += entry.getValue().length;
        }
        Row[] rows = new Row[rows_cnt];
        String[] predicates = predicate2ArgumentSetMap.keySet().toArray(new String[0]);

        /* Create titles */
        for (int i = 0; i < rows.length; i++) {
            rows[i] = sheet.createRow(i);
        }
        for (int col_idx = 2, i = 0; i < predicates.length; i++) {
            String predicate = predicates[i];
            rows[0].createCell(col_idx).setCellValue(predicate);
            rows[col_idx].createCell(0).setCellValue(predicate);
            int arity = predicate2ArgumentSetMap.get(predicate).length;
            for (int j = 0; j < arity; j++) {
                rows[1].createCell(col_idx + j).setCellValue(j + 1);
                rows[col_idx + j].createCell(1).setCellValue(j + 1);
            }
            col_idx += arity;
        }

        /* Fill in the chart */
        for (int i = 0, row_idx = 2; i < predicates.length; i++) {
            MultiSet<String>[] args_i = predicate2ArgumentSetMap.get(predicates[i]);
            int arity_i = args_i.length;
            for (int j = i, col_idx = row_idx; j < predicates.length; j++) {
                MultiSet<String>[] args_j = predicate2ArgumentSetMap.get(predicates[j]);
                int arity_j = args_j.length;

                for (int ii = 0; ii < arity_i; ii++) {
                    for (int jj = 0; jj < arity_j; jj++) {
                        double similarity = args_i[ii].jaccardSimilarity(args_j[jj]);
                        rows[row_idx + ii].createCell(col_idx + jj).setCellValue(similarity);
                        rows[col_idx + jj].createCell(row_idx + ii).setCellValue(similarity);
                    }
                }
                col_idx += arity_j;
            }
            row_idx += arity_i;
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
        computeJaccardMatrix();
        writeMatrix();
    }

    public static void main(String[] args) {
        ObserveArgumentCorrelation exp = new ObserveArgumentCorrelation(
                "testData/familyRelation/FamilyRelationSimple(0.050000)(10x).tsv"
        );
        exp.run();
    }
}
