package comp6791.answers;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Researchers {

    private List<String> researchers = new ArrayList<>();
    private Set<String> foundResearchers = new HashSet<>();
    private Set<Integer> resultsKeySet = new HashSet<>();

    public Researchers(Set<Integer> docsIds) {
        this.resultsKeySet = docsIds;
    }

    public void showResearchersName() {
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader("researchers.txt"))) {
            String line = "";
            while ((line = bufferedReader.readLine()) != null) {
                researchers.add(line.trim());
            }
            display();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void display() {
        for (String name : researchers) {
            Set<Integer> docsId = new HashSet<>();
            try (BufferedReader bufferedReader = new BufferedReader(new FileReader("invertedIndex/ConcordAIIndex.txt"))) {
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    String[] lineData = line.split(":", 2);
                    if (lineData[0].trim().equalsIgnoreCase(name.toLowerCase())) {
                        List<String> postingList = Arrays.stream(lineData[1].trim().substring(1, lineData[1].length() - 1)
                                .split(",")).collect(Collectors.toList());
                        for (String data : postingList) {
                            String[] mapper = data.split("=");
                            docsId.add(Integer.parseInt(mapper[0].trim()));
                        }
                        break;
                    }
                }
                if (docsId.size() < resultsKeySet.size()) {
                    docsId.retainAll(resultsKeySet);
                    if (docsId.size() > 0)
                        foundResearchers.add(name);
                } else {
                    resultsKeySet.retainAll(docsId);
                    if (resultsKeySet.size() > 0)
                        foundResearchers.add(name);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        System.out.println("Researchers : " + foundResearchers);
    }
}
