package comp6791;

import java.util.Arrays;
import java.util.List;

/**
 * comp6791.Constants File
 *
 * @author Mayank Jariwala
 */
class Constants {

    static final int DOC_TO_READ = 500;

    static final List<String> stopWords = Arrays.asList(
            "a", "an", "and", "are", "as", "at", "be", "by", "for", "from",
            "has", "he", "in", "is", "it", "its", "of", "on", "that", "the",
            "to", "was", "were", "will", "with"
    );
    // Ref: https://www.suchmaschine.biz/stop-word-lists/
    //30
//        static final List<String> stopWords = Arrays.asList(
//            "the", "of ", "and ", "to ", "in ", "for ", "internet ", "on ", "home ", "is ", "by ", "all ", "this ", "with ", "services ", "about ", "or ", "at ", "email ", "from ", "are ", "website ", "us ", "site ", "sites ", "you ", "information ", "contact ", "more ", "an ", "search ", "new ", "that ", "your ", "it ", "be ", "prices ", "as ", "page ", "hotels ", "products ", "other ", "have ", "web ", "copyright ", "download ", "not ", "can ", "reviews ", "our ", "use ", "women"
//    ).parallelStream().map(String::trim).collect(Collectors.toList());
    // 153
//    static final List<String> stopWords = Arrays.asList(
//            "a", "about", "above", "after", "again", "against", "all", "am", "an", "and", "any", "are", "as", "at", "be", "because", "been", "before", "being", "below", "between", "both", "but", "by", "could", "did", "do", "does", "doing", "down", "during", "each", "few", "for", "from", "further", "had", "has", "have", "having", "he", "he’d", "he’ll", "he’s", "her", "here", "here’s", "hers", "herself", "him", "himself", "his", "how", "how’s", "I", "I’d", "I’ll", "I’m", "I’ve", "if", "in", "into", "is", "it", "it’s", "its", "itself", "let’s", "me", "more", "most", "my", "myself", "nor", "of", "on", "once", "only", "or", "other", "ought", "our", "ours", "ourselves", "out", "over", "own", "same", "she", "she’d", "she’ll", "she’s", "should", "so", "some", "such", "than", "that", "that’s", "the", "their", "theirs", "them", "themselves", "then", "there", "there’s", "these", "they", "they’d", "they’ll", "they’re", "they’ve", "this", "those", "through", "to", "too", "under", "until", "up", "very", "was", "we", "we’d", "we’ll", "we’re", "we’ve", "were", "what", "what’s", "when", "when’s", "where", "where’s", "which", "while", "who", "who’s", "whom", "why", "why’s", "with", "would", "you", "you’d", "you’ll", "you’re", "you’ve", "your", "yours", "yourself", "yourselves"
//    ).parallelStream().map(String::trim).collect(Collectors.toList());

}
