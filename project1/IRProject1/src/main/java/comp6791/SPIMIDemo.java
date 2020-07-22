package comp6791;

import comp6791.model.BlockDictionary;
import jdk.nashorn.internal.ir.debug.ObjectSizeCalculator;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * comp6791.SPIMI Algorithm - Single Pass In Memory Index
 *
 * @author Mayank Jariwala
 */
public class SPIMIDemo {

    private static List<String> individualPositionCount = new ArrayList<>();
    private SortedMap<String, SortedSet<Integer>> invertedDict = new TreeMap<>();
    private List<BlockReader> blockReaderList = new ArrayList<>();
    private int invertedIndex = 0, initialPointReader = 0;
    private static int blockNo = 0;
    private List<String> keyBuffer = new ArrayList<>(blockNo);
    private List<SortedSet<Integer>> postingListId = new ArrayList<>();
    private static int tokens = 0;
    private static int nonposition = 0;

    public static void main(String[] args) {
        SPIMIDemo SPIMIDemo = new SPIMIDemo();
        SPIMIDemo.startReadingFiles();
        SPIMIDemo.startMergingPhase();
        System.gc();
        System.out.println("Positional Tokens : " + individualPositionCount.size());
        System.out.println("Terms Distinct : " + tokens);
        System.out.println("Non-Position : " + nonposition);
    }

    /**
     * Responsible for merging all intermediate blocks
     */
    private void startMergingPhase() {
        System.out.println("----- Merging " + blockNo + " Blocks phase -----");
        try {
            int i = 0;
            while (i < blockNo) {
                FileReader fileReader = new FileReader("blocks/block_" + i + ".txt");
                BufferedReader bufferedReader = new BufferedReader(fileReader);
                blockReaderList.add(new BlockReader(bufferedReader));
                i++;
            }
            for (int blockReaderPoint = 0; blockReaderPoint < blockNo; blockReaderPoint++) {
                BlockReader br = blockReaderList.get(blockReaderPoint);
                try {
                    if (br.isFileClosed()) {
                        String data = br.readNext();
                        if (!data.equalsIgnoreCase("")) {
                            String[] fileLineData = data.split("`", 2);
                            SortedSet<Integer> postingList = Arrays.stream(fileLineData[1].trim().substring(1, fileLineData[1].length() - 1)
                                    .split(",")).map(s -> Integer.valueOf(s.trim())).collect(Collectors.toCollection(TreeSet::new));
                            postingListId.add(initialPointReader, postingList);
                            keyBuffer.add(initialPointReader, fileLineData[0].trim());
                            initialPointReader++;
                        }
                    }
                } catch (NullPointerException e) {
                    // Continue
                }
            }
            while (!blockReaderList.isEmpty()) {
                findMin(keyBuffer);
            }
            if (invertedDict.size() > 0)
                writeInvertedIndexToFile(invertedDict);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Find min/smallest word from n blocks dictionary first pointer and put into new dictionary of
     * 25000 terms
     *
     * @param currentInTransitData First Word from each block of intermediate dictionary
     * @throws IOException Exception related to I/O operation
     */
    private void findMin(List<String> currentInTransitData) throws IOException {
        if (currentInTransitData.isEmpty()) {
            blockReaderList.clear();
            return;
        }
        String data = Collections.min(currentInTransitData);
        int indexOfMin = currentInTransitData.indexOf(data);
        if (invertedDict.containsKey(data)) {
            SortedSet<Integer> existingList = invertedDict.get(data);
            existingList.addAll(postingListId.get(indexOfMin));
            invertedDict.put(data, existingList);
        } else {
            invertedDict.put(data, postingListId.get(indexOfMin));
        }
        if (invertedDict.size() == 25000) {
            writeInvertedIndexToFile(invertedDict);
            invertedDict.clear();
        }
        BlockReader br = blockReaderList.get(indexOfMin);
        try {
            if (br.isFileClosed()) {
                String topStringRead = br.readNext();
                if (topStringRead == null) {
                    currentInTransitData.remove(indexOfMin);
                    blockReaderList.remove(indexOfMin);
                    br.close();
                    return;
                }
                String[] fileLineData = topStringRead.split("`", 2);
                if (fileLineData.length == 2) {
                    SortedSet<Integer> postingList = Arrays.stream(fileLineData[1].trim().substring(1, fileLineData[1].length() - 1)
                            .split(",")).map(s -> Integer.valueOf(s.trim()))
                            .collect(Collectors.toCollection(TreeSet::new));
                    postingListId.set(indexOfMin, postingList);
                    keyBuffer.set(indexOfMin, fileLineData[0].trim());
                }
            }
        } catch (NullPointerException e) {
            br.close();
        }
    }

    /**
     * Start Reading SGM Files and pass to comp6791.SPIMI#storeSingleFileDocsWithContent function
     */
    private void startReadingFiles() {
        try {
            Stream<Path> path = Files.list(Paths.get("/Users/Silence/Desktop/concordia/reuters21578/"));
            List<Path> sgmFiles = path.filter(i -> i.getFileName().toString().endsWith(".sgm")).collect(Collectors.toList());
            int noOfSgmFiles = sgmFiles.size();
            boolean fileStatus;
            int fileReadPointer = 0;
            do {
                fileStatus = storeSingleFileDocsWithContent(sgmFiles.get(fileReadPointer).toString());
                fileReadPointer++;
                noOfSgmFiles--;
            } while (fileStatus && noOfSgmFiles > 0);
            System.err.println("Congrats!! All sgm files are read!!");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Process cleaning and normalize steps content of single file body text upto limit of
     * 500 docs and write the block into disk.
     *
     * @param file Name of Current Reading File
     * @return boolean Contents are stored or not
     */
    private boolean storeSingleFileDocsWithContent(String file) {
        List<String> tokens = new ArrayList<>();
        SortedMap<String, SortedSet<Integer>> loadFileContent = new TreeMap<>();
        File sgmFileObject = new File(file);
        Document document;
        int round = 0, pointer, docsToRead;
        try {
            document = Jsoup.parse(sgmFileObject, "utf-8");
            Elements elements = document.getElementsByTag("REUTERS");
            int totalDocs = elements.size();
            int corpusRead;
            while (totalDocs != 0) {
                corpusRead = 0;
                pointer = round * 500;
                docsToRead = totalDocs > Constants.DOC_TO_READ ? Constants.DOC_TO_READ : totalDocs;
                while (corpusRead < docsToRead) {
                    Element element = elements.get(pointer);
                    int newDocId = Integer.parseInt(element.attr("newid"));
                    String[] bodyContent = element.getElementsByTag("TEXT")
                            .text().trim().split("-", 2);
                    if (bodyContent.length == 2 && !bodyContent[1].trim().equals("")) {
                        String[] cleanContentTokens = bodyContent[1]
                                .toLowerCase()
                                .replaceAll("[0-9]", "")
                                .trim()
                                .split(" ");
                        tokens = Arrays.stream(cleanContentTokens).filter(s -> !s.isEmpty())
                                .collect(Collectors.toList());
                        tokens.removeAll(Constants.stopWords);
                    }
                    for (String token : tokens) {
                        if (!token.isEmpty()) {
                            individualPositionCount.add(token);
                            if (loadFileContent.containsKey(token)) {
                                SortedSet<Integer> sortedSet = loadFileContent.get(token);
                                sortedSet.add(newDocId);
                                loadFileContent.put(token, sortedSet);
                            } else {
                                SortedSet<Integer> sortedSet = new TreeSet<>();
                                sortedSet.add(newDocId);
                                loadFileContent.put(token, sortedSet);
                            }
                        }
                    }
                    corpusRead++;
                    pointer++;
                    totalDocs--;
                }

                BlockDictionary blockDictionary = new BlockDictionary();
                blockDictionary.setLoadFileContent(loadFileContent);
                writeDictionaryFile(docsToRead, blockNo, blockDictionary);
                loadFileContent.clear();
                round++;
                blockNo++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (IndexOutOfBoundsException e) {
            System.err.println("Oops!! Array bound exception triggered!! " + e.getLocalizedMessage());
        }
        System.gc();
        return true;
    }

    /**
     * Simply write the intermediate block which is dictionary into disk.
     *
     * @param documentRead    Number of document read
     * @param blockNo         Current Block No
     * @param blockDictionary Intermediate Dictionary of Collected Terms
     */
    private void writeDictionaryFile(int documentRead, int blockNo, BlockDictionary blockDictionary) {
        SortedMap<String, SortedSet<Integer>> loadFileContent = blockDictionary.getLoadFileContent();
        System.out.println(documentRead +
                " documents are process and its being written into disk with block no. " + blockNo + "" +
                " \t [ Dictionary Size(MB) ]: " + (double) ObjectSizeCalculator.getObjectSize(loadFileContent) / (1024 * 1024));
        String blockName = "blocks/block_" + blockNo + ".txt";
        try {
            Path path = Paths.get("blocks");
            createAndWriteContentTooFile(loadFileContent, blockName, path);
        } catch (IOException e) {
            System.err.println("Issue occur while writing blocks: " + e.getMessage());
        }
    }

    /**
     * Simply write the inverted index into disk.
     *
     * @param invertedDict Inverted Index of Max 25k Terms
     */
    private void writeInvertedIndexToFile(SortedMap<String, SortedSet<Integer>> invertedDict) {
        try {
            String invertedIndexFileName = "invertedIndex/ii_" + invertedIndex + ".txt";
            Path path = Paths.get("invertedIndex");
            createAndWriteContentTooFile(invertedDict, invertedIndexFileName, path);
            for (Map.Entry<String, SortedSet<Integer>> sortedSetEntry : invertedDict.entrySet()) {
                nonposition += sortedSetEntry.getValue().size();
            }
            tokens += invertedDict.size();
            invertedIndex++;
        } catch (IOException e) {
            System.err.println("Issue occur while writing blocks: " + e.getMessage());
        }
    }

    /**
     * Actual Perform I/O operation for writing content from memory to disk.
     *
     * @param invertedDict          Inverted Index of Max 25k Terms
     * @param invertedIndexFileName Inverted Index File name
     * @param path                  Path of storing file.
     * @throws IOException Exception related to I/O operation
     */
    private void createAndWriteContentTooFile(SortedMap<String, SortedSet<Integer>> invertedDict, String invertedIndexFileName, Path path) throws IOException {
        if (!Files.exists(path))
            Files.createDirectory(path);
        File file = new File(invertedIndexFileName);
        if (!file.exists()) {
            // File created status
            file.createNewFile();
        }
        BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(invertedIndexFileName));
        for (Map.Entry<String, SortedSet<Integer>> sortedSetEntry : invertedDict.entrySet()) {
            String content = sortedSetEntry.getKey() + "`" + sortedSetEntry.getValue().toString();
            bufferedWriter.write(content + "\n");
        }
        bufferedWriter.close();
    }
}
