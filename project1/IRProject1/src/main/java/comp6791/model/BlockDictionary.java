package comp6791.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;

/**
 * Term Dictionary Model
 *
 * @author Mayank Jariwala
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class BlockDictionary implements Serializable {

    private SortedMap<String, SortedSet<Integer>> loadFileContent = new TreeMap<>();
}
