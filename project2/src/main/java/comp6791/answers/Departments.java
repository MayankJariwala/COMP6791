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
public class Departments {

    private List<String> departments = new ArrayList<>();
    private Set<String> foundDepartments = new HashSet<>();
    private Set<Integer> resultsKeySet = new HashSet<>();

    public Departments(Set<Integer> docsIds) {
        this.resultsKeySet = docsIds;
    }

    public void showDepartmentsName() {
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader("departments.txt"))) {
            String line = "";
            while ((line = bufferedReader.readLine()) != null) {
                departments.add(line.trim());
            }
            display();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void display() {
        for (String name : departments) {
            Set<Integer> docsId = new HashSet<>();
            try (BufferedReader bufferedReader = new BufferedReader(new FileReader("invertedIndex/ConcordAIIndex.txt"))) {
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    String[] lineData = line.split(":", 2);
                    if (lineData[0].trim().equalsIgnoreCase(name.trim().toLowerCase())) {
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
                        foundDepartments.add(name);
                } else {
                    resultsKeySet.retainAll(docsId);
                    if (resultsKeySet.size() > 0)
                        foundDepartments.add(name);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        System.out.println("Department Name : " + foundDepartments);
    }
}
