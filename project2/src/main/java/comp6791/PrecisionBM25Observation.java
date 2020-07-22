package comp6791;

import lombok.Getter;
import lombok.Setter;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

@Getter
@Setter
public class PrecisionBM25Observation {

    private static PrecisionBM25Observation precisionBM25Observation;

    private PrecisionBM25Observation() {

    }

    public static PrecisionBM25Observation getInstance() {
        if (precisionBM25Observation == null)
            precisionBM25Observation = new PrecisionBM25Observation();
        return precisionBM25Observation;
    }

    public void writeBM25(String query, List<String> results) {
        writeTop10Results(query, results);
        writeTop50Results(query, results);
        writeTop100Results(query, results);
    }

    private void writeTop10Results(String query, List<String> results) {
        try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter("bm25/" + query + "10.doc"))) {
            bufferedWriter.write("Precision@10: " + query.toUpperCase() + "\n");
            for (int i = 0; i < (Math.min(results.size(), 10)); i++) {
                bufferedWriter.write((+1) + ". " + results.get(i) + "\n");
            }
        } catch (IOException e) {
            System.out.println(e.getLocalizedMessage());
        }
    }

    private void writeTop50Results(String query, List<String> results) {
        try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter("bm25/" + query + "50.doc"))) {
            bufferedWriter.write("Precision@50: " + query.toUpperCase() + "\n");
            for (int i = 0; i < (Math.min(results.size(), 50)); i++) {
                bufferedWriter.write((i + 1) + ". " + results.get(i) + "\n");
            }
        } catch (IOException e) {
            System.out.println(e.getLocalizedMessage());
        }
    }

    private void writeTop100Results(String query, List<String> results) {
        try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter("bm25/" + query + "100.doc"))) {
            bufferedWriter.write("Precision@100: " + query.toUpperCase() + "\n");
            for (int i = 0; i < (Math.min(results.size(), 100)); i++) {
                bufferedWriter.write((i + 1) + ". " + results.get(i) + "\n");
            }
        } catch (IOException e) {
            System.out.println(e.getLocalizedMessage());
        }
    }
}
