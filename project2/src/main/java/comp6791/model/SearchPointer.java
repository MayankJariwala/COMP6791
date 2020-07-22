package comp6791.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Search Pointer - Global Map Pointer
 * <p>
 * This domain has information related to which letter words are in which
 * inverted index file.
 * </p>
 *
 * @author Mayank Jariwala
 */
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class SearchPointer {
    private String fileName;
    private int minValue; // Dictionary "start word" ASCII value
    private int maxValue; // Dictionary 'end word" ASCII value
}
