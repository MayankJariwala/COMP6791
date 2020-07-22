package comp6791.model;

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
    private List<SearchPointer> searchPointers;
    private BufferedReader bufferedReader;

    public Query(List<SearchPointer> searchPointers) {
        this.searchPointers = searchPointers;
    }

    /**
     * Query Boxer : This function is responsible for taking input from user
     * and call appropriate function.
     *
     * @return String
     */
    public String queryBoxer() {
        System.out.println("Select Querying options:");
        System.out.println("1. Single Word ");
        System.out.println("2. Multiple AND (Space Separated)");
        System.out.println("3. Multiple OR (Space Separated)");
        System.out.println("Select Choice:");
        int choice = scanner.nextInt();
        scanner.nextLine();
        System.out.println("Enter Selected Condition Query:");
//        Because Text are converted to Lower case
        String query = scanner.nextLine().toLowerCase();
        SortedSet<Integer> resultSet = new TreeSet<>();
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
        return resultSet.toString();
    }

    /**
     * @param query Query separated by space which is then And-ed
     * @return Set of Document Id
     */
    private SortedSet<Integer> performMultipleAndQuery(String query) {
        String[] multipleTags = query.split(" ");
        List<SortedSet<Integer>> collectionsOfDocIds = new ArrayList<>();
        for (String tag : multipleTags) {
            SortedSet<Integer> tempResults = performSingleQuery(tag);
            assert tempResults != null;
            collectionsOfDocIds.add(tempResults);
        }
        collectionsOfDocIds.sort((Comparator<SortedSet>) (o1, o2) -> o2.size() - o1.size());
        if (!collectionsOfDocIds.isEmpty()) {
            if (collectionsOfDocIds.size() <= 1)
                return collectionsOfDocIds.get(0);
            SortedSet<Integer> first = collectionsOfDocIds.get(0);
            for (int k = 1; k < collectionsOfDocIds.size(); k++) {
                first.retainAll(collectionsOfDocIds.get(k));
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
    private SortedSet<Integer> performMultipleORQuery(String query) {
        String[] multipleTags = query.split(" ");
        SortedSet<Integer> finalResults = new TreeSet<>();
        for (String tag : multipleTags) {
            SortedSet<Integer> tempResults = performSingleQuery(tag);
            assert tempResults != null;
            finalResults.addAll(tempResults);
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
    private SortedSet<Integer> performSingleQuery(String query) {
        int initialAscii = (int) query.toCharArray()[0];
        List<String> fileNames = new ArrayList<>();
        this.searchPointers.forEach(searchPointer -> {
            if (searchPointer.getMinValue() <= initialAscii && searchPointer.getMaxValue() >= initialAscii) {
                fileNames.add(searchPointer.getFileName());
            }
        });
        try {
            SortedSet<Integer> resultedPostingList = new TreeSet<>();
            for (String currentFile : fileNames) {
                bufferedReader = new BufferedReader(new FileReader(currentFile));
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    String[] lineData = line.split(":", 2);
                    SortedSet<Integer> postingList = Arrays.stream(lineData[1].trim().substring(1, lineData[1].length() - 1)
                            .split(",")).map(s -> Integer.valueOf(s.trim())).collect(Collectors.toCollection(TreeSet::new));
                    if (lineData[0].trim().equalsIgnoreCase(query)) {
                        resultedPostingList.addAll(postingList);
                        break;
                    }
                }
            }
            return resultedPostingList;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
