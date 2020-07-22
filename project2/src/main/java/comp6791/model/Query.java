package comp6791.model;

import comp6791.Constants;
import comp6791.PrecisionBM25Observation;
import comp6791.PrecisionTFIDFObservation;
import comp6791.SPIMI;
import comp6791.answers.Areas;
import comp6791.answers.Departments;
import comp6791.answers.Researchers;
import comp6791.service.TfIDFRanking;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Query Model
 *
 * @author Mayank Jariwala
 */
public class Query {
    private Scanner scanner = new Scanner(System.in);
    private HashMap<String, SortedMap<Integer, Integer>> postingListOfQueryTerm = new HashMap<>();
    private SortedMap<Integer, Double> docsScoreForQuery = new TreeMap<>();
    private TfIDFRanking tfIDFRanking = new TfIDFRanking();

    public Query() {
    }

    /**
     * Query Boxer : This function is responsible for taking input from user
     * and call appropriate function.
     *
     * @return String
     */
    public String queryBoxer() {
        int choice = 1;
        try {
            System.out.println("Select Querying options:");
            System.out.println("1. Single Word ");
            System.out.println("2. Multiple AND (Space Separated)");
            System.out.println("3. Multiple OR (Space Separated)");
            System.out.println("Select Choice:");
            choice = scanner.nextInt();
            scanner.nextLine();
        } catch (Exception e) {
            System.out.println("Oops!! You entered wrong input");
            return "";
        }
        System.out.println("Enter Query:");
        String query = "";
        while (query.isEmpty()) {
            query = scanner.nextLine().toLowerCase()
                    .replace("[", "")
                    .replace("\\", "")
                    .replace("]", "")
                    .replace("-", " ")
                    .replaceAll("[^a-zA-z]", " ")
                    .replaceAll("[\"\'’()^!,_`]", "")
                    .trim();
        }
        SortedMap<Integer, Integer> resultSet = new TreeMap<>();
        switch (choice) {
            case 1:
                resultSet = performSingleQuery(query);
                break;
            case 2:
                resultSet = performMultipleAndQuery(query);
                break;
            case 3:
                resultSet = performMultipleORQuery(query);
                break;
            default:
                System.out.println("Wrong Choice selected");
                break;
        }
        assert resultSet != null;
        Set<Integer> rankDocIds;
        if (resultSet.size() > 1) {
            System.out.println("PROJECT I OUTPUT: " + resultSet.keySet());
            System.out.println("Do you want to enter value for K and B (true/false)?");
            boolean askKAndB = scanner.nextBoolean();
            if (askKAndB) {
                System.out.println("Enter K");
                Constants.K = scanner.nextDouble();
                scanner.nextLine();
                System.out.println("Enter B");
                Constants.B = scanner.nextDouble();
                scanner.nextLine();
            }
            rankDocIds = rankTheDocuments(query, resultSet);
        } else {
            System.out.println("No Ranking Needed for single document result " + resultSet.keySet());
            rankDocIds = resultSet.keySet();
        }
        List<String> rankingByBMI = mapDocIdWithFileName(rankDocIds);
        System.out.println("Ranking By BMI : " + rankingByBMI);
        rankDocIds = tfIDFRanking.rankTheDocuments(query, resultSet, postingListOfQueryTerm);
        List<String> rankingByTFIDF = mapDocIdWithFileName(rankDocIds);
        System.out.println("Ranking By TF-IDF : " + rankingByTFIDF);
        // Write Observation
        PrecisionTFIDFObservation.getInstance().writeTFIDF(query, rankingByTFIDF);
        PrecisionBM25Observation.getInstance().writeBM25(query, rankingByBMI);
        if (resultSet.keySet().size() > 0) {
            Researchers researchers = new Researchers(resultSet.keySet());
            researchers.showResearchersName();
            Areas areas = new Areas(resultSet.keySet());
            areas.showResearcherAreasName();
            Departments departments = new Departments(resultSet.keySet());
            departments.showDepartmentsName();
        }
        return "Completed!";
    }

    private List<String> mapDocIdWithFileName(Set<Integer> resultsKeySet) {
        List<String> fileNames = new ArrayList<>();
        for (int docId : resultsKeySet) {
            String foundPath = SPIMI.ConcordiafilesMapping.get(docId);
            fileNames.add(foundPath.replace("D:\\concordia\\fall2019\\comp6791\\projects\\project2\\", ""));
        }
        return fileNames;
    }

    /**
     * @param query Query separated by space which is then And-ed
     * @return Set of Document Id
     */
    private SortedMap<Integer, Integer> performMultipleAndQuery(String query) {
        String[] multipleTags = query.split(" ");
        List<String> tokens = Arrays.stream(multipleTags).filter(s -> !s.equalsIgnoreCase(""))
                .collect(Collectors.toList());
        tokens.removeAll(Constants.stopWords);
        List<SortedMap<Integer, Integer>> collectionsOfDocIds = new ArrayList<>();
        for (String tag : tokens) {
            SortedMap<Integer, Integer> tempResults = performSingleQuery(tag);
            assert tempResults != null;
            collectionsOfDocIds.add(tempResults);
        }
        collectionsOfDocIds.sort((Comparator<SortedMap>) (o1, o2) -> o1.size() - o2.size());
        if (!collectionsOfDocIds.isEmpty()) {
            if (collectionsOfDocIds.size() <= 1)
                return collectionsOfDocIds.get(0);
            SortedMap<Integer, Integer> first = collectionsOfDocIds.get(0);
            Set<Integer> docIds = first.keySet();
            for (int k = 1; k < collectionsOfDocIds.size(); k++) {
                docIds.retainAll(collectionsOfDocIds.get(k).keySet());
            }
            return first;
        } else {
            return null;
        }
    }

    /**
     * @param query Query separated by space which is then Or-ed
     * @return Set of Document Id
     */
    private SortedMap<Integer, Integer> performMultipleORQuery(String query) {
        String[] multipleTags = query.split(" ");
        List<String> tokens = Arrays.stream(multipleTags).filter(s -> !s.equalsIgnoreCase(""))
                .collect(Collectors.toList());
        tokens.removeAll(Constants.stopWords);
        SortedMap<Integer, Integer> finalResults = new TreeMap<>();
        for (String tag : tokens) {
            SortedMap<Integer, Integer> tempResults = performSingleQuery(tag);
            assert tempResults != null;
            finalResults.putAll(tempResults);
        }
        return finalResults;
    }

    /**
     * This function is responsible to search the term in created dictionary and
     * provided respective docId in sorted order
     *
     * @param query Single query term
     * @return Set of Document Id
     */
    private SortedMap<Integer, Integer> performSingleQuery(String query) {
        try {
            SortedMap<Integer, Integer> resultedPostingList = new TreeMap<>();
            BufferedReader bufferedReader = new BufferedReader(new FileReader("invertedIndex/ConcordAIIndex.txt"));
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                String[] lineData = line.split(":", 2);
                if (lineData[0].trim().equalsIgnoreCase(query)) {
                    List<String> postingList = Arrays.stream(lineData[1].trim().substring(1, lineData[1].length() - 1)
                            .split(",")).collect(Collectors.toList());
                    SortedMap<Integer, Integer> postingListMap = new TreeMap<>();
                    for (String data : postingList) {
                        String[] keyValue = data.split("=");
                        postingListMap.put(Integer.valueOf(keyValue[0].trim()), Integer.valueOf(keyValue[1].trim()));
                    }
                    resultedPostingList.putAll(postingListMap);
                    break;
                }
            }
            postingListOfQueryTerm.put(query, resultedPostingList);
            return resultedPostingList;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private Set<Integer> rankTheDocuments(String query, SortedMap<Integer, Integer> resultSet) {
        for (Map.Entry<Integer, Integer> document : resultSet.entrySet()) {
            double score = scoreOfQueryInDocument(query, document);
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
    private double scoreOfQueryInDocument(String query, Map.Entry<Integer, Integer> document) {
        double scoreOfDoc = 0.0;
        String[] queryTerms = query.split(" ");
        for (String queryTerm : queryTerms) {
            try {
                double IdfOfQueryTerm = calculateIDFForQueryTerm(queryTerm);
                double sectionBValue = calculateSectionB(queryTerm, document);
                scoreOfDoc += IdfOfQueryTerm * sectionBValue;
            } catch (Exception ignored) {

            }
        }
        return scoreOfDoc;
    }


    /**
     * Calculate IDF for current query in term in query.
     * For example: IDF("George") in "George Bush"
     *
     * @param queryTerm Query Term
     * @return IDF Value
     */
    private double calculateIDFForQueryTerm(String queryTerm) {
        double totalDocs = SPIMI.concordiaDocNumber;
        double docsContainingQueryTerm;
        docsContainingQueryTerm = getPostingListLengthFromAiIndex(queryTerm);
        if (docsContainingQueryTerm == 0.0 && postingListOfQueryTerm.get(queryTerm) != null) {
            System.out.println("Using DF of Concordia for " + queryTerm);
            docsContainingQueryTerm = postingListOfQueryTerm.get(queryTerm).size();
        }
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

    /**
     * Function responsible to calculate the part B of the BM25 formula.
     *
     * @param queryTerm Query Term
     * @param document  Current InTransit Document Object
     * @return PartB Value
     */
    private double calculateSectionB(String queryTerm, Map.Entry<Integer, Integer> document) {
        double currentDocLength = SPIMI.docsMappingWithNoOfTokens.get(document.getKey());
        double queryTermFrequencyInCurrentDoc = 0.0;
        if (postingListOfQueryTerm.get(queryTerm) != null || postingListOfQueryTerm.get(queryTerm).get(document.getKey()) != null)
            queryTermFrequencyInCurrentDoc = postingListOfQueryTerm.get(queryTerm).get(document.getKey());
        double sectionBValueNumerator = queryTermFrequencyInCurrentDoc * (Constants.K + 1);
        double sectionBValueDenominator = queryTermFrequencyInCurrentDoc + Constants.K * (1 - Constants.B + (Constants.B * (currentDocLength / Constants.DAVG)));
        return sectionBValueNumerator / sectionBValueDenominator;
    }
}
