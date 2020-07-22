package comp6791;

import java.util.Arrays;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * comp6791.Constants File
 *
 * @author Mayank Jariwala
 */
public class Constants {

    static final int DOC_TO_READ = 1000;

    public static final List<String> stopWords = Arrays.asList(
            "a", "an", "and", "are", "as", "at", "be", "by", "for", "from",
            "has", "he", "in", "is", "it", "its", "of", "on", "that", "the",
            "to", "was", "were", "will", "with"
    );

    public static double K = 1.45;
    public static double B = 0.75;
    public static double DAVG = 7440.05525336387;
    public static String CONCORDIA_BASE_PATH = "www.concordia.ca";
    public static String AI_TOPICS_BASE_PATH = "aitopics.org";

    public static String AI_INDEX_NAME = "AIIndex";
    public static String CONCORDIA_AI_INDEX_NAME = "ConcordAIIndex";

    public static String getCleanText(String rawText) {
        return rawText
                .replace("\n", " ")
                .toLowerCase()
                .replace("[", "")
                .replace("\\", "")
                .replace("]", "")
                .replace("-", " ")
                .replaceAll("[^a-zA-z]", " ")
                .replaceAll("[\"\'’()^!,_`]", "")
                .trim();
    }
}
