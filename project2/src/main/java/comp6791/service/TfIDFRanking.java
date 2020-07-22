package comp6791.service;

import comp6791.SPIMI;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.*;
import java.util.stream.Collectors;

public class TfIDFRanking {
    private SortedMap<Integer, Double> docsScoreForQuery = new TreeMap<>();

    public Set<Integer> rankTheDocuments(String query, SortedMap<Integer, Integer> resultSet, HashMap<String, SortedMap<Integer, Integer>> postingListOfQueryTerm) {
        for (Map.Entry<Integer, Integer> document : resultSet.entrySet()) {
            double score = scoreOfQueryInDocument(query, document, postingListOfQueryTerm);
            docsScoreForQuery.put(document.getKey(), score);
        }
        Comparator<Map.Entry<Integer, Double>> valueComparator = (e1, e2) -> {
            Double v1 = e1.getValue();
            Double v2 = e2.getValue();
            return v2.compareTo(v1);
        };
        List<Map.Entry<Integer, Double>> listOfRankDocIds = new ArrayList<>(docsScoreForQuery.entrySet());
        listOfRankDocIds.sort(valueComparator);
        LinkedHashMap<Integer, Double> sortedByValue = new LinkedHashMap<>();
        for (Map.Entry<Integer, Double> entry : listOfRankDocIds) {
            sortedByValue.put(entry.getKey(), entry.getValue());
        }
        return sortedByValue.keySet();
    }

    /**
     * This function calculate score of current received document as a formal parameter
     *
     * @param query    Input Query
     * @param document Current InTransit Document Object
     * @return Score of InTransit Document
     */
    private double scoreOfQueryInDocument(String query, Map.Entry<Integer, Integer> document, HashMap<String, SortedMap<Integer, Integer>> postingListOfQueryTerm) {
        double scoreOfDoc = 0.0;
        String[] queryTerms = query.split(" ");
        for (String queryTerm : queryTerms) {
            try {
                double IdfOfQueryTerm = calculateIDFForQueryTerm(queryTerm, postingListOfQueryTerm);
                double tf = getTFOfQueryTerm(queryTerm, postingListOfQueryTerm, document);
                scoreOfDoc += IdfOfQueryTerm * tf;
            } catch (Exception ignored) {
            }
        }
        return scoreOfDoc;
    }

    private double getTFOfQueryTerm(String queryTerm, HashMap<String, SortedMap<Integer, Integer>> postingListOfQueryTerm, Map.Entry<Integer, Integer> document) {
        double queryTermFrequencyInCurrentDoc = 0.0;
        if (postingListOfQueryTerm.get(queryTerm) != null || postingListOfQueryTerm.get(queryTerm).get(document.getKey()) != null)
            queryTermFrequencyInCurrentDoc = postingListOfQueryTerm.get(queryTerm).get(document.getKey());
        return queryTermFrequencyInCurrentDoc;
    }

    /**
     * Calculate IDF for current query in term in query.
     * For example: IDF("George") in "George Bush"
     *
     * @param queryTerm Query Term
     * @return IDF Value
     */
    private double calculateIDFForQueryTerm(String queryTerm, HashMap<String, SortedMap<Integer, Integer>> postingListOfQueryTerm) {
        double totalDocs = SPIMI.concordiaDocNumber == 1 ? 10479 : SPIMI.concordiaDocNumber;
        double docsContainingQueryTerm;
        docsContainingQueryTerm = getPostingListLengthFromAiIndex(queryTerm);
        if (docsContainingQueryTerm == 0.0 && postingListOfQueryTerm.get(queryTerm) != null)
            docsContainingQueryTerm = postingListOfQueryTerm.get(queryTerm).size();
        double numerator = totalDocs - docsContainingQueryTerm + 0.5;
        double denominator = docsContainingQueryTerm + 0.5;
        return Math.log(numerator / denominator);
    }

    private double getPostingListLengthFromAiIndex(String queryTerm) {
        double dfValue = 0.0;
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader("invertedIndex/AITermIndex.txt"))) {
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                String[] lineData = line.split(":", 2);
                if (lineData[0].trim().equalsIgnoreCase(queryTerm)) {
                    List<String> postingList = Arrays.stream(lineData[1].trim().substring(1, lineData[1].length() - 1)
                            .split(",")).collect(Collectors.toList());
                    dfValue = postingList.size();
                }
            }
        } catch (Exception e) {
            System.out.println("No Such Term Found in AI Index");
        }
        return dfValue;
    }

}
