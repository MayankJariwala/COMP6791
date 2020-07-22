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
public class Areas {

    private List<String> researchers = new ArrayList<>();
    private Set<String> foundAreas = new HashSet<>();
    private Set<Integer> resultsKeySet = new HashSet<>();

    public Areas(Set<Integer> docsIds) {
        this.resultsKeySet = docsIds;
    }

    public void showResearcherAreasName() {
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader("areas.txt"))) {
            String line;
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
            List<Set<Integer>> collectedIds = new ArrayList<>();
            try (BufferedReader bufferedReader = new BufferedReader(new FileReader("invertedIndex/ConcordAIIndex.txt"))) {
                String line;
                for (String areaName : name.split(" ")) {
                    while ((line = bufferedReader.readLine()) != null) {
                        String[] lineData = line.split(":", 2);
                        if (lineData[0].trim().equalsIgnoreCase(areaName.toLowerCase())) {
                            List<String> postingList = Arrays.stream(lineData[1].trim().substring(1, lineData[1].length() - 1)
                                    .split(",")).collect(Collectors.toList());
                            for (String data : postingList) {
                                String[] mapper = data.split("=");
                                docsId.add(Integer.parseInt(mapper[0].trim()));
                            }
                            collectedIds.add(docsId);
                            break;
                        }
                    }
                    collectedIds.sort(Comparator.comparingInt(Set::size));
                    if (collectedIds.size() > 0) {
                        Set<Integer> first = collectedIds.get(0);
                        if (collectedIds.size() > 1) {
                            for (int k = 1; k < collectedIds.size(); k++) {
                                first.retainAll(collectedIds.get(k));
                            }
                        }
                        if (first.size() < resultsKeySet.size()) {
                            first.retainAll(resultsKeySet);
                            if (first.size() > 0)
                                foundAreas.add(name);
                        }
                    }
                }
//                if (docsId.size() < resultsKeySet.size()) {
//                    docsId.retainAll(resultsKeySet);
//                    if (docsId.size() > 0)
//                        foundAreas.add(name);
//                } else {
//                    resultsKeySet.retainAll(docsId);
//                    if (resultsKeySet.size() > 0)
//                        foundAreas.add(name);
//                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        System.out.println("Areas : " + foundAreas);
    }
}
